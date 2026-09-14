#!/usr/bin/env node
// GOOGLE_APPLICATION_CREDENTIALS=firebase-credentials.json node scripts/fcm-token.mjs
import { readFileSync } from 'node:fs'
import { createSign } from 'node:crypto'
import { resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

export function serviceAccount() {
  const keyPath = process.env.GOOGLE_APPLICATION_CREDENTIALS
  if (!keyPath) {
    throw new Error('Set GOOGLE_APPLICATION_CREDENTIALS ke path service account JSON')
  }

  let sa
  try {
    sa = JSON.parse(readFileSync(keyPath, 'utf8'))
  } catch (error) {
    throw new Error(`Service account tidak terbaca (${keyPath}): ${error.message}`)
  }

  if (!sa.client_email || !sa.private_key || !sa.project_id) {
    throw new Error(`Service account tidak lengkap: ${keyPath}`)
  }

  return sa
}

export async function accessToken(sa = serviceAccount()) {
  const now = Math.floor(Date.now() / 1000)
  const b64 = (obj) => Buffer.from(JSON.stringify(obj)).toString('base64url')

  const claim = b64({
    iss: sa.client_email,
    scope: 'https://www.googleapis.com/auth/firebase.messaging',
    aud: 'https://oauth2.googleapis.com/token',
    iat: now,
    exp: now + 3600,
  })

  const unsigned = `${b64({ alg: 'RS256', typ: 'JWT' })}.${claim}`
  const signature = createSign('RSA-SHA256').update(unsigned).sign(sa.private_key, 'base64url')

  const res = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion: `${unsigned}.${signature}`,
    }),
  })

  const json = await res.json()
  if (!res.ok) {
    throw new Error(`Gagal menukar JWT dengan access token:\n${JSON.stringify(json, null, 2)}`)
  }

  return json.access_token
}

const isMain = process.argv[1] && fileURLToPath(import.meta.url) === resolve(process.argv[1])

if (isMain) {
  try {
    process.stdout.write(await accessToken())
  } catch (error) {
    console.error(error.message)
    process.exit(1)
  }
}
