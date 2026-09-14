#!/usr/bin/env node
// GOOGLE_APPLICATION_CREDENTIALS=firebase-credentials.json node scripts/fcm-push.mjs --topic promo
// GOOGLE_APPLICATION_CREDENTIALS=firebase-credentials.json node scripts/fcm-push.mjs --fid <installationId>
import { accessToken, serviceAccount } from './fcm-token.mjs'

const TOPICS = ['promo', 'announcement', 'happy-people']
const FID_PATTERN = /^[A-Za-z0-9_-]{16,}$/
const CHANNELS = ['general', 'transaction', 'promo']
const DEEP_LINK_SCHEME = 'bcaf:'

//deeplink: 'quickduit://home',
//deeplink: 'quickduit://profile'

const DEFAULTS = {
  topic: 'promo',
  title: 'Promo nih cuy',
  body: 'Ada promo menarique',
  //channel: 'transaction',
  channel: 'promo',
  deeplink: 'bcaf://transaction/TRX-001',
}

const USAGE = `
Trigger push notification FCM HTTP v1 ke topic atau ke satu perangkat lewat Firebase
installation id (data-only, agar deeplink selalu diproses aplikasi).

  GOOGLE_APPLICATION_CREDENTIALS=firebase-credentials.json \\
    node scripts/fcm-push.mjs [opsi]

Target (pilih salah satu, default: --topic ${DEFAULTS.topic}):
  --topic <nama>     ${TOPICS.join(' | ')}
  --fid <id>         Firebase installation id satu perangkat

Opsi:
  --deeplink <uri>   Default: ${DEFAULTS.deeplink}
  --title <teks>     Default: ${DEFAULTS.title}
  --body <teks>      Default: ${DEFAULTS.body}
  --channel <id>     ${CHANNELS.join(' | ')} (default: ${DEFAULTS.channel})
  --project <id>     Default: project_id dari service account
  --dry-run          Validasi payload di server FCM tanpa benar-benar mengirim
  --help

Perangkat hanya menerima kiriman topic bila sudah subscribe lewat
DeviceRegistrationRepository.subscribe(). FCM selalu menerima kiriman ke topic,
termasuk topic tanpa satu pun subscriber, jadi respons sukses bukan bukti terkirim.

Installation id terbit dari FirebaseMessaging.register() dan sampai ke
PushMessagingService.onRegistered(). Berbeda dengan topic, FCM menolak kiriman
--fid bila id-nya tidak dikenal, jadi respons sukses berarti benar-benar terkirim.
`

function parseArgs(argv) {
  const flags = new Set(['dry-run', 'help'])
  const parsed = {}

  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    if (!arg.startsWith('--')) throw new Error(`Argumen tidak dikenal: ${arg}`)

    const [key, inlineValue] = arg.slice(2).split(/=(.*)/s)
    if (flags.has(key)) {
      parsed[key] = true
      continue
    }

    const value = inlineValue ?? argv[++i]
    if (value === undefined) throw new Error(`Opsi --${key} butuh nilai`)
    parsed[key] = value
  }

  return parsed
}

function buildTarget(args) {
  if (args.fid !== undefined && args.topic !== undefined) {
    throw new Error('Pilih salah satu target: --topic atau --fid')
  }

  if (args.fid !== undefined) {
    if (!FID_PATTERN.test(args.fid)) {
      throw new Error(`Installation id tidak valid: ${args.fid}`)
    }
    return { fid: args.fid }
  }

  const topic = args.topic ?? DEFAULTS.topic
  if (!TOPICS.includes(topic)) {
    throw new Error(`Topic "${topic}" tidak dikenal aplikasi, pilih: ${TOPICS.join(', ')}`)
  }

  return { topic }
}

function buildMessage(args) {
  const target = buildTarget(args)

  const channel = args.channel ?? DEFAULTS.channel
  if (!CHANNELS.includes(channel)) {
    throw new Error(`Channel "${channel}" tidak dikenal, pilih: ${CHANNELS.join(', ')}`)
  }

  const deeplink = args.deeplink ?? DEFAULTS.deeplink
  const parsedDeepLink = URL.parse(deeplink)
  if (!parsedDeepLink) throw new Error(`Deeplink bukan URI valid: ${deeplink}`)
  if (parsedDeepLink.protocol !== DEEP_LINK_SCHEME) {
    console.warn(`Peringatan: scheme "${parsedDeepLink.protocol}" bukan ${DEEP_LINK_SCHEME}, aplikasi kemungkinan mengabaikannya.`)
  }

  return {
    ...target,
    data: {
      title: args.title ?? DEFAULTS.title,
      body: args.body ?? DEFAULTS.body,
      channel,
      deeplink,
    },
    android: { priority: 'high' },
  }
}

async function send(projectId, bearer, message, dryRun) {
  const res = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${bearer}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ validate_only: Boolean(dryRun), message }),
  })

  const json = await res.json()
  if (!res.ok) throw new Error(`FCM menolak pesan (HTTP ${res.status}):\n${JSON.stringify(json, null, 2)}`)

  return json
}

try {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    console.log(USAGE.trim())
    process.exit(0)
  }

  const message = buildMessage(args)
  const sa = serviceAccount()
  const projectId = args.project ?? sa.project_id

  console.log(JSON.stringify(message, null, 2))

  const result = await send(projectId, await accessToken(sa), message, args['dry-run'])
  const target = message.fid ? `installation id ${message.fid}` : `topic ${message.topic}`
  console.log(args['dry-run'] ? 'Payload valid (dry run, tidak dikirim).' : `Terkirim ke ${target}: ${result.name}`)
} catch (error) {
  console.error(error.message)
  process.exit(1)
}
