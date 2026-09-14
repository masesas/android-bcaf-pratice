# 08 — Mengirim Pesan dari Backend (HTTP v1)

Sisi server. Bagian ini untuk tim backend; developer Android tetap perlu membacanya agar bisa
membedakan "app tidak menerima" dari "server tidak mengirim".

## 8.1 Endpoint dan otentikasi

```
POST https://fcm.googleapis.com/v1/projects/{PROJECT_ID}/messages:send
Authorization: Bearer <OAuth2 access token>
Content-Type: application/json
```

- `{PROJECT_ID}` diambil dari service account JSON (field `project_id`).
- Access token diterbitkan dari service account dengan scope
  `https://www.googleapis.com/auth/firebase.messaging`, berlaku ~1 jam, dan **harus di-cache**.
  Menerbitkan token baru di setiap pengiriman adalah penyebab umum latensi dan rate limit.

**API legacy (`/fcm/send` + `Authorization: key=<SERVER_KEY>`) sudah dimatikan.** Kalau menemukan
tutorial atau kode lama yang memakainya, itu tidak akan pernah berhasil — bukan masalah konfigurasi.

## 8.2 Uji cepat dengan curl

```bash
export PROJECT_ID="bcaf-test-1-4f2a9"
export GOOGLE_APPLICATION_CREDENTIALS="$HOME/secrets/firebase-service-account.json"

ACCESS_TOKEN=$(gcloud auth application-default print-access-token)

curl -s -X POST \
  "https://fcm.googleapis.com/v1/projects/${PROJECT_ID}/messages:send" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "message": {
      "token": "TOKEN_DEVICE_DI_SINI",
      "data": {
        "type": "LOAN_APPLICATION",
        "title": "Pengajuan disetujui",
        "body": "Pengajuan pinjaman #A-10293 telah disetujui.",
        "referenceId": "A-10293"
      },
      "android": {
        "priority": "HIGH",
        "ttl": "3600s"
      }
    }
  }'
```

Response sukses:

```json
{ "name": "projects/bcaf-test-1-4f2a9/messages/0:1757340191234567%31bd1c9631bd1c96" }
```

> Response `200 OK` berarti **FCM menerima pesan**, bukan berarti device menerimanya. Pengiriman ke
> device tidak pernah dikonfirmasi ke pengirim. Ini alasan mengapa daftar notifikasi in-app harus
> tetap punya jalur REST.

## 8.3 Anatomi message

```json
{
  "message": {
    "token": "...",
    "data": { "type": "...", "title": "...", "body": "...", "referenceId": "..." },
    "android": {
      "priority": "HIGH",
      "ttl": "3600s",
      "collapse_key": "loan_A-10293",
      "restricted_package_name": "com.masesas.exercise.bcaf_test_1"
    }
  }
}
```

| Field | Arti | Rekomendasi |
|---|---|---|
| `token` / `topic` / `condition` | tujuan — pilih **tepat satu** | `token` untuk notifikasi personal |
| `data` | payload custom, semua nilai string, maks 4096 byte | jalur utama app ini |
| `notification` | judul/isi yang dirender sistem | jangan digabung dengan pembangunan notifikasi manual |
| `android.priority` | `NORMAL` atau `HIGH` | `HIGH` hanya untuk yang benar-benar time-sensitive |
| `android.ttl` | berapa lama FCM menyimpan bila device offline | `3600s` untuk status transaksi; default 4 minggu terlalu lama untuk info yang cepat basi |
| `android.collapse_key` | pesan dengan key sama saling menimpa saat device offline | pakai id entitas agar user tidak menerima 5 update basi sekaligus |
| `restricted_package_name` | membatasi pengiriman ke satu package | jaring pengaman salah kirim antar environment |

Soal `priority`: `HIGH` membangunkan device dari Doze. Menandai semua pesan `HIGH` membuat Android
menurunkan prioritas app secara keseluruhan dan justru memperlambat pesan yang benar-benar penting.
Aturan sederhana: `HIGH` untuk perubahan status pengajuan, `NORMAL` untuk promosi.

## 8.4 Firebase Admin SDK (Java / Spring Boot)

Untuk backend JVM, Admin SDK menangani OAuth, refresh token, dan retry.

```kotlin
// build.gradle.kts backend
implementation("com.google.firebase:firebase-admin:9.10.0")
```

```kotlin
@Configuration
class FirebaseConfig {

    @Bean
    fun firebaseMessaging(
        @Value("\${firebase.service-account}") credentialsJson: String,
    ): FirebaseMessaging {
        val options = FirebaseOptions.builder()
            .setCredentials(
                GoogleCredentials.fromStream(credentialsJson.byteInputStream())
            )
            .build()

        val app = FirebaseApp.getApps()
            .firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
            ?: FirebaseApp.initializeApp(options)

        return FirebaseMessaging.getInstance(app)
    }
}
```

```kotlin
@Service
class PushSender(
    private val messaging: FirebaseMessaging,
    private val deviceTokens: DeviceTokenRepository,
) {

    fun notifyLoanStatus(userId: Long, referenceId: String, title: String, body: String) {
        val tokens = deviceTokens.findActiveByUserId(userId)
        if (tokens.isEmpty()) return

        tokens.chunked(MULTICAST_LIMIT).forEach { chunk ->
            val message = MulticastMessage.builder()
                .addAllTokens(chunk)
                .putData("type", "LOAN_APPLICATION")
                .putData("title", title)
                .putData("body", body)
                .putData("referenceId", referenceId)
                .setAndroidConfig(
                    AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setTtl(Duration.ofHours(1).toMillis())
                        .setCollapseKey("loan_$referenceId")
                        .build()
                )
                .build()

            val response = messaging.sendEachForMulticast(message)
            handleFailures(chunk, response)
        }
    }

    private companion object {
        const val MULTICAST_LIMIT = 500
    }
}
```

Catatan API: `sendAll()` dan `sendMulticast()` versi lama memakai batch endpoint yang sudah
dihapus. Yang berlaku sekarang adalah **`sendEach()`** dan **`sendEachForMulticast()`**, maksimal
**500 token** per panggilan.

## 8.5 Menangani token mati

Ini yang membedakan integrasi yang bertahan setahun dari yang perlahan berhenti bekerja.

```kotlin
private fun handleFailures(tokens: List<String>, response: BatchResponse) {
    response.responses.forEachIndexed { index, result ->
        if (result.isSuccessful) return@forEachIndexed

        when (result.exception?.messagingErrorCode) {
            MessagingErrorCode.UNREGISTERED,
            MessagingErrorCode.INVALID_ARGUMENT -> deviceTokens.delete(tokens[index])

            MessagingErrorCode.SENDER_ID_MISMATCH -> {
                log.error("Token {} milik Firebase project lain", tokens[index])
                deviceTokens.delete(tokens[index])
            }

            MessagingErrorCode.QUOTA_EXCEEDED,
            MessagingErrorCode.UNAVAILABLE,
            MessagingErrorCode.INTERNAL -> retryQueue.enqueue(tokens[index], message)

            else -> log.warn("Gagal kirim ke {}", tokens[index], result.exception)
        }
    }
}
```

| Kode | Arti | Tindakan |
|---|---|---|
| `UNREGISTERED` (404) | app di-uninstall / token dicabut | **hapus token** dari DB |
| `INVALID_ARGUMENT` (400) | token cacat atau payload salah | hapus token; periksa payload |
| `SENDER_ID_MISMATCH` (403) | token diterbitkan Firebase project lain | hapus; biasanya tercampurnya `google-services.json` debug/release |
| `QUOTA_EXCEEDED` (429) | terlalu banyak pesan | retry dengan backoff eksponensial |
| `UNAVAILABLE` (503) / `INTERNAL` (500) | gangguan sementara FCM | retry dengan backoff + jitter |
| `THIRD_PARTY_AUTH_ERROR` | kredensial APNs (iOS) | tidak relevan untuk Android |

Tanpa penghapusan `UNREGISTERED`, tabel token membengkak oleh device mati dan biaya sekaligus latensi
pengiriman naik terus.

## 8.6 Topic — siaran tanpa mengelola token

`token`, `topic`, dan `condition` saling eksklusif — satu message memakai **tepat satu**. Selain
field tujuan itu, seluruh payload identik: `data`, `android.priority`, `ttl`, `collapse_key` semua
berlaku sama. Dari sisi client **tidak ada yang perlu diubah** — `BcafMessagingService` dan
`PushPayload` tidak bisa dan tidak perlu membedakan pesan topic dari pesan token.

```json
{
  "message": {
    "topic": "promo",
    "data": {
      "type": "GENERAL",
      "title": "Promo bunga 0%",
      "body": "Berlaku sampai 30 September."
    },
    "android": { "priority": "NORMAL", "ttl": "86400s" }
  }
}
```

Kondisi gabungan (maksimal 5 topic per ekspresi, mendukung `&&`, `||`, `!`, dan tanda kurung):

```json
{ "message": { "condition": "'promo' in topics && 'android' in topics" } }
```

### Subscribe di client

```kotlin
FirebaseMessaging.getInstance().subscribeToTopic("promo").await()
FirebaseMessaging.getInstance().unsubscribeFromTopic("promo").await()
```

Idempoten — aman dipanggil di setiap app start. Untuk keperluan uji coba, langganan khusus build
debug menghemat banyak waktu karena tidak perlu lagi menyalin token per device:

```kotlin
if (BuildConfig.DEBUG) {
    FirebaseMessaging.getInstance().subscribeToTopic("debug")
}
```

Setelah itu seluruh device debug tim bisa dipicu dengan satu perintah:

```bash
curl -s -X POST \
  "https://fcm.googleapis.com/v1/projects/${PROJECT_ID}/messages:send" \
  -H "Authorization: Bearer $(node scripts/fcm-token.mjs)" \
  -H "Content-Type: application/json" \
  -d '{
    "message": {
      "topic": "debug",
      "data": {
        "type": "LOAN_APPLICATION",
        "title": "Tes topic",
        "body": "Pengajuan #A-10293 disetujui.",
        "referenceId": "A-10293"
      },
      "android": { "priority": "HIGH" }
    }
  }'
```

> **Tanpa `gcloud`?** `scripts/fcm-token.mjs` mencetak access token langsung dari service account
> JSON memakai `node:crypto` bawaan — tidak butuh instalasi apa pun:
>
> ```bash
> export GOOGLE_APPLICATION_CREDENTIALS="$HOME/secrets/firebase-service-account.json"
> node scripts/fcm-token.mjs
> ```
>
> `PROJECT_ID` adalah field `project_id` di `app/google-services.json` — **bukan** `mobilesdk_app_id`
> yang berformat `1:375913628663:android:...`. Memakai app id menghasilkan `401 UNAUTHENTICATED`
> yang menyesatkan, seolah-olah masalahnya ada di kredensial.

### Trigger manual lewat Firebase Console — dan jebakannya

Console → **Messaging** → **New campaign** → Target: **Topic**. Tidak butuh terminal maupun
kredensial, tapi ada satu hal yang harus dipahami sebelum memakainya untuk menguji:

> **Console selalu menyertakan blok `notification`.** Sesuai tabel di
> [01 §1.3](01-konsep-dan-arsitektur.md#13-tiga-jenis-payload-dan-konsekuensinya), saat app berada di
> background notifikasi digambar oleh sistem dan `onMessageReceived()` **tidak dipanggil** — sehingga
> `PushNotifier`, pemilihan channel, dan deep link kita semuanya dilewati. Custom data dari
> "Additional options" tetap terkirim, tapi baru terbaca lewat intent extras ketika notifikasi
> diketuk.

Konsekuensi praktis: Console berguna untuk membuktikan **"pesan sampai ke device"** — koneksi FCM,
token, izin, dan channel. Console **tidak** bisa dipakai untuk menguji jalur kode kita. Untuk itu
pakai curl data-only di atas.

### Subscription hilang saat logout

`unregisterCurrentToken()` di [06 §6.4](06-registrasi-token-ke-backend.md#64-layer-data) memanggil
`FirebaseMessaging.deleteToken()`. Menghapus token **menghapus seluruh langganan topic device itu**,
karena langganan terikat pada token, bukan pada instalasi app.

Artinya: setelah logout, device berhenti menerima pesan topic apa pun sampai berlangganan ulang.
Topic yang harus tetap hidup lintas sesi perlu didaftarkan ulang saat token baru terbit:

```kotlin
override fun onNewToken(token: String) {
    scope.launch {
        pushTokenRepository.syncToken(token)
        if (BuildConfig.DEBUG) FirebaseMessaging.getInstance().subscribeToTopic("debug")
    }
}
```

Kalau app tidak memakai topic sama sekali, perilaku ini justru menguntungkan dan tidak perlu
ditangani — cukup diketahui agar tidak dikira bug.

### Batasan dan aturan pakai

| Hal | Nilai / aturan |
|---|---|
| Format nama topic | hanya `[a-zA-Z0-9-_.~%]+` |
| Maksimal topic per app instance | 2000 |
| Jumlah subscriber per topic | tidak dibatasi |
| Propagasi subscribe | beberapa detik sampai beberapa menit |
| Kecepatan fanout | jauh lebih lambat dari kirim per-token; hitungan menit untuk audiens besar |

Dua konsekuensinya:

1. **Pesan pertama tepat setelah `subscribeToTopic()` sering tidak sampai.** Ini propagasi, bukan bug
   — jangan dikejar dengan menambah retry.
2. **Topic bukan jalur untuk pesan time-sensitive.** Status pengajuan yang harus tiba dalam hitungan
   detik wajib lewat token.

### Batas keamanan yang tidak boleh dilanggar

**Topic tidak punya otorisasi sama sekali.** Siapa pun yang membongkar APK bisa menemukan nama topic
dan berlangganan — termasuk topic yang namanya "ditebak" seperti `user_10293`. Tidak ada mekanisme di
FCM untuk memverifikasi bahwa yang berlangganan memang berhak.

- **Boleh lewat topic:** promo, pengumuman umum, info maintenance, topic uji coba.
- **Wajib lewat token:** status pengajuan pinjaman, nominal, dokumen, dan apa pun yang terikat pada
  satu user.

Jangan pernah memakai id user, nomor kontrak, atau nomor telepon sebagai nama topic.

## 8.7 Kuota dan performa

- Tidak ada biaya per pesan; FCM gratis.
- Batas praktis: ~600 ribu pesan/menit per project sebelum `QUOTA_EXCEEDED`. Blast besar harus
  di-throttle dari sisi backend, bukan mengandalkan retry.
- Kirim secara asinkron dari request user. Menahan HTTP response sampai FCM menjawab membuat latensi
  API bergantung pada layanan pihak ketiga.
- Simpan `messageId` dari response untuk korelasi log saat menyelidiki keluhan user.

## 8.8 Checklist bagian ini

- [ ] Memakai HTTP v1, bukan API legacy
- [ ] Access token di-cache, tidak diterbitkan per pengiriman
- [ ] Payload data-only, semua nilai string, di bawah 4096 byte
- [ ] `ttl` dan `collapse_key` diisi sesuai jenis pesan
- [ ] `priority: HIGH` hanya untuk pesan time-sensitive
- [ ] `UNREGISTERED` / `INVALID_ARGUMENT` menghapus token dari DB
- [ ] Topic hanya dipakai untuk siaran umum; semua pesan personal lewat `token`
- [ ] Tidak ada id user / nomor kontrak yang dipakai sebagai nama topic
- [ ] Topic yang harus bertahan lintas sesi didaftarkan ulang di `onNewToken()`
- [ ] Pengiriman berjalan asinkron dari request user
- [ ] Environment debug dan production memakai Firebase project berbeda

---

Sebelumnya: [07 — Deep Link](07-deep-link-dan-navigasi.md) ·
Lanjut: [09 — Testing & Troubleshooting](09-testing-dan-troubleshooting.md)
