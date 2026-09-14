# 01 — Konsep & Arsitektur FCM

> Sebelum menulis satu baris kode pun, ini yang harus dipahami. Sebagian besar bug push notification
> berasal dari salah paham di bagian ini, bukan dari salah ketik kode.

## 1.1 Peta jalur pesan

```
Backend BCAF                Google FCM                    Device
     |                          |                            |
     |-- POST /v1/projects/... ->|                           |
     |   (OAuth2 service acc.)   |-- push via socket ------->| Google Play services
     |                           |                           |      |
     |<-- 200 { name: "..." } ---|                           |      +--> App (FirebaseMessagingService)
                                                             |      +--> System tray (tanpa app)
```

Tiga hal penting dari diagram di atas:

1. **Backend tidak pernah bicara langsung ke device.** Backend bicara ke FCM, FCM yang mengantar.
2. **Yang menerima di device adalah Google Play services**, bukan app kita. App baru dilibatkan
   kalau Play services memutuskan perlu — dan itu tergantung *jenis payload* (lihat 1.3).
3. **Alamat tujuan adalah registration token**, bukan nomor HP/email/user id. Token ini milik
   pasangan (app install + device). Backend harus menyimpan pemetaan `userId -> [token]`.

## 1.2 Registration token — siklus hidup

Token adalah string panjang (~163 karakter) yang diterbitkan Play services untuk instalasi app kita.

Token **berubah** ketika:

- app di-install ulang atau data-nya di-clear,
- user me-restore app ke device baru,
- Firebase mendeteksi token kompromi/basi,
- app memanggil `deleteToken()` (mis. saat logout).

Token **tidak** dikirim ulang otomatis ke backend. Ini tanggung jawab kita:

| Momen | Aksi |
|---|---|
| App start (sudah login) | `getToken()` → sync ke backend bila beda dengan yang terakhir dikirim |
| `onNewToken()` dipanggil | sync ke backend |
| Login berhasil | sync ke backend (token lama milik user lain harus lepas) |
| Logout | hapus token di backend, lalu `deleteToken()` di device |

> **Jebakan paling umum:** hanya mengandalkan `onNewToken()`. Callback itu **tidak** dipanggil setiap
> app start — hanya saat token benar-benar berubah. App yang cuma pakai `onNewToken()` akan bekerja
> di device developer (yang baru install) dan gagal senyap di device user lama.

## 1.3 Tiga jenis payload dan konsekuensinya

Ini tabel paling penting di seluruh dokumen ini.

| Payload | App di foreground | App di background / tertutup |
|---|---|---|
| **notification** saja | `onMessageReceived()` dipanggil, **tidak ada** notifikasi otomatis | Play services menampilkan notifikasi sendiri, `onMessageReceived()` **tidak** dipanggil |
| **data** saja | `onMessageReceived()` dipanggil | `onMessageReceived()` dipanggil (butuh `priority: high` agar lolos Doze) |
| **notification + data** | `onMessageReceived()` dipanggil, data ada di `message.data` | Notifikasi ditampilkan sistem, data diantar lewat **intent extras** saat notifikasi diketuk |

Konsekuensi praktis:

- Kalau ingin **kontrol penuh** atas tampilan notifikasi (custom layout, grouping, penyimpanan ke
  Room, badge count) → gunakan **data-only** dan bangun notifikasi sendiri.
- Kalau cukup notifikasi standar dan ingin hemat effort → gunakan **notification**.
- **Jangan mengirim `notification` + membangun notifikasi manual di `onMessageReceived()`.** Di
  foreground akan tampil sekali, di background akan tampil sekali dari sistem — terlihat benar,
  sampai suatu saat tampil dobel. Pilih satu jalur.

Dokumen ini memakai **data-only** sebagai jalur utama, karena kebutuhan BCAF (deep link ke detail
transaksi, simpan riwayat ke daftar notifikasi in-app) menuntut kontrol penuh.

## 1.4 Yang tidak dijamin FCM

Tulis ini di catatan rilis, jangan sampai jadi kejutan di production:

- **FCM bukan message queue.** Pesan bisa hilang: device offline > 4 minggu (TTL maksimal), storage
  penuh, atau app di-force-stop oleh user (app tidak menerima apa pun sampai dibuka manual).
- **Urutan tidak dijamin.** Dua pesan berturut-turut bisa tiba terbalik.
- **Doze/App Standby menunda pesan normal-priority** hingga maintenance window. Hanya
  `priority: high` yang membangunkan device — dan penggunaannya diawasi sistem.
- **Device tanpa Google Play services** (beberapa device China, emulator tanpa GMS) tidak akan pernah
  menerima FCM.

Karena itu, **push notification bukan satu-satunya sumber kebenaran**. Layar notifikasi in-app harus
tetap bisa refresh via REST (`GET /api/notifications`); push hanya mempercepat.

## 1.5 Posisi di arsitektur project ini

Mengikuti aturan layer di [`../architecture.md`](../architecture.md):

```
core/notification/          <- framework entry point + tampilan notifikasi
    BcafMessagingService.kt     (menerima pesan, memicu sync token)
    NotificationChannels.kt     (registrasi channel)
    PushNotifier.kt             (membangun & posting NotificationCompat)
    PushPayload.kt              (map<String,String> -> model typed)

domain/notification/
    model/PushPayload sudah di core (framework-free), repository kontrak di sini
    repository/PushTokenRepository.kt

data/notification/
    remote/PushTokenApi.kt, PushTokenDto.kt
    local/PushTokenLocalDataSource.kt   (token terakhir yang sukses dikirim)
    repository/PushTokenRepositoryImpl.kt

di/PushNotificationModule.kt
```

Aturan yang tetap berlaku: `domain` tidak boleh mengimpor apa pun dari Firebase atau Android
framework. `BcafMessagingService` boleh, karena dia memang kelas framework.

---

Lanjut: [02 — Setup Firebase Console](02-setup-firebase-console.md)
