# 04 — Notification Channel & Runtime Permission

Dua gerbang yang harus lolos sebelum satu notifikasi pun terlihat user:
**channel terdaftar** dan **izin `POST_NOTIFICATIONS` diberikan**. Gagal di salah satunya
menghasilkan gejala yang identik: pesan sampai, log bersih, layar tetap sepi.

## 4.1 Notification channel

Sejak Android 8 (API 26) setiap notifikasi wajib punya channel. `minSdk` project ini 29, jadi
channel **selalu** wajib — tidak ada cabang legacy yang perlu ditulis.

`app/src/main/java/com/masesas/exercise/bcaf_test_1/core/notification/NotificationChannels.kt`:

```kotlin
package com.masesas.exercise.bcaf_test_1.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.masesas.exercise.bcaf_test_1.R

object NotificationChannels {

    fun generalId(context: Context): String =
        context.getString(R.string.notification_channel_general_id)

    fun transactionId(context: Context): String =
        context.getString(R.string.notification_channel_transaction_id)

    fun register(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                generalId(context),
                context.getString(R.string.notification_channel_general_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_general_description)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                transactionId(context),
                context.getString(R.string.notification_channel_transaction_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_transaction_description)
                enableVibration(true)
            }
        )
    }
}
```

Daftarkan sedini mungkin — `Application.onCreate()`, bukan Activity:

```kotlin
@HiltAndroidApp
class BcafApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.register(this)
    }
}
```

Alasannya: saat app di background dan pesan `notification` datang, Play services menampilkan
notifikasi tanpa membuka Activity apa pun. Kalau channel baru dibuat di Activity, channel itu belum
ada dan sistem jatuh ke channel fallback.

### Aturan channel yang mudah terlewat

- `createNotificationChannel()` **idempoten** — aman dipanggil tiap start. Panggilan kedua tidak
  mengubah channel yang sudah ada.
- **Importance tidak bisa dinaikkan setelah channel dibuat.** Kalau channel dirilis dengan
  `IMPORTANCE_DEFAULT` lalu ingin `IMPORTANCE_HIGH`, satu-satunya cara adalah membuat channel dengan
  **id baru** (mis. `bcaf_transaction_v2`) dan menghapus yang lama via `deleteNotificationChannel()`.
  Karena itu: pikirkan importance sekali di awal.
- User bisa mematikan channel satu per satu. Cek dengan
  `manager.getNotificationChannel(id)?.importance == IMPORTANCE_NONE` sebelum menyalahkan FCM.
- Bikin channel **per kategori yang user mungkin ingin matikan terpisah**, bukan per jenis pesan
  teknis. Dua channel di atas sudah proporsional untuk app ini.

## 4.2 Runtime permission Android 13+

`POST_NOTIFICATIONS` adalah runtime permission sejak API 33. Di API 29–32 izin ini tidak ada dan
notifikasi aktif secara default.

### Kondisi kode saat ini — perlu diperbaiki

`MainActivity.requestNotificationPermission()` sekarang berbunyi:

```kotlin
val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ...
notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
```

Tiga masalah:

1. `granted` dihitung lalu **tidak dipakai** — cabang yang memakainya dikomentari, jadi dialog izin
   diminta ulang **setiap app start**, termasuk ketika izin sudah diberikan.
2. Di device API 29–32 (yang masuk `minSdk` project ini) `POST_NOTIFICATIONS` bukan runtime
   permission; peluncuran tetap terjadi dan callback kembali *denied*. Karena `startRouting()`
   dipanggil dari callback, alurnya kebetulan jalan — tapi kebetulan yang menutupi salah logika.
3. Android membekukan permintaan izin setelah **dua kali penolakan**. Meminta di setiap start
   mempercepat app sampai ke kondisi "permanently denied", dan sesudah itu dialog tidak akan pernah
   muncul lagi.

Perbaikannya cukup mengaktifkan kembali cabang yang dikomentari dan menjaga guard versi:

```kotlin
private fun requestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        startRouting()
        return
    }

    val granted = ContextCompat.checkSelfPermission(
        this, Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED

    if (granted) startRouting()
    else notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
}
```

`startRouting()` tetap dipanggil di kedua cabang karena izin notifikasi **tidak boleh** memblokir
akses ke app — user yang menolak notifikasi tetap harus bisa login.

### Versi Compose

Bila permintaan izin dipindah ke layar Compose (mis. setelah login berhasil, bukan di splash):

```kotlin
@Composable
fun rememberNotificationPermissionRequest(): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {},
    )
    val context = LocalContext.current

    return remember(launcher, context) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
```

### Kapan sebaiknya meminta izin

Bukan di detik pertama app dibuka. Urutan yang jauh lebih tinggi tingkat penerimaannya:

1. User login.
2. Saat user pertama kali membuka tab **Notifikasi**, atau tepat setelah mengajukan pinjaman
   (momen ketika notifikasi jelas berguna baginya).
3. Tampilkan penjelasan singkat in-app **sebelum** dialog sistem, bukan sesudah.

Kalau `shouldShowRequestPermissionRationale()` bernilai `true`, artinya user sudah pernah menolak —
tampilkan penjelasan, dan setelah penolakan kedua arahkan ke Settings:

```kotlin
Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
```

## 4.3 Memverifikasi kedua gerbang

```bash
# Izin yang dipegang app
adb shell dumpsys package com.masesas.exercise.bcaf_test_1 | grep -A2 POST_NOTIFICATIONS

# Channel yang terdaftar beserta importance-nya
adb shell dumpsys notification --noredact | grep -A6 "com.masesas.exercise.bcaf_test_1"
```

## 4.4 Checklist bagian ini

- [ ] `NotificationChannels.register()` dipanggil di `Application.onCreate()`
- [ ] Id channel di `strings.xml` sama dengan yang dirujuk `meta-data` di manifest
- [ ] Guard `Build.VERSION.SDK_INT >= TIRAMISU` ada sebelum `launch()`
- [ ] Izin tidak diminta ulang saat sudah granted
- [ ] Penolakan izin tidak memblokir alur login

---

Sebelumnya: [03 — Gradle & Manifest](03-setup-gradle-dan-manifest.md) ·
Lanjut: [05 — FirebaseMessagingService](05-messaging-service.md)
