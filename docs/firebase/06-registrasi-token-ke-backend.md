# 06 — Registrasi Token ke Backend

Bagian yang paling sering diremehkan dan paling sering jadi penyebab "push-nya tidak sampai".
FCM sudah benar, kode client sudah benar, tapi backend memegang token yang salah.

## 6.1 Kontrak endpoint

Sepakati dua endpoint dengan backend:

```
POST   /api/notifications/devices     { token, platform, appVersion, deviceModel }
DELETE /api/notifications/devices     { token }
```

Keduanya memakai `Authorization: Bearer <accessToken>` — sudah otomatis ditempelkan
`AuthHeaderInterceptor`, jadi tidak ada penanganan header di layer fitur.

Perilaku yang harus dijamin backend:

- `POST` bersifat **upsert** dan **idempoten**. Token yang sama dikirim dua kali tidak membuat dua
  baris.
- Satu user boleh punya **banyak** token (HP + tablet). Jangan menimpa token lama saat user login di
  device kedua.
- Satu token hanya boleh terikat ke **satu** user. Bila token yang sudah terdaftar atas user A
  dikirim atas nama user B (device dipakai bergantian), backend memindahkan kepemilikan ke B.
  Tanpa aturan ini, user B menerima notifikasi milik user A — insiden kebocoran data, bukan sekadar bug.
- Saat FCM mengembalikan `UNREGISTERED` / `INVALID_ARGUMENT` untuk sebuah token, backend
  **menghapus** token itu. Detailnya di [08 §8.5](08-kirim-dari-backend.md#85-menangani-token-mati).

## 6.2 Dependency tambahan

Mengambil token dari Firebase mengembalikan `Task<String>`. Agar bisa di-`await` dalam coroutine:

```toml
# libs.versions.toml — [libraries]
kotlinx-coroutines-play-services = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version.ref = "coroutines" }
```

```kotlin
// app/build.gradle.kts
implementation(libs.kotlinx.coroutines.play.services)
```

`coroutines` sudah ada di version catalog (`1.10.2`), dipakai ulang di sini.

## 6.3 Layer domain

`domain/notification/repository/PushTokenRepository.kt`:

```kotlin
package com.masesas.exercise.bcaf_test_1.domain.notification.repository

import com.masesas.exercise.bcaf_test_1.domain.common.AppResult

/**
 * Menjaga agar token FCM device ini terdaftar di backend atas nama user yang sedang login.
 *
 * Semua operasi idempoten dan aman dipanggil berkali-kali.
 */
interface PushTokenRepository {

    /** Mengambil token terkini dari Firebase lalu mendaftarkannya bila berbeda dari yang terakhir sukses dikirim. */
    suspend fun syncCurrentToken(): AppResult<Unit>

    /** Mendaftarkan [token] yang datang dari `onNewToken`. */
    suspend fun syncToken(token: String): AppResult<Unit>

    /** Melepas token device ini di backend lalu menghapusnya di Firebase. Dipanggil saat logout. */
    suspend fun unregisterCurrentToken(): AppResult<Unit>
}
```

Tidak ada tipe Firebase maupun Android yang bocor ke interface ini — aturan layer project tetap utuh.

## 6.4 Layer data

`data/notification/remote/PushTokenDto.kt`:

```kotlin
package com.masesas.exercise.bcaf_test_1.data.notification.remote

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceRequestDto(
    val token: String,
    val platform: String = "ANDROID",
    val appVersion: String,
    val deviceModel: String,
)

@Serializable
data class UnregisterDeviceRequestDto(
    val token: String,
)
```

`data/notification/remote/PushTokenApi.kt`:

```kotlin
package com.masesas.exercise.bcaf_test_1.data.notification.remote

import com.masesas.exercise.bcaf_test_1.core.network.ApiEnvelope
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.POST

interface PushTokenApi {

    @POST("api/notifications/devices")
    suspend fun register(@Body body: RegisterDeviceRequestDto): ApiEnvelope<Unit>

    @HTTP(method = "DELETE", path = "api/notifications/devices", hasBody = true)
    suspend fun unregister(@Body body: UnregisterDeviceRequestDto): ApiEnvelope<Unit>
}
```

`@HTTP(hasBody = true)` dipakai karena anotasi `@DELETE` bawaan Retrofit tidak mengizinkan body.

`data/notification/local/PushTokenLocalDataSource.kt`:

```kotlin
package com.masesas.exercise.bcaf_test_1.data.notification.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.masesas.exercise.bcaf_test_1.BuildConfig
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val PUSH_PREFERENCES_NAME = "${BuildConfig.APPLICATION_ID}_push"

private val Context.pushPreferences: DataStore<Preferences> by preferencesDataStore(
    name = PUSH_PREFERENCES_NAME,
)

class PushTokenLocalDataSource(context: Context) {

    private val dataStore = context.applicationContext.pushPreferences

    suspend fun lastSyncedToken(): String? = dataStore.data
        .catch { throwable -> if (throwable is IOException) emit(emptyPreferences()) else throw throwable }
        .map { it[Keys.LAST_SYNCED_TOKEN] }
        .first()

    suspend fun saveSyncedToken(token: String) {
        dataStore.edit { it[Keys.LAST_SYNCED_TOKEN] = token }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(Keys.LAST_SYNCED_TOKEN) }
    }

    private object Keys {
        val LAST_SYNCED_TOKEN = stringPreferencesKey("last_synced_token")
    }
}
```

Menyimpan token terakhir yang **sukses** dikirim mencegah satu POST di setiap app start. Yang penting:
tulis ke DataStore **hanya setelah** backend menjawab sukses. Menulis lebih awal berarti kegagalan
jaringan menghasilkan device yang selamanya menganggap dirinya sudah terdaftar.

`data/notification/repository/PushTokenRepositoryImpl.kt`:

```kotlin
package com.masesas.exercise.bcaf_test_1.data.notification.repository

import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging
import com.masesas.exercise.bcaf_test_1.BuildConfig
import com.masesas.exercise.bcaf_test_1.core.network.requirePayload
import com.masesas.exercise.bcaf_test_1.core.network.runApiCatching
import com.masesas.exercise.bcaf_test_1.data.notification.local.PushTokenLocalDataSource
import com.masesas.exercise.bcaf_test_1.data.notification.remote.PushTokenApi
import com.masesas.exercise.bcaf_test_1.data.notification.remote.RegisterDeviceRequestDto
import com.masesas.exercise.bcaf_test_1.data.notification.remote.UnregisterDeviceRequestDto
import com.masesas.exercise.bcaf_test_1.domain.common.AppResult
import com.masesas.exercise.bcaf_test_1.domain.common.CommonFailure
import com.masesas.exercise.bcaf_test_1.domain.common.map
import com.masesas.exercise.bcaf_test_1.domain.common.onSuccess
import com.masesas.exercise.bcaf_test_1.domain.notification.repository.PushTokenRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class PushTokenRepositoryImpl internal constructor(
    private val api: PushTokenApi,
    private val localDataSource: PushTokenLocalDataSource,
    private val json: Json,
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PushTokenRepository {

    override suspend fun syncCurrentToken(): AppResult<Unit> = withContext(ioDispatcher) {
        val token = runCatching { messaging.token.await() }.getOrNull()
            ?: return@withContext AppResult.failure(CommonFailure.Unexpected())

        if (token == localDataSource.lastSyncedToken()) return@withContext AppResult.success(Unit)

        register(token)
    }

    override suspend fun syncToken(token: String): AppResult<Unit> = withContext(ioDispatcher) {
        register(token)
    }

    override suspend fun unregisterCurrentToken(): AppResult<Unit> = withContext(ioDispatcher) {
        val token = localDataSource.lastSyncedToken()
            ?: return@withContext AppResult.success(Unit)

        val result = runApiCatching(json) {
            api.unregister(UnregisterDeviceRequestDto(token)).requirePayload().map { }
        }

        localDataSource.clear()
        runCatching { messaging.deleteToken().await() }

        result
    }

    private suspend fun register(token: String): AppResult<Unit> = runApiCatching(json) {
        api.register(
            RegisterDeviceRequestDto(
                token = token,
                appVersion = BuildConfig.VERSION_NAME,
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            )
        ).requirePayload().map { }
    }.onSuccess { localDataSource.saveSyncedToken(token) }
}
```

Empat detail yang disengaja:

1. **`unregisterCurrentToken()` tetap membersihkan lokal dan Firebase walau panggilan backend gagal.**
   Logout tidak boleh gagal hanya karena jaringan mati; sisa token di backend akan dibersihkan sendiri
   saat FCM mengembalikan `UNREGISTERED`.
2. **`deleteToken()` dipanggil terakhir.** Kalau dipanggil lebih dulu, token yang mau dilepas ke
   backend sudah tidak ada.
3. **`messaging` diinjeksi lewat parameter dengan default.** Test bisa memasukkan fake tanpa
   menyentuh Firebase.
4. **`map { }`** mengubah `AppResult<Unit>` dari envelope menjadi `AppResult<Unit>` domain tanpa
   membocorkan tipe DTO.

> **Catatan bila backend memakai envelope `data: null` untuk sukses:** `requirePayload()` di project
> ini menganggap `data == null` sebagai kegagalan. Untuk endpoint yang memang tidak mengembalikan
> body, minta backend mengirim `"data": {}`, atau tambahkan helper `requireSuccess()` di
> `core/network/ApiCall.kt` yang hanya memeriksa blok `error`. Sepakati ini sebelum implementasi —
> kalau tidak, semua sync token akan dilaporkan gagal padahal berhasil.

## 6.5 Dependency injection

`di/PushNotificationModule.kt`:

```kotlin
package com.masesas.exercise.bcaf_test_1.di

import android.content.Context
import com.masesas.exercise.bcaf_test_1.data.notification.local.PushTokenLocalDataSource
import com.masesas.exercise.bcaf_test_1.data.notification.remote.PushTokenApi
import com.masesas.exercise.bcaf_test_1.data.notification.repository.PushTokenRepositoryImpl
import com.masesas.exercise.bcaf_test_1.domain.notification.repository.PushTokenRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PushNotificationModule {

    @Provides
    @Singleton
    fun providePushTokenApi(retrofit: Retrofit): PushTokenApi =
        retrofit.create(PushTokenApi::class.java)

    @Provides
    @Singleton
    fun providePushTokenLocalDataSource(
        @ApplicationContext context: Context,
    ): PushTokenLocalDataSource = PushTokenLocalDataSource(context)

    @Provides
    @Singleton
    fun providePushTokenRepository(
        api: PushTokenApi,
        localDataSource: PushTokenLocalDataSource,
        json: Json,
    ): PushTokenRepository = PushTokenRepositoryImpl(
        api = api,
        localDataSource = localDataSource,
        json = json,
    )
}
```

Bentuknya sengaja mengikuti `AuthModule` dan `NetworkModule` yang sudah ada — `object` module,
`@Provides` mengembalikan tipe interface domain.

## 6.6 Memasang pemicu sync

Token disinkronkan di tiga titik. Semuanya lewat `AuthViewModel`, karena hanya di sana status login
diketahui.

```kotlin
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val pushTokenRepository: PushTokenRepository,
) : ViewModel() {

    // setelah restoreSession() atau login() sukses dan user terbukti login:
    private fun syncPushToken() {
        viewModelScope.launch { pushTokenRepository.syncCurrentToken() }
    }

    fun logout() {
        viewModelScope.launch {
            pushTokenRepository.unregisterCurrentToken()
            authRepository.logout()
        }
    }
}
```

Urutan pada `logout()` mengikat: **lepas token dulu, baru hapus session.** Endpoint
`DELETE /devices` butuh `Authorization` header, dan header itu diambil dari session yang sedang
dihapus. Membalik urutannya menghasilkan 401 dan token yatim di backend yang terus menerima push.

Titik ketiga, `onNewToken()`, sudah terpasang di [05 §5.4](05-messaging-service.md#54-bcafmessagingservicekt).

## 6.7 Checklist bagian ini

- [ ] `kotlinx-coroutines-play-services` ditambahkan
- [ ] Sync dijalankan di: app start (sudah login), login sukses, dan `onNewToken()`
- [ ] Token lokal ditulis **hanya** setelah backend menjawab sukses
- [ ] `logout()` melepas token **sebelum** menghapus session
- [ ] Bentuk envelope untuk response tanpa body sudah disepakati dengan backend
- [ ] Backend memindahkan kepemilikan token saat device dipakai user berbeda

---

Sebelumnya: [05 — Messaging Service](05-messaging-service.md) ·
Lanjut: [07 — Deep Link & Navigasi](07-deep-link-dan-navigasi.md)
