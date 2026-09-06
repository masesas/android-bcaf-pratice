# 11 — Anti-Pattern

> Setiap bagian berisi kode yang salah, alasannya, dan versi yang direkomendasikan. Pakai bab ini sebagai checklist saat code review.

## 1. `GlobalScope`

```kotlin
// Hindari
fun refresh() {
    GlobalScope.launch {
        repository.getUser()
    }
}
```

Masalah: tidak pernah dibatalkan, keluar dari structured concurrency, bocor memori, tidak bisa dites.

```kotlin
// Rekomendasi: pemilik yang jelas
fun refresh() {
    viewModelScope.launch {
        _uiState.update { it.copy(user = repository.getUser()) }
    }
}
```

Bila memang harus hidup lebih lama dari layar, gunakan application scope yang di-inject atau `WorkManager`.

---

## 2. Repository membuat coroutine tanpa alasan

```kotlin
// Hindari
class UserRepositoryImpl(private val api: UserApi) {

    private val scope = CoroutineScope(Dispatchers.IO)

    fun getUser() {
        scope.launch { api.getUser() }
    }
}
```

Masalah: hasil tidak bisa diambil pemanggil, tidak ikut dibatalkan saat layar tutup, error hilang, tidak bisa dites.

```kotlin
// Rekomendasi
class UserRepositoryImpl(private val api: UserApi) : UserRepository {

    override suspend fun getUser(): User = api.getUser().toDomain()
}
```

---

## 3. Nested `launch` tanpa alasan

```kotlin
// Hindari
viewModelScope.launch {
    launch {
        val user = repository.getUser()
        _uiState.update { it.copy(user = user) }
    }
}
```

Masalah: menambah satu level Job tanpa manfaat, urutan eksekusi jadi kabur.

```kotlin
// Rekomendasi
viewModelScope.launch {
    val user = repository.getUser()
    _uiState.update { it.copy(user = user) }
}
```

`launch` bersarang hanya benar bila memang menjalankan beberapa pekerjaan bersamaan:

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { viewModel.uiState.collect(::render) }
        launch { viewModel.events.collect(::handleEvent) }
    }
}
```

---

## 4. `async` tanpa concurrency

```kotlin
// Hindari
val user = async { repository.getUser() }.await()
```

```kotlin
// Rekomendasi
val user = repository.getUser()
```

```kotlin
// Hindari: dua async yang di-await berurutan tetap sequential
val user = async { repository.getUser() }.await()
val transactions = async { repository.getTransactions() }.await()

// Rekomendasi
coroutineScope {
    val user = async { repository.getUser() }
    val transactions = async { repository.getTransactions() }
    Pair(user.await(), transactions.await())
}
```

---

## 5. `Dispatchers.IO` berlebihan

```kotlin
// Hindari
suspend fun getUser(): User = withContext(Dispatchers.IO) {
    api.getUser().toDomain()          // Retrofit sudah main-safe
}

suspend fun loadAll(): List<User> = withContext(Dispatchers.IO) {
    dao.getAll().map { it.toDomain() } // Room suspend sudah main-safe
}
```

```kotlin
// Rekomendasi
suspend fun getUser(): User = api.getUser().toDomain()

suspend fun loadAll(): List<User> = dao.getAll().map { it.toDomain() }
```

`withContext(Dispatchers.IO)` tetap benar untuk file, `SharedPreferences.commit()`, atau SDK blocking.

Kesalahan turunannya: menumpuk `withContext` di setiap layer.

```kotlin
// Hindari: pindah thread 3 kali untuk satu request
// ViewModel
withContext(Dispatchers.IO) { useCase() }
// UseCase
withContext(Dispatchers.IO) { repository.getUser() }
// Repository
withContext(Dispatchers.IO) { api.getUser() }
```

---

## 6. Custom `CoroutineScope` tanpa lifecycle

```kotlin
// Hindari
class SyncManager {

    private val scope = CoroutineScope(Job())

    fun sync() {
        scope.launch { repository.syncAll() }
    }
}
```

Masalah: tidak ada yang membatalkan scope, dan `Job()` biasa membuat satu kegagalan mematikan seluruh scope selamanya.

```kotlin
// Rekomendasi: scope di-inject dan bisa dibatalkan
class SyncManager @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun sync() {
        scope.launch { repository.syncAll() }
    }
}
```

Untuk komponen yang punya akhir hidup, sediakan pembersihan:

```kotlin
class SyncManager : Closeable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun close() = scope.cancel()
}
```

---

## 7. Swallowing exception

```kotlin
// Hindari
try {
    repository.getUser()
} catch (throwable: Throwable) {
    // diam
}
```

```kotlin
// Hindari juga: ikut menelan CancellationException
try {
    repository.getUser()
} catch (throwable: Throwable) {
    _uiState.update { it.copy(error = throwable.message) }
}
```

```kotlin
// Rekomendasi
try {
    repository.getUser()
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    Log.e(TAG, "getUser gagal", throwable)
    _uiState.update { it.copy(isLoading = false, error = throwable.toUiMessage()) }
}
```

Sama berlakunya untuk `runCatching`, yang secara default juga menangkap `CancellationException`.

---

## 8. Business logic langsung dari Compose

```kotlin
// Hindari
@Composable
fun ProfileScreen(repository: UserRepository) {
    val scope = rememberCoroutineScope()

    Button(onClick = {
        scope.launch { repository.getUser() }      // UI memanggil data layer
    }) {
        Text("Muat")
    }
}
```

Masalah: hasil hilang saat recomposition, tidak ada state yang bertahan, tidak bisa dites, mati saat rotasi.

```kotlin
// Rekomendasi
@Composable
fun ProfileRoute(viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileScreen(state = state, onLoad = viewModel::load)
}
```

`rememberCoroutineScope` tetap benar untuk UI murni (snackbar, scroll, sheet).

---

## 9. Menjalankan coroutine di body composable

```kotlin
// Hindari: dijalankan ulang setiap recomposition
@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    rememberCoroutineScope().launch { viewModel.load() }
}
```

```kotlin
// Rekomendasi
@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    LaunchedEffect(Unit) { viewModel.load() }
}
```

---

## 10. Collect `Flow` tanpa lifecycle di View

```kotlin
// Hindari
lifecycleScope.launch {
    viewModel.uiState.collect { state -> render(state) }
}
```

```kotlin
// Hindari juga: sudah deprecated
lifecycleScope.launchWhenStarted {
    viewModel.uiState.collect(::render)
}
```

```kotlin
// Rekomendasi
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect(::render)
    }
}
```

---

## 11. `runBlocking` di kode produksi

```kotlin
// Hindari
fun getUserBlocking(): User = runBlocking { repository.getUser() }
```

Masalah: memblokir thread pemanggil; bila dipanggil dari main thread, langsung ANR.

```kotlin
// Rekomendasi
suspend fun getUser(): User = repository.getUser()
```

`runBlocking` hanya wajar di `main()` aplikasi non-Android dan sebagian test lama (`runTest` lebih baik).

---

## 12. Mutasi state tidak atomik

```kotlin
// Hindari: rawan race saat beberapa coroutine menulis bersamaan
_uiState.value = _uiState.value.copy(isLoading = true)
```

```kotlin
// Rekomendasi
_uiState.update { it.copy(isLoading = true) }
```

---

## 13. Membocorkan `Context`/`View` ke coroutine panjang

```kotlin
// Hindari
lifecycleScope.launch {
    delay(30_000)
    binding.banner.isVisible = true      // binding bisa sudah null
}
```

```kotlin
// Rekomendasi
viewLifecycleOwner.lifecycleScope.launch {
    delay(30_000)
    binding.banner.isVisible = true
}
```

---

## 14. Loop berat yang tidak kooperatif

```kotlin
// Hindari
viewModelScope.launch(Dispatchers.Default) {
    while (true) { recompute() }
}
```

```kotlin
// Rekomendasi
viewModelScope.launch(Dispatchers.Default) {
    while (isActive) { recompute() }
}
```

---

## 15. Mengekspos `MutableStateFlow` ke UI

```kotlin
// Hindari
val uiState = MutableStateFlow(ProfileUiState())
```

```kotlin
// Rekomendasi
private val _uiState = MutableStateFlow(ProfileUiState())
val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
```

---

## Ringkasan review

| Cari di kode | Tindakan |
|---|---|
| `GlobalScope` | Ganti dengan scope yang punya pemilik |
| `CoroutineScope(` di layer data | Hapus, ekspos `suspend fun`/`Flow` |
| `withContext(Dispatchers.IO)` membungkus Retrofit/Room | Hapus |
| `async { }.await()` berurutan | Panggil langsung atau paralelkan dengan benar |
| `catch (Throwable)` tanpa `CancellationException` | Tambahkan `catch` cancellation lebih dulu |
| `launchWhenStarted` / `collectAsState()` | Ganti ke `repeatOnLifecycle` / `collectAsStateWithLifecycle` |
| `runBlocking` | Ubah jadi `suspend fun` |
| `_uiState.value = _uiState.value.copy(...)` | Ganti ke `update { }` |
| `lifecycleScope` di Fragment yang menyentuh view | Ganti ke `viewLifecycleOwner.lifecycleScope` |

Lanjut ke [12 — API Reference](12-api-reference.md).
