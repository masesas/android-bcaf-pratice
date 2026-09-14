# Firebase Cloud Messaging — Panduan End-to-End

Panduan implementasi push notification untuk project **bcaf-test-1**, dari membuat project Firebase
sampai notifikasi diketuk dan membuka layar detail yang benar. Seluruh contoh kode memakai konvensi
yang sudah berjalan di repo ini: Hilt, Retrofit + kotlinx.serialization, `AppResult`/`AppFailure`,
DataStore, dan navigasi Compose type-safe.

## Urutan baca

| # | Dokumen | Isi |
|---|---|---|
| 01 | [Konsep & Arsitektur](01-konsep-dan-arsitektur.md) | Jalur pesan, siklus hidup token, tiga jenis payload dan konsekuensinya, jaminan yang **tidak** diberikan FCM |
| 02 | [Setup Firebase Console](02-setup-firebase-console.md) | Membuat project, mendaftarkan app, `google-services.json`, service account untuk backend |
| 03 | [Gradle & Manifest](03-setup-gradle-dan-manifest.md) | Version catalog, plugin, `<service>`, meta-data, ikon notifikasi, ProGuard |
| 04 | [Channel & Runtime Permission](04-channel-dan-permission.md) | Notification channel, izin Android 13+, perbaikan untuk `MainActivity` yang ada sekarang |
| 05 | [Messaging Service](05-messaging-service.md) | `PushPayload`, `PushNotifier`, `BcafMessagingService`, batas waktu service, kapan butuh WorkManager |
| 06 | [Registrasi Token ke Backend](06-registrasi-token-ke-backend.md) | Kontrak endpoint, layer domain/data, DI, pemicu sync saat start/login/logout |
| 07 | [Deep Link & Navigasi](07-deep-link-dan-navigasi.md) | `navDeepLink` type-safe, `PendingIntent`, `onNewIntent`, deep link tertunda saat belum login |
| 08 | [Kirim dari Backend](08-kirim-dari-backend.md) | HTTP v1 + OAuth2, curl, Admin SDK, penanganan token mati, topic, kuota |
| 09 | [Testing & Troubleshooting](09-testing-dan-troubleshooting.md) | Unit test parser, urutan uji manual, perintah `adb`, tabel gejala → penyebab |
| 10 | [Checklist Rilis & Batasan](10-checklist-rilis.md) | Urutan implementasi, checklist keamanan, batasan yang diterima |

## Ringkasan alur

```
Backend  --HTTP v1-->  FCM  -->  Play services  -->  BcafMessagingService.onMessageReceived()
                                                          |
                                              PushPayload.from(message.data)
                                                          |
                                              PushNotifier.show(payload)
                                                          |
                                     PendingIntent  bcaf://transaction/{id}
                                                          |
                                    ketuk --> HomeActivityCompose --> NavController
                                                          |
                                              TransactionDetailScreen
```

Jalur token, arah sebaliknya:

```
FirebaseMessaging.getToken() / onNewToken()
        |
PushTokenRepository.syncToken()
        |
POST /api/notifications/devices  (Bearer token dari AuthHeaderInterceptor)
        |
Backend menyimpan userId -> [token]
```

## Keputusan desain

| Keputusan | Alasan | Alternatif yang ditolak |
|---|---|---|
| Payload **data-only** | Butuh kontrol penuh: deep link, channel per kategori, simpan ke Room | Payload `notification` — tidak memanggil `onMessageReceived` saat background, sehingga notifikasi tidak bisa dicatat ke daftar in-app |
| Deep link lewat **URI**, bukan intent extras | `navDeepLink<TransactionDetailRoute>` sudah terdaftar; pemetaan tinggal di deklarasi destination | `when(type)` manual di Activity — pemetaan tersebar di dua tempat dan mudah tidak sinkron |
| `notificationId` dari `referenceId` | Update status pengajuan yang sama saling mengganti, tidak menumpuk | Timestamp — lima update satu pengajuan jadi lima notifikasi |
| Sync token via `CoroutineScope` service | WorkManager belum ada di dependency; kegagalan tertutup sync saat app start | WorkManager — lebih andal, disiapkan sebagai jalur upgrade |
| Token terakhir disimpan di DataStore | Menghindari POST di setiap app start | Selalu POST — beban backend sia-sia |
| Firebase project terpisah debug/release | Pesan uji tidak pernah nyasar ke user | Satu project — sekali salah kirim tidak bisa ditarik |

## Prasyarat

- Device atau emulator dengan **Google Play services** (image emulator "Google APIs")
- Akses ke Firebase Console untuk project BCAF
- Backend yang menyediakan `POST`/`DELETE /api/notifications/devices`

---

Dokumen terkait: [Arsitektur Aplikasi](../architecture.md) ·
[Kotlin Coroutines Handbook](../kotlin-coroutines-handbook/README.md)
