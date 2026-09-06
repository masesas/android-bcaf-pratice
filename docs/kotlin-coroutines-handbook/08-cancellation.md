# 08 — Cancellation

> Cancellation adalah mekanisme utama yang membuat coroutine aman di Android. Sebagian besar kebocoran memori dan crash "view sudah null" berasal dari coroutine yang tidak ikut dibatalkan.

## Prinsip dasar

1. Cancellation bersifat **cooperative**: coroutine berhenti pada titik suspend atau saat kode memeriksa status aktifnya.
2. Cancellation menurun ke seluruh child (structured concurrency).
3. Cancellation dilaporkan lewat `CancellationException` — ini **bukan** error aplikasi.
4. Coroutine yang sudah dibatalkan **tidak bisa** menjalankan fungsi suspend lagi (kecuali dengan `NonCancellable`).

---

## Cancellation otomatis berdasarkan lifecycle

| Scope | Dibatalkan saat | Efek |
|---|---|---|
| `viewModelScope` | `ViewModel.onCleared()` | Semua request layar berhenti |
| `lifecycleScope` | Activity/Fragment `DESTROYED` | Animasi & collect berhenti |
| `viewLifecycleOwner.lifecycleScope` | View Fragment dihancurkan | Aman dari `binding` null |
| `repeatOnLifecycle(STARTED)` | Turun di bawah `STARTED` | Collect berhenti saat background, jalan lagi saat kembali |
| `rememberCoroutineScope()` | Composable keluar composition | Snackbar/animasi berhenti |
| `LaunchedEffect(key)` | Keluar composition atau `key` berubah | Efek lama dibatalkan sebelum yang baru jalan |

### ViewModel destroyed

```kotlin
class ProfileViewModel : ViewModel() {

    fun load() {
        viewModelScope.launch {
            val user = repository.getUser()      // request dibatalkan bila ViewModel di-clear
            _uiState.update { it.copy(user = user) }
        }
    }

    override fun onCleared() {
        super.onCleared()                        // viewModelScope sudah dibatalkan di sini
        connection.close()                       // bersihkan resource non-coroutine
    }
}
```

`viewModelScope` **tidak** dibatalkan saat rotasi layar, karena ViewModel bertahan. Ini alasan business logic ditaruh di ViewModel, bukan di Activity.

### Activity / Fragment lifecycle

```kotlin
// Fragment: gunakan viewLifecycleOwner supaya coroutine mati bersama view
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state -> binding.name.text = state.name }
    }
}
```

Tanpa `viewLifecycleOwner`, coroutine masih hidup saat Fragment masuk back stack, dan `binding` bisa sudah null → crash.

### Compose lifecycle

```kotlin
@Composable
fun ProfileScreen(userId: String) {
    LaunchedEffect(userId) {
        viewModel.load(userId)     // dibatalkan saat userId berubah atau composable keluar
    }
}
```

```kotlin
val state by viewModel.uiState.collectAsStateWithLifecycle()
// collection berhenti saat layar tidak STARTED, lanjut lagi saat kembali
```

---

## Cancellation manual

### Membatalkan satu `Job`

```kotlin
private var uploadJob: Job? = null

fun startUpload(uri: Uri) {
    uploadJob = viewModelScope.launch { uploadUseCase(uri) }
}

fun cancelUpload() {
    uploadJob?.cancel()
    uploadJob = null
}
```

### Membatalkan dan menunggu selesai

```kotlin
suspend fun restart() {
    job?.cancelAndJoin()
    job = viewModelScope.launch { work() }
}
```

### Membatalkan seluruh custom scope

```kotlin
class ConnectionManager : Closeable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun close() {
        scope.cancel()
    }
}
```

Jangan panggil `viewModelScope.cancel()` atau `lifecycleScope.cancel()` — scope tersebut dikelola framework dan tidak bisa dipakai lagi setelah dibatalkan.

---

## Membuat kode kooperatif terhadap cancellation

Fungsi suspend dari library coroutine sudah cancellable. Yang bermasalah adalah loop CPU-bound.

```kotlin
// Salah: tidak pernah berhenti walau dibatalkan
viewModelScope.launch(Dispatchers.Default) {
    while (true) {
        computeNextFrame()
    }
}

// Benar
viewModelScope.launch(Dispatchers.Default) {
    while (isActive) {
        computeNextFrame()
    }
}
```

Pilihan pemeriksaan:

| API | Perilaku |
|---|---|
| `isActive` | Properti boolean; cocok untuk kondisi loop |
| `ensureActive()` | Melempar `CancellationException` bila sudah dibatalkan |
| `yield()` | Memberi kesempatan coroutine lain sekaligus memeriksa cancellation |

```kotlin
suspend fun processAll(items: List<Item>) = withContext(Dispatchers.Default) {
    items.forEach { item ->
        ensureActive()
        process(item)
    }
}
```

---

## Membersihkan resource saat dibatalkan

```kotlin
suspend fun downloadTo(target: File) {
    val stream = openStream()
    try {
        stream.copyTo(target)
    } finally {
        stream.close()                     // dijalankan walau coroutine dibatalkan
    }
}
```

Kalau pembersihannya butuh fungsi suspend, bungkus dengan `NonCancellable`:

```kotlin
suspend fun startSession() {
    try {
        runSession()
    } finally {
        withContext(NonCancellable) {
            repository.releaseSession()     // suspend, tetap dijalankan walau sudah dibatalkan
        }
    }
}
```

Gunakan `NonCancellable` **hanya** untuk pembersihan singkat, bukan untuk memaksa pekerjaan panjang tetap berjalan.

---

## Timeout

```kotlin
suspend fun getUserWithTimeout(): User? = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
    repository.getUser()
}
```

| API | Bila waktu habis |
|---|---|
| `withTimeout(ms)` | Melempar `TimeoutCancellationException` |
| `withTimeoutOrNull(ms)` | Mengembalikan `null` |

`TimeoutCancellationException` adalah turunan `CancellationException`, jadi `catch (Throwable)` polos akan menangkapnya juga — perhatikan urutan `catch`.

---

## Cancellation di layer data

Fungsi yang menerjemahkan exception **wajib** meneruskan `CancellationException`, seperti pada `runApiCatching` proyek ini:

```kotlin
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (http: HttpException) {
    AppResult.failure(http.toFailure(json))
}
```

Kalau `CancellationException` ikut diubah menjadi `AppResult.Failure`, akibatnya:

- Layar yang sudah ditutup tetap "menerima" error dan mencoba mengubah state.
- Log penuh error palsu setiap kali user berpindah layar.
- Structured concurrency rusak: parent mengira child selesai normal.

---

## Kesalahan umum

```kotlin
// 1. Menelan cancellation
catch (throwable: Throwable) { showError(throwable) }

// 2. Mengabaikan cancellation di loop berat
while (true) { heavyWork() }

// 3. Membatalkan scope milik framework
viewModelScope.cancel()

// 4. Menyimpan referensi View di coroutine yang lebih panjang umurnya
lifecycleScope.launch { delay(10_000); binding.text.text = "..." }   // pakai viewLifecycleOwner

// 5. Melanjutkan update state setelah cancellation
job.cancel()
_uiState.update { it.copy(isLoading = false) }   // pastikan update ini memang diinginkan
```

---

## Checklist cancellation

- [ ] Coroutine yang menyentuh view memakai `viewLifecycleOwner.lifecycleScope`.
- [ ] Collect `Flow` di View memakai `repeatOnLifecycle`.
- [ ] Loop CPU-bound memeriksa `isActive`/`ensureActive()`.
- [ ] `CancellationException` selalu diteruskan.
- [ ] Pembersihan resource ada di blok `finally` (pakai `NonCancellable` bila suspend).
- [ ] Request yang bisa diganti (search, filter) menyimpan `Job` dan membatalkan yang lama.
- [ ] Tidak ada `cancel()` pada scope milik framework.

Lanjut ke [09 — Real-World Example](09-real-world-example.md).
