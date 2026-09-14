# 03 — Gradle, Plugin, dan Manifest

Semua edit di bagian ini murni konfigurasi. Setelah selesai, `./gradlew :app:assembleDebug` harus
tetap hijau walau belum ada satu baris kode Kotlin baru.

## 3.1 Version catalog

`gradle/libs.versions.toml` — tambahkan:

```toml
[versions]
# ...
googleServices = "4.5.0"
firebaseBom = "34.18.0"

[libraries]
# ...
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-messaging = { group = "com.google.firebase", name = "firebase-messaging" }

[plugins]
# ...
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

Catatan versi:

- `firebase-messaging` sengaja **tanpa versi** — versinya ditentukan BOM. Jangan pernah menyetel
  versi artifact Firebase secara manual saat memakai BOM.
- Artifact `firebase-messaging-ktx` **sudah deprecated**; seluruh API KTX dipindah ke artifact utama
  sejak BOM 32.5.0. Jangan menambahkannya.
- Angka di atas adalah versi terbaru saat dokumen ini ditulis. Verifikasi ulang dengan:

  ```bash
  curl -s https://dl.google.com/dl/android/maven2/com/google/firebase/firebase-bom/maven-metadata.xml | tail -5
  curl -s https://dl.google.com/dl/android/maven2/com/google/gms/google-services/maven-metadata.xml | tail -5
  ```

## 3.2 Root `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.androidx.navigation.safeargs) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false   // <-- baru
}
```

## 3.3 `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    // ...
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)   // <-- baru
}

dependencies {
    // ...
    // Firebase — versi diatur BOM
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
}
```

Repository `google()` sudah dideklarasikan di `settings.gradle.kts` (`dependencyResolutionManagement`),
jadi tidak ada perubahan di sana.

> **Kalau sync gagal** dengan pesan seputar `com.google.gms.google-services` dan AGP: plugin
> google-services merilis update mengikuti AGP mayor. Naikkan `googleServices` ke rilis terbaru
> (perintah `curl` di §3.1), jangan menurunkan AGP.

Sanity check bahwa plugin benar-benar membaca konfigurasi:

```bash
./gradlew :app:processDebugGoogleServices
```

Task ini gagal dengan pesan eksplisit bila `google-services.json` hilang atau package name-nya tidak
cocok — pemeriksaan paling murah sebelum lanjut.

## 3.4 google-services.json per build type

Bila memakai dua Firebase project (disarankan):

```
app/
  src/debug/google-services.json      <- project staging
  src/release/google-services.json    <- project production
```

Plugin memilih berdasarkan build variant, dengan urutan pencarian
`src/<variant>/` → `src/<buildType>/` → `src/<flavor>/` → `app/`. Bila hanya ada satu project,
cukup `app/google-services.json` saja dan abaikan §3.4 ini.

Jangan menaruh file di dua tempat sekaligus untuk build type yang sama — yang lebih spesifik menang
secara diam-diam, dan ini sumber bug "kenapa push production nyasar ke Firebase staging".

## 3.5 AndroidManifest.xml

Manifest saat ini sudah punya `POST_NOTIFICATIONS`. Yang perlu ditambah:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".BcafApplication"
        ...>

        <service
            android:name=".core.notification.BcafMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>

        <meta-data
            android:name="com.google.firebase.messaging.default_notification_channel_id"
            android:value="@string/notification_channel_general_id" />

        <meta-data
            android:name="com.google.firebase.messaging.default_notification_icon"
            android:resource="@drawable/ic_notification" />

        <meta-data
            android:name="com.google.firebase.messaging.default_notification_color"
            android:resource="@color/notification_accent" />

        <!-- activity ... -->
    </application>
</manifest>
```

Penjelasan tiap baris:

| Elemen | Kegunaan | Wajib? |
|---|---|---|
| `<service>` + `MESSAGING_EVENT` | titik masuk `onMessageReceived` / `onNewToken` | Ya |
| `android:exported="false"` | hanya Play services yang boleh mem-bind | Ya |
| `default_notification_channel_id` | channel untuk pesan **notification** yang ditampilkan sistem saat app background | Sangat disarankan — tanpa ini Android memakai channel fallback `fcm_fallback_notification_channel` yang tidak bisa kita atur |
| `default_notification_icon` | ikon kecil untuk notifikasi yang dibangun sistem | Disarankan |
| `default_notification_color` | warna aksen ikon | Opsional |

`meta-data` **tidak** berpengaruh pada notifikasi yang kita bangun sendiri di `onMessageReceived()`;
di sana channel dan ikon ditentukan langsung di kode. Tetap dipasang sebagai jaring pengaman kalau
suatu saat backend mengirim payload `notification`.

## 3.6 Resource pendukung

`app/src/main/res/values/strings.xml`:

```xml
<string name="notification_channel_general_id" translatable="false">bcaf_general</string>
<string name="notification_channel_general_name">Informasi Umum</string>
<string name="notification_channel_general_description">Pengumuman dan informasi produk</string>
<string name="notification_channel_transaction_id" translatable="false">bcaf_transaction</string>
<string name="notification_channel_transaction_name">Status Pengajuan</string>
<string name="notification_channel_transaction_description">Perubahan status pengajuan pinjaman</string>
```

`app/src/main/res/values/colors.xml`:

```xml
<color name="notification_accent">#0B5FFF</color>
```

`app/src/main/res/drawable/ic_notification.xml` — **ikon notifikasi harus siluet putih transparan.**
Android sejak API 21 membuang seluruh warna pada small icon dan hanya memakai alpha channel. Memakai
`ic_launcher` di sini menghasilkan kotak putih polos di status bar — ini keluhan QA paling sering.

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="#FFFFFF">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.9,2 2,2zM18,16v-5c0,-3.07 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68C7.63,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2z" />
</vector>
```

Ganti `pathData` dengan ikon brand BCAF; yang wajib dipertahankan adalah aturan siluet putih.

## 3.7 ProGuard / R8

Tidak ada aturan tambahan yang perlu ditulis. Artifact Firebase membawa `consumer-rules.pro` sendiri.
Yang perlu diperhatikan justru **model payload kita**: kalau nanti payload di-parse dengan
kotlinx.serialization, kelas `@Serializable` sudah aman karena plugin serialization menghasilkan
aturan keep-nya. Build release saat ini `isMinifyEnabled = false`, jadi belum relevan — catat sebagai
hal yang diverifikasi ulang saat minify diaktifkan.

## 3.8 Checklist bagian ini

- [ ] `libs.versions.toml` berisi `firebase-bom`, `firebase-messaging`, plugin `google-services`
- [ ] Plugin di-declare `apply false` di root, di-apply di `:app`
- [ ] `implementation(platform(libs.firebase.bom))` mendahului artifact Firebase lain
- [ ] `./gradlew :app:processDebugGoogleServices` hijau
- [ ] `<service>` dengan `MESSAGING_EVENT` terdaftar, `exported="false"`
- [ ] `ic_notification` berupa siluet putih, bukan `ic_launcher`

---

Sebelumnya: [02 — Firebase Console](02-setup-firebase-console.md) ·
Lanjut: [04 — Channel & Runtime Permission](04-channel-dan-permission.md)
