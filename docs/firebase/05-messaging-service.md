# 05 — FirebaseMessagingService, Payload, dan Notifier

Tiga file di `core/notification/`: parser payload, pembangun notifikasi, dan service-nya sendiri.
Dipisah supaya parser dan builder bisa di-unit-test tanpa menyentuh Firebase.

## 5.1 Kontrak payload

Sepakati bentuk payload dengan tim backend **sebelum** menulis parser. Untuk app ini:

```json
{
  "message": {
    "token": "<registration token>",
    "data": {
      "type": "LOAN_APPLICATION",
      "title": "Pengajuan disetujui",
      "body": "Pengajuan pinjaman #A-10293 telah disetujui.",
      "referenceId": "A-10293",
      "sentAt": "2026-09-08T14:03:11Z"
    },
    "android": { "priority": "HIGH" }
  }
}
```

Aturan yang mengikat kedua sisi:

- **Semua nilai di `data` adalah string.** FCM tidak punya tipe lain di blok ini; angka dan boolean
  harus dikirim sebagai string dan di-parse di client.
- Kunci `from`, `message_type`, `collapse_key`, `google.*`, dan `gcm.*` **dipesan sistem** — jangan
  dipakai sebagai nama field.
- Ukuran total payload maksimal **4096 byte**. Jangan mengirim isi notifikasi panjang atau gambar
  base64; kirim `referenceId` dan biarkan app mengambil detail via REST.
- Field yang tidak dikenal harus diabaikan client, bukan bikin crash — supaya backend bisa menambah
  field tanpa memaksa rilis app.

## 5.2 `PushPayload.kt`

```kotlin
package com.masesas.exercise.bcaf_test_1.core.notification

enum class PushType {
    LOAN_APPLICATION,
    LOAN_PRODUCT,
    GENERAL;

    companion object {
        fun from(raw: String?): PushType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: GENERAL
    }
}

data class PushPayload(
    val type: PushType,
    val title: String,
    val body: String,
    val referenceId: String?,
) {
    companion object {
        private const val KEY_TYPE = "type"
        private const val KEY_TITLE = "title"
        private const val KEY_BODY = "body"
        private const val KEY_REFERENCE_ID = "referenceId"

        fun from(data: Map<String, String>): PushPayload? {
            val title = data[KEY_TITLE]?.takeIf { it.isNotBlank() } ?: return null
            val body = data[KEY_BODY]?.takeIf { it.isNotBlank() } ?: return null

            return PushPayload(
                type = PushType.from(data[KEY_TYPE]),
                title = title,
                body = body,
                referenceId = data[KEY_REFERENCE_ID]?.takeIf { it.isNotBlank() },
            )
        }
    }
}
```

`from()` mengembalikan `null` alih-alih melempar. Payload rusak dari backend tidak boleh membuat
proses app mati di background — itu terlihat user sebagai crash tanpa sebab.

Ini juga titik yang paling mudah dan paling berharga untuk di-unit-test: payload lengkap, payload
tanpa `title`, `type` tidak dikenal, `type` beda kapitalisasi, dan map kosong.

## 5.3 `PushNotifier.kt`

```kotlin
package com.masesas.exercise.bcaf_test_1.core.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.presentation.compose.HomeActivityCompose

const val EXTRA_PUSH_TYPE = "extra_push_type"
const val EXTRA_PUSH_REFERENCE_ID = "extra_push_reference_id"

class PushNotifier(private val context: Context) {

    fun show(payload: PushPayload) {
        val manager = context.getSystemService<NotificationManager>() ?: return

        val channelId = when (payload.type) {
            PushType.LOAN_APPLICATION -> NotificationChannels.transactionId(context)
            else -> NotificationChannels.generalId(context)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(payload))
            .build()

        manager.notify(notificationId(payload), notification)
    }

    private fun contentIntent(payload: PushPayload): PendingIntent {
        val intent = Intent(context, HomeActivityCompose::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_PUSH_TYPE, payload.type.name)
            .putExtra(EXTRA_PUSH_REFERENCE_ID, payload.referenceId)

        return PendingIntent.getActivity(
            context,
            notificationId(payload),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationId(payload: PushPayload): Int =
        payload.referenceId?.hashCode() ?: payload.title.hashCode()
}
```

Empat keputusan yang layak dijelaskan:

- **`FLAG_IMMUTABLE` wajib** sejak Android 12 (API 31). Tanpa flag mutability yang eksplisit,
  `PendingIntent.getActivity()` melempar `IllegalArgumentException` — dan crash-nya terjadi di
  background, jauh dari layar developer.
- **`notificationId` diturunkan dari `referenceId`**, bukan `System.currentTimeMillis()`. Efeknya:
  update status untuk pengajuan yang sama **mengganti** notifikasi lama, bukan menumpuk lima baris
  untuk satu pengajuan. Kalau memang ingin menumpuk, pakai id unik + `setGroup()`.
- **`requestCode` PendingIntent sama dengan `notificationId`.** Kalau semua notifikasi memakai
  requestCode `0`, `FLAG_UPDATE_CURRENT` akan menimpa extras notifikasi sebelumnya dan setiap
  notifikasi membuka detail yang sama — bug klasik yang sulit dilacak.
- **`FLAG_ACTIVITY_CLEAR_TOP or SINGLE_TOP`** membuat Activity yang sudah berjalan menerima
  `onNewIntent()` alih-alih dibuat ulang. Penanganannya ada di [07](07-deep-link-dan-navigasi.md).

## 5.4 `BcafMessagingService.kt`

```kotlin
package com.masesas.exercise.bcaf_test_1.core.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.masesas.exercise.bcaf_test_1.domain.notification.repository.PushTokenRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "BcafMessagingService"

@AndroidEntryPoint
class BcafMessagingService : FirebaseMessagingService() {

    @Inject lateinit var pushTokenRepository: PushTokenRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val notifier by lazy { PushNotifier(applicationContext) }

    override fun onMessageReceived(message: RemoteMessage) {
        val payload = PushPayload.from(message.data)
            ?: run {
                Log.w(TAG, "Payload diabaikan: ${message.data.keys}")
                return
            }

        notifier.show(payload)
    }

    override fun onNewToken(token: String) {
        scope.launch { pushTokenRepository.syncToken(token) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
```

### Yang perlu diketahui tentang service ini

- **`onMessageReceived()` sudah berjalan di thread background** milik FCM, bukan main thread.
  Pekerjaan ringan (parse + `notify`) boleh langsung di sana.
- **Ada batas waktu.** Sistem memberi service ini sekitar **20 detik** (10 detik pada beberapa kondisi)
  sebelum dianggap boleh dimatikan. Pekerjaan jaringan yang bisa gagal dan perlu retry — seperti sync
  token — **tidak boleh** bergantung pada scope service. Lihat §5.5.
- **`@AndroidEntryPoint` berlaku untuk Service.** Hilt menyuntik field sebelum `onCreate()` selesai.
  `BcafApplication` sudah `@HiltAndroidApp`, jadi tidak ada setup tambahan.
- **`onNewToken()` tidak dipanggil setiap start.** Sync token saat app start ditangani terpisah di
  [06](06-registrasi-token-ke-backend.md).
- Jangan memanggil `startActivity()` dari sini. Sejak Android 10, launching Activity dari background
  diblokir sistem; jalur yang benar adalah notifikasi + `PendingIntent`.

### Menyimpan notifikasi ke Room

Tab **Notifikasi** saat ini masih `PlaceholderScreen`. Ketika diisi, `onMessageReceived()` adalah
tempat menulis entity-nya:

```kotlin
override fun onMessageReceived(message: RemoteMessage) {
    val payload = PushPayload.from(message.data) ?: return

    notifier.show(payload)
    scope.launch { notificationRepository.save(payload) }
}
```

Pola offline-first project ini tetap berlaku: Room satu-satunya sumber baca, push hanya salah satu
penulis. Push yang hilang tidak merusak daftar, karena `GET /api/notifications` tetap menyinkronkan
saat layar dibuka.

## 5.5 Kapan butuh WorkManager

Scope milik service mati begitu service dihentikan. Untuk pekerjaan yang **wajib sampai** — sync
token, ack ke backend — jalur yang tahan proses mati adalah WorkManager:

```toml
# libs.versions.toml
work = "2.11.2"
hiltWork = "1.4.0"
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }
androidx-hilt-work = { group = "androidx.hilt", name = "hilt-work", version.ref = "hiltWork" }
androidx-hilt-compiler = { group = "androidx.hilt", name = "hilt-compiler", version.ref = "hiltWork" }
```

```kotlin
@HiltWorker
class SyncPushTokenWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: PushTokenRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val token = inputData.getString(KEY_TOKEN) ?: return Result.failure()
        return when (repository.syncToken(token)) {
            is AppResult.Success -> Result.success()
            is AppResult.Failure -> Result.retry()
        }
    }
}
```

`onNewToken()` lalu hanya meng-enqueue worker dengan constraint `NetworkType.CONNECTED` dan backoff
eksponensial. **Keputusan untuk project ini:** v1 memakai `CoroutineScope` sederhana seperti di §5.4
karena WorkManager dan `hilt-work` belum ada di dependency, dan kegagalan sync tertutup oleh sync
saat app start. Naikkan ke WorkManager ketika mulai ada laporan user yang tidak menerima push setelah
ganti device — itu gejalanya.

## 5.6 Checklist bagian ini

- [ ] `PushPayload.from()` mengembalikan `null` untuk payload rusak, tidak melempar
- [ ] Unit test parser mencakup payload kosong, tanpa title, dan `type` tak dikenal
- [ ] `PendingIntent` memakai `FLAG_IMMUTABLE`
- [ ] `notificationId` dan `requestCode` unik per referensi
- [ ] Service `@AndroidEntryPoint`, scope di-`cancel()` pada `onDestroy()`
- [ ] Tidak ada `startActivity()` dari service

---

Sebelumnya: [04 — Channel & Permission](04-channel-dan-permission.md) ·
Lanjut: [06 — Registrasi Token ke Backend](06-registrasi-token-ke-backend.md)
