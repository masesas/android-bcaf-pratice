# 02 — Dispatcher

> Dispatcher menentukan **thread mana** yang menjalankan coroutine. Salah pilih dispatcher menyebabkan ANR, salah pakai berlebihan menyebabkan kode berisik dan lebih lambat.

## Tabel ringkas

| Dispatcher | Thread | Untuk apa | Jangan dipakai untuk |
|---|---|---|---|
| `Dispatchers.Main` | UI thread | Update UI, state ViewModel | Operasi blocking |
| `Dispatchers.Main.immediate` | UI thread | Sama, tanpa re-dispatch bila sudah di main | — |
| `Dispatchers.IO` | Pool besar (default 64 thread) | Blocking I/O sungguhan | Retrofit/Room `suspend`, kalkulasi CPU |
| `Dispatchers.Default` | Pool sebesar jumlah core | Kalkulasi CPU-bound | Blocking I/O |
| `Dispatchers.Unconfined` | Tidak ditentukan | Kasus khusus & sebagian testing | Kode produksi biasa |

---

## `Dispatchers.Main`

**Fungsi:** menjalankan coroutine di UI thread Android.

**Kapan dipakai:**

- Update `View`, `LiveData`, atau state UI dari coroutine yang saat ini berada di background.
- Hampir selalu **tidak perlu ditulis manual** karena `viewModelScope` dan `lifecycleScope` sudah memakai `Dispatchers.Main.immediate` secara default.

```kotlin
viewModelScope.launch {                      // sudah di Main
    val user = getUserUseCase()              // suspend, main-safe
    _uiState.update { it.copy(user = user) } // aman langsung di sini
}
```

**Kapan tidak perlu:**

```kotlin
// Redundan: viewModelScope sudah Main
viewModelScope.launch {
    val user = getUserUseCase()
    withContext(Dispatchers.Main) {
        _uiState.update { it.copy(user = user) }
    }
}
```

**`Main` vs `Main.immediate`:** `Main.immediate` menjalankan blok langsung bila pemanggil sudah berada di main thread, tanpa menunggu antrian `Handler`. Ini penting agar update state tidak "terlambat satu frame". `viewModelScope` dan `lifecycleScope` sudah memakai `Main.immediate`.

---

## `Dispatchers.IO`

**Fungsi:** pool thread besar yang dirancang untuk operasi **blocking** yang lebih banyak menunggu daripada menghitung.

**Kapan dipakai:**

- Baca/tulis file, `SharedPreferences` versi blocking, `ContentResolver`.
- Library atau SDK lama yang API-nya blocking dan tidak menyediakan `suspend`.
- Query database yang API-nya blocking (bukan Room `suspend`/`Flow`).

```kotlin
suspend fun readCachedProfile(): String? = withContext(Dispatchers.IO) {
    cacheFile.takeIf { it.exists() }?.readText()
}
```

**Kapan TIDAK perlu (ini kesalahan paling sering):**

```kotlin
// Tidak perlu: Retrofit suspend sudah menjalankan request di background
suspend fun getUser(): UserDto = withContext(Dispatchers.IO) {
    api.getUser()
}

// Benar
suspend fun getUser(): UserDto = api.getUser()
```

```kotlin
// Tidak perlu: Room suspend DAO sudah main-safe
suspend fun loadAll(): List<UserEntity> = withContext(Dispatchers.IO) {
    dao.getAll()
}

// Benar
suspend fun loadAll(): List<UserEntity> = dao.getAll()
```

Library yang **sudah menangani threading sendiri** dan tidak butuh `withContext(Dispatchers.IO)`:

| Library | Bentuk API | Catatan |
|---|---|---|
| Retrofit | `suspend fun` | Request dijalankan di executor OkHttp. |
| Room | `suspend fun` DAO, `Flow` query | Room memindahkan ke query executor sendiri. |
| DataStore | `Flow`, `suspend fun edit` | Sudah main-safe. |
| OkHttp `await` (adapter coroutine) | `suspend` | Berbasis callback async. |
| Ktor Client | `suspend` | Main-safe. |

Yang **masih** butuh `Dispatchers.IO`: `File`, `InputStream`, `Bitmap` decode dari disk, `SharedPreferences.commit()`, JDBC/SQLite mentah, dan SDK pihak ketiga yang blocking.

**Biaya `withContext` yang tidak perlu:** setiap `withContext` memaksa perpindahan thread dan penjadwalan ulang. Bila dipakai di semua level (data source, repository, use case), satu request bisa berpindah thread 3–4 kali tanpa manfaat apa pun.

### `limitedParallelism`

Jika perlu membatasi paralelisme untuk resource tertentu (misalnya satu koneksi database atau upload berat), jangan buat thread pool baru.

```kotlin
private val diskDispatcher = Dispatchers.IO.limitedParallelism(4)
```

`limitedParallelism` berbagi thread pool `IO` yang sama, jadi tidak menambah jumlah thread di proses aplikasi.

---

## `Dispatchers.Default`

**Fungsi:** pool thread seukuran jumlah CPU core, untuk pekerjaan **CPU-bound**.

**Kapan dipakai:**

- Parsing JSON besar secara manual, sorting/filtering list besar, kalkulasi finansial berulang, kompresi, image processing di memori.

```kotlin
suspend fun buildAmortization(input: LoanInput): List<Installment> =
    withContext(Dispatchers.Default) {
        (1..input.tenor).map { month -> calculateInstallment(input, month) }
    }
```

**Kapan tidak perlu:**

- Operasi ringan (map beberapa puluh item, format string, konversi DTO → domain). Perpindahan thread lebih mahal daripada pekerjaannya.
- Serialisasi via converter Retrofit — sudah dilakukan di luar main thread oleh OkHttp.

> Patokan praktis: pindahkan ke `Default` bila pekerjaannya berpotensi melebihi ~16 ms (satu frame). Di bawah itu, jangan.

---

## `Dispatchers.Unconfined`

**Fungsi:** tidak mengikat coroutine ke thread tertentu; coroutine berjalan di thread pemanggil sampai suspend pertama, lalu melanjutkan di thread mana pun yang membangunkannya.

**Kapan dipakai:**

- Kasus lanjutan seperti operator `Flow` tertentu.
- Sebagian skenario testing (walau `UnconfinedTestDispatcher` lebih tepat untuk itu).

**Kapan tidak dipakai:** hampir semua kode produksi Android. Perilakunya sulit diprediksi dan mudah menyebabkan update UI dari thread yang salah.

---

## Di mana dispatcher sebaiknya ditentukan

**Aturan:** dispatcher ditentukan **sedekat mungkin dengan operasi yang membutuhkannya**, yaitu di data source atau repository — bukan di ViewModel.

```kotlin
// Salah: ViewModel jadi tahu detail threading layer bawah
viewModelScope.launch {
    val user = withContext(Dispatchers.IO) { repository.getUser() }
}

// Benar: repository menjaga main-safety sendiri, ViewModel bersih
viewModelScope.launch {
    val user = repository.getUser()
}
```

Alasannya: jika suatu saat `repository.getUser()` berubah dari file lokal menjadi Retrofit, ViewModel tidak perlu ikut diubah.

---

## Dispatcher yang di-inject (untuk testing)

Dispatcher yang di-hardcode membuat unit test sulit dikontrol. Injeksikan lewat Hilt bila memang ada perpindahan thread.

```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
```

```kotlin
class FileProfileCache @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    suspend fun read(): String? = withContext(dispatcher) { file.readText() }
}
```

Di test, ganti dengan `StandardTestDispatcher()`. Lihat [10 — Testing](10-testing.md).

> Kalau repository tidak memakai `withContext` sama sekali (karena Retrofit/Room sudah main-safe), **tidak perlu** meng-inject dispatcher. Jangan menambah abstraksi tanpa kebutuhan.

---

## Checklist dispatcher

- [ ] Tidak ada `withContext(Dispatchers.IO)` yang membungkus Retrofit atau Room `suspend`.
- [ ] Tidak ada `withContext(Dispatchers.Main)` di dalam `viewModelScope.launch`.
- [ ] Pekerjaan blocking sungguhan memakai `Dispatchers.IO`.
- [ ] Pekerjaan CPU berat memakai `Dispatchers.Default`.
- [ ] Dispatcher ditentukan di layer data, bukan di ViewModel atau UI.
- [ ] Tidak ada `Dispatchers.Unconfined` di kode produksi.
- [ ] Tidak ada pembuatan thread pool manual (`newFixedThreadPoolContext`); pakai `limitedParallelism`.

Lanjut ke [03 — Scope Android](03-scope-android.md).
