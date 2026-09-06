# 07 — Error Handling

> Aturan intinya: **tangkap error sedekat mungkin dengan sumbernya, ubah menjadi tipe hasil, dan jangan pernah menelan `CancellationException`.**

## Cara exception menyebar

```text
launch  → exception langsung dilempar ke parent saat terjadi
async   → exception disimpan di Deferred, dilempar saat await()
```

Konsekuensinya:

- `launch` yang gagal pada `Job` biasa akan membatalkan parent dan semua sibling.
- `launch` yang gagal pada `SupervisorJob` (misalnya `viewModelScope`) hanya membatalkan dirinya sendiri.
- `async` yang gagal dan **tidak pernah di-`await`** bisa membuat error tidak terlihat — pada `coroutineScope` tetap membatalkan scope, pada `supervisorScope` bisa hilang diam-diam.

---

## `try/catch`

Cara paling langsung dan paling sering benar.

```kotlin
viewModelScope.launch {
    _uiState.update { it.copy(isLoading = true, error = null) }

    try {
        val user = getUserUseCase()
        _uiState.update { it.copy(isLoading = false, user = user) }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        _uiState.update { it.copy(isLoading = false, error = throwable.toMessage()) }
    }
}
```

Poin penting:

- **Urutan `catch` wajib**: `CancellationException` lebih dulu, lalu `Throwable`.
- `try/catch` di sekitar `coroutineScope { }` menangkap kegagalan child-nya.
- `try/catch` di dalam satu `launch` **tidak** menangkap kegagalan `launch` lain.

```kotlin
// Salah: catch tidak menangkap kegagalan child launch
try {
    viewModelScope.launch { throw IllegalStateException() }
} catch (throwable: Throwable) {
    // tidak pernah dieksekusi
}

// Benar
viewModelScope.launch {
    try {
        riskyWork()
    } catch (throwable: Throwable) {
        handle(throwable)
    }
}
```

---

## Mengubah exception menjadi hasil bertipe

Pendekatan yang dipakai proyek ini: error jaringan diterjemahkan di layer data menjadi `AppResult.Failure`, sehingga ViewModel tidak perlu `try/catch` untuk kasus normal.

```kotlin
suspend fun <T> runApiCatching(
    json: Json,
    block: suspend () -> AppResult<T>,
): AppResult<T> = try {
    block()
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (http: HttpException) {
    AppResult.failure(http.toFailure(json))
} catch (io: IOException) {
    AppResult.failure(CommonFailure.Network(io))
} catch (throwable: Throwable) {
    AppResult.failure(CommonFailure.Unexpected(throwable))
}
```

```kotlin
when (val result = getProfile()) {
    is AppResult.Success -> _uiState.update { it.copy(user = result.data) }
    is AppResult.Failure -> _uiState.update { it.copy(error = result.failure.toMessage()) }
}
```

Keuntungan: alur error menjadi eksplisit di signature, tidak bergantung pada disiplin `try/catch` di setiap pemanggil.

---

## `CoroutineExceptionHandler`

Handler terakhir untuk exception yang **tidak tertangkap** pada root coroutine.

```kotlin
private val errorHandler = CoroutineExceptionHandler { _, throwable ->
    Log.e(TAG, "Unhandled coroutine error", throwable)
    _uiState.update { it.copy(isLoading = false, error = GENERIC_ERROR) }
}

fun load() {
    viewModelScope.launch(errorHandler) {
        _uiState.update { it.copy(user = getUserUseCase()) }
    }
}
```

Batasannya:

- Hanya bekerja pada **root coroutine** (`launch` langsung dari scope). Memasangnya di `launch` bagian dalam tidak berpengaruh.
- **Tidak berlaku untuk `async`** — exception `async` muncul di `await()`.
- Bukan pengganti `try/catch`; anggap sebagai jaring pengaman/logging, bukan penanganan error utama.

---

## `SupervisorJob`

Membuat kegagalan satu child tidak menjatuhkan sibling-nya.

```kotlin
val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + errorHandler)

scope.launch { syncProfile() }        // gagal → hanya ini yang mati
scope.launch { syncTransactions() }   // tetap jalan
```

`viewModelScope` sudah memakai `SupervisorJob`, sehingga:

```kotlin
viewModelScope.launch { throw IllegalStateException() }  // ViewModel tetap sehat
viewModelScope.launch { loadProfile() }                  // tetap jalan
```

Tapi ingat: exception yang tidak tertangkap pada `viewModelScope.launch` tetap **meng-crash aplikasi** kecuali ada `CoroutineExceptionHandler`. `SupervisorJob` hanya mengatur propagasi pembatalan, bukan menelan error.

---

## `supervisorScope`

Versi `suspend` dari `SupervisorJob` untuk satu blok kode.

```kotlin
suspend fun syncAll(): SyncReport = supervisorScope {
    val profile = async { syncProfile() }
    val transactions = async { syncTransactions() }

    SyncReport(
        profileOk = profile.awaitCatching(),
        transactionsOk = transactions.awaitCatching(),
    )
}

private suspend fun Deferred<*>.awaitCatching(): Boolean = try {
    await()
    true
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    Log.w(TAG, "Sync gagal", throwable)
    false
}
```

`coroutineScope` vs `supervisorScope`:

| | `coroutineScope` | `supervisorScope` |
|---|---|---|
| Child gagal | Semua sibling dibatalkan | Sibling tetap jalan |
| Exception ke pemanggil | Dilempar | Hanya bila di-`await` dan tidak ditangkap |
| Dipakai saat | Semua data wajib berhasil | Sebagian data boleh gagal |

---

## Kasus: satu request gagal, yang lain tetap boleh jalan

Layar dashboard, notifikasi boleh kosong bila endpoint-nya bermasalah.

```kotlin
suspend fun loadDashboard(): DashboardUiModel = supervisorScope {
    val profile = async { profileRepository.getProfile() }
    val transactions = async { transactionRepository.getRecent() }
    val notifications = async { notificationRepository.getUnread() }

    DashboardUiModel(
        profile = profile.await(),                                   // wajib, boleh melempar
        transactions = transactions.awaitOrNull().orEmpty(),         // opsional
        notifications = notifications.awaitOrNull().orEmpty(),       // opsional
    )
}

private suspend fun <T> Deferred<T>.awaitOrNull(): T? = try {
    await()
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    null
}
```

Dengan konvensi `AppResult`, pola ini menjadi lebih sederhana — lihat [06 — Sequential vs Concurrent](06-sequential-concurrent.md#versi-3--dengan-appresult-konvensi-proyek-ini).

---

## Error pada Flow

```kotlin
val uiState: StateFlow<ProfileUiState> = repository.observeProfile()
    .map { ProfileUiState(user = it) }
    .catch { throwable -> emit(ProfileUiState(error = throwable.toMessage())) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState(isLoading = true))
```

- `catch` hanya menangkap error dari **upstream**, bukan dari collector.
- `catch` tidak menangkap `CancellationException` (memang tidak boleh).
- `retry`/`retryWhen` berguna untuk error jaringan sementara.

```kotlin
repository.observeProfile()
    .retryWhen { cause, attempt -> cause is IOException && attempt < MAX_RETRY }
```

---

## Anti-pattern error handling

```kotlin
// 1. Menelan CancellationException
try {
    repository.getUser()
} catch (throwable: Throwable) {   // ikut menangkap CancellationException
    showError()
}
```

```kotlin
// 2. runCatching tanpa memeriksa cancellation
val result = runCatching { repository.getUser() }   // juga menangkap CancellationException
```

```kotlin
// 3. catch kosong
try {
    sync()
} catch (throwable: Throwable) {
    // diam saja
}
```

```kotlin
// 4. Mengandalkan CoroutineExceptionHandler untuk error yang bisa diprediksi
viewModelScope.launch(handler) { api.getUser() }   // error jaringan bukan "unhandled"
```

Versi yang benar untuk `runCatching`:

```kotlin
val result = runCatching { repository.getUser() }
    .onFailure { if (it is CancellationException) throw it }
```

---

## Checklist error handling

- [ ] Setiap `catch (Throwable)` didahului `catch (CancellationException) { throw it }`.
- [ ] Error jaringan/database diterjemahkan di layer data menjadi tipe hasil (`AppResult`).
- [ ] ViewModel selalu mengembalikan state ke `isLoading = false` di jalur error.
- [ ] `CoroutineExceptionHandler` hanya sebagai jaring pengaman/logging.
- [ ] `async` yang gagal selalu di-`await` di suatu tempat.
- [ ] `supervisorScope` dipakai hanya bila kegagalan parsial memang diinginkan.
- [ ] Pesan error yang tampil ke user tidak membocorkan detail teknis.

Lanjut ke [08 — Cancellation](08-cancellation.md).
