# 09 — Testing & Troubleshooting

## 9.1 Unit test — parser payload

`PushPayload.from()` adalah satu-satunya logika murni di jalur push, dan satu-satunya yang bisa
diuji tanpa device. Tulis test-nya **sebelum** implementasinya.

`app/src/test/java/com/masesas/exercise/bcaf_test_1/core/notification/PushPayloadTest.kt`:

```kotlin
class PushPayloadTest {

    @Test
    fun `payload lengkap dipetakan utuh`() {
        val payload = PushPayload.from(
            mapOf(
                "type" to "LOAN_APPLICATION",
                "title" to "Pengajuan disetujui",
                "body" to "Pengajuan #A-10293 disetujui.",
                "referenceId" to "A-10293",
            )
        )

        assertEquals(PushType.LOAN_APPLICATION, payload?.type)
        assertEquals("A-10293", payload?.referenceId)
    }

    @Test
    fun `type tidak dikenal jatuh ke GENERAL`() {
        val payload = PushPayload.from(
            mapOf("type" to "PROMO_KILAT", "title" to "t", "body" to "b")
        )

        assertEquals(PushType.GENERAL, payload?.type)
    }

    @Test
    fun `type tidak peka huruf besar kecil`() {
        val payload = PushPayload.from(
            mapOf("type" to "loan_application", "title" to "t", "body" to "b")
        )

        assertEquals(PushType.LOAN_APPLICATION, payload?.type)
    }

    @Test
    fun `title kosong menghasilkan null`() {
        assertNull(PushPayload.from(mapOf("title" to "   ", "body" to "b")))
    }

    @Test
    fun `map kosong menghasilkan null`() {
        assertNull(PushPayload.from(emptyMap()))
    }

    @Test
    fun `referenceId kosong dianggap tidak ada`() {
        val payload = PushPayload.from(mapOf("title" to "t", "body" to "b", "referenceId" to ""))

        assertNull(payload?.referenceId)
    }
}
```

Buktikan test-nya benar-benar bisa merah: ubah sementara `?: return null` menjadi `?: ""` dan
pastikan dua test terakhir gagal. Test yang tidak pernah bisa gagal tidak menjaga apa pun.

Untuk `PushTokenRepositoryImpl`, fake `PushTokenApi` dan `PushTokenLocalDataSource` cukup untuk
menguji tiga hal yang mudah salah:

- token identik dengan `lastSyncedToken` **tidak** memicu panggilan API,
- kegagalan API **tidak** menulis `saveSyncedToken`,
- `unregisterCurrentToken()` tetap membersihkan lokal ketika API gagal.

## 9.2 Mendapatkan token untuk pengujian

Tambahkan log sementara (hapus sebelum rilis):

```kotlin
if (BuildConfig.DEBUG) {
    FirebaseMessaging.getInstance().token
        .addOnSuccessListener { Log.d("FCM_TOKEN", it) }
}
```

```bash
adb logcat -s FCM_TOKEN
```

Cara lain tanpa menyentuh kode: Firebase Console → **Messaging** → **New campaign** →
**Send test message** → tempel token.

## 9.3 Urutan pengujian manual

Uji dari yang paling sempit ke paling luas. Kalau langkah 1 gagal, tidak ada gunanya melanjutkan.

1. **Navigasi tanpa push**

   ```bash
   adb shell am start -a android.intent.action.VIEW \
     -d "bcaf://transaction/A-10293" com.masesas.exercise.bcaf_test_1
   ```

2. **Notifikasi tanpa FCM** — panggil `PushNotifier(context).show(payloadDummy)` dari tombol debug.
   Memisahkan masalah tampilan/channel/izin dari masalah jaringan.

3. **FCM foreground** — kirim data-only via curl ([08 §8.2](08-kirim-dari-backend.md#82-uji-cepat-dengan-curl))
   saat app terbuka. `onMessageReceived` harus terpanggil.

4. **FCM background** — tekan Home, kirim lagi.

5. **App tertutup** — swipe dari recents (jangan **Force stop**), kirim lagi.

6. **Setelah reboot device** — token harus tetap valid, push tetap masuk.

7. **Setelah clear data** — token berubah, `onNewToken` terpanggil, backend menerima token baru.

8. **Logout lalu kirim ke token lama** — harus gagal/tidak sampai, membuktikan unregister bekerja.

Perbedaan penting: **Force stop** membuat app tidak menerima pesan apa pun sampai dibuka manual, dan
itu perilaku Android yang benar — bukan bug. Selalu pakai swipe dari recents saat menguji "app
tertutup".

## 9.4 Perintah diagnosis

```bash
# Log FCM dari sisi Play services
adb logcat -s FirebaseMessaging FA FirebaseInstanceId

# Verifikasi service terdaftar di manifest yang ter-install
adb shell dumpsys package com.masesas.exercise.bcaf_test_1 | grep -i messaging

# Channel + importance + apakah notifikasi app diblokir
adb shell dumpsys notification --noredact | grep -A8 com.masesas.exercise.bcaf_test_1

# Status izin
adb shell dumpsys package com.masesas.exercise.bcaf_test_1 | grep -A2 POST_NOTIFICATIONS

# Simulasi Doze untuk menguji priority
adb shell dumpsys deviceidle force-idle
adb shell dumpsys deviceidle unforce
```

## 9.5 Tabel troubleshooting

| Gejala | Penyebab paling mungkin | Cara memastikan |
|---|---|---|
| `getToken()` selalu gagal / null | Play services tidak ada atau usang (emulator tanpa GMS) | Pakai image emulator "Google APIs"; cek `GoogleApiAvailability` |
| Token didapat, pesan tidak pernah masuk | `google-services.json` dari project berbeda dengan pengirim | Bandingkan `project_id` di JSON dengan `PROJECT_ID` di curl |
| Sampai di debug, tidak di release | `google-services.json` release belum dipasang / package name beda | Jalankan `./gradlew :app:processReleaseGoogleServices` |
| curl `401 UNAUTHENTICATED` | `gcloud` tidak terpasang sehingga `Bearer` kosong, atau URL memakai `mobilesdk_app_id` sebagai `PROJECT_ID` | Cek `node scripts/fcm-token.mjs` mencetak token; bandingkan URL dengan `project_id` di `google-services.json` |
| curl `200 OK` ke topic tapi tidak ada yang menerima | Belum ada device yang `subscribeToTopic()` — kirim ke topic kosong tetap sukses | Pastikan `subscribeToTopic` sudah jalan; tunggu propagasi beberapa menit |
| curl `200 OK` tapi device sepi | App di-force-stop, atau device offline melewati TTL | Buka app manual, kirim ulang |
| `onMessageReceived` jalan di foreground saja | Payload memakai blok `notification` | Kirim **data-only** |
| Notifikasi dobel | Payload berisi `notification` **dan** kode membangun notifikasi sendiri | Buang blok `notification` |
| Notifikasi tidak muncul, log bersih | Izin `POST_NOTIFICATIONS` ditolak, atau channel di-disable user | `dumpsys notification`, §9.4 |
| Ikon kotak putih di status bar | Small icon bukan siluet putih | Ganti dengan vector putih ([03 §3.6](03-setup-gradle-dan-manifest.md#36-resource-pendukung)) |
| Crash saat pesan datang di background | `PendingIntent` tanpa `FLAG_IMMUTABLE` (API 31+) | `adb logcat` cari `IllegalArgumentException` |
| Semua notifikasi membuka detail yang sama | `requestCode` PendingIntent konstan + `FLAG_UPDATE_CURRENT` | Pakai id unik per referensi |
| Deep link tidak jalan saat app terbuka | `onNewIntent` tidak diteruskan ke NavController | §7.4 |
| Pesan terlambat berjam-jam | `priority: NORMAL` + device dalam Doze | Naikkan ke `HIGH` untuk yang time-sensitive |
| `SENDER_ID_MISMATCH` | Token dari project debug dikirim lewat kredensial production | Pisahkan tabel token per environment |
| Push masuk ke user yang salah | Backend tidak memindahkan kepemilikan token saat device ganti user | [06 §6.1](06-registrasi-token-ke-backend.md#61-kontrak-endpoint) |
| Berhenti bekerja setelah beberapa bulan | Token basi tidak pernah dibersihkan | Terapkan penghapusan `UNREGISTERED` |

## 9.6 Yang tidak bisa diuji di emulator

- Perilaku Doze sesungguhnya (bisa disimulasi, tidak identik).
- Battery optimization agresif vendor (Xiaomi, Oppo, Vivo, Huawei) yang membunuh proses background.
  Untuk itu wajib ada satu device fisik dari vendor tersebut di daftar uji rilis.
- Push saat device benar-benar mati/tidur panjang.

---

Sebelumnya: [08 — Kirim dari Backend](08-kirim-dari-backend.md) ·
Lanjut: [10 — Checklist Rilis & Batasan](10-checklist-rilis.md)
