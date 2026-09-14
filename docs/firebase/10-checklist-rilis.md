# 10 — Checklist Rilis, Keamanan, dan Batasan

## 10.1 Urutan implementasi

Kerjakan berurutan; tiap langkah bisa diverifikasi sendiri sebelum lanjut.

| # | Langkah | Bukti selesai |
|---|---|---|
| 1 | Project & app Firebase dibuat, `google-services.json` terpasang | `./gradlew :app:processDebugGoogleServices` hijau |
| 2 | Dependency + plugin Gradle | `./gradlew :app:assembleDebug` hijau |
| 3 | Channel + izin runtime | `dumpsys notification` menampilkan kedua channel |
| 4 | `PushPayload` + unit test | test merah dulu, lalu hijau |
| 5 | `PushNotifier` | notifikasi dummy tampil dari tombol debug |
| 6 | `BcafMessagingService` | curl data-only → notifikasi muncul |
| 7 | Deep link | `adb am start` membuka detail; ketuk notifikasi membuka detail |
| 8 | Registrasi token ke backend | baris token muncul di DB backend setelah login |
| 9 | Unregister saat logout | kirim ke token lama tidak sampai |
| 10 | Backend menghapus token `UNREGISTERED` | uninstall → kirim → baris terhapus |

## 10.2 Keamanan

- [ ] Service account JSON **tidak** ada di repo Android maupun di APK
- [ ] `<service>` FCM `android:exported="false"`
- [ ] Payload push **tidak** memuat data sensitif — notifikasi terbaca di lockscreen. Kirim
      `referenceId`, bukan nominal pinjaman, sisa tagihan, atau data pribadi
- [ ] Nilai dari deep link URI diperlakukan sebagai input tidak tepercaya; detail selalu diambil ulang
      lewat endpoint ber-otorisasi
- [ ] Endpoint registrasi device butuh `Authorization`; tidak ada jalur anonim
- [ ] Satu token hanya terikat ke satu user pada satu waktu
- [ ] Data personal tidak pernah dikirim lewat topic
- [ ] Log token dihapus sebelum rilis (`Log.d("FCM_TOKEN", ...)`)
- [ ] Firebase project debug dan production terpisah

## 10.3 Sebelum menekan tombol rilis

- [ ] `google-services.json` untuk build release sudah yang benar
- [ ] Diuji pada build release yang ditandatangani, bukan hanya debug
- [ ] Ikon notifikasi diperiksa pada tema terang dan gelap
- [ ] Matriks perilaku [07 §7.7](07-deep-link-dan-navigasi.md#77-matriks-perilaku-yang-harus-diuji) lolos
- [ ] Diuji di minimal satu device vendor dengan battery optimization agresif
- [ ] Diuji pada Android 13+ (izin) **dan** Android 12 ke bawah (tanpa izin)
- [ ] Backend punya rate limiting untuk blast
- [ ] Ada dashboard/log jumlah kirim, sukses, dan token terhapus
- [ ] Bila `isMinifyEnabled` diaktifkan: alur push diuji ulang pada build ter-minify

## 10.4 Batasan yang diterima (v1)

Didokumentasikan supaya tidak dilaporkan sebagai bug:

- **Pengiriman tidak dijamin.** Force stop, device offline melewati TTL, atau perangkat tanpa Google
  Play services tidak akan menerima push. Daftar notifikasi in-app tetap harus bisa di-refresh via REST.
- **Deep link tertunda hanya bertahan selama proses hidup.** User yang menutup app di layar login
  kehilangan tujuan deep link-nya.
- **Sync token memakai `CoroutineScope` milik service, bukan WorkManager.** Kegagalan sync di
  `onNewToken` tidak di-retry; tertutup oleh sync saat app start berikutnya. Naikkan ke WorkManager
  bila mulai ada laporan user tidak menerima push setelah ganti device.
- **Urutan pesan tidak dijamin.** UI tidak boleh mengasumsikan urutan kedatangan; urutkan berdasarkan
  timestamp dari payload/server.
- **Tidak ada badge count di launcher.** Perilaku badge berbeda-beda antar vendor dan tidak ada API
  standar Android.
- **Notifikasi belum disimpan ke Room.** Tab Notifikasi masih placeholder; hook penyimpanannya sudah
  disiapkan di [05 §5.4](05-messaging-service.md#menyimpan-notifikasi-ke-room).

## 10.5 Di luar cakupan

Sengaja tidak diimplementasikan di v1:

- Notification action button (Setujui / Tolak langsung dari notifikasi)
- Grouping & summary notification
- Rich notification dengan gambar (`BigPictureStyle`)
- In-app messaging dan Firebase Analytics
- Penjadwalan pengiriman berdasarkan zona waktu user
- App Links (`https://`) dengan verifikasi `assetlinks.json`

---

Sebelumnya: [09 — Testing & Troubleshooting](09-testing-dan-troubleshooting.md) ·
Kembali ke [daftar isi](README.md)
