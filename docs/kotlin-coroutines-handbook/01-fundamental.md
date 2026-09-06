# 01 — Fundamental Kotlin Coroutines

> Konsep dasar yang benar-benar dipakai setiap hari, dengan contoh kode pendek untuk masing-masing.

## Model mental utama

```text
CoroutineScope        → siapa pemilik coroutine, kapan dibatalkan
    ↓ menyimpan
CoroutineContext      → kumpulan konfigurasi (Map immutable)
    ↓ minimal berisi
Dispatcher + Job      → jalan di thread mana + lifecycle coroutine
```

Tiga kalimat yang perlu diingat:

1. **Scope** menjawab "coroutine ini milik siapa dan hidup sampai kapan".
2. **Context** menjawab "coroutine ini jalan dengan konfigurasi apa".
3. **Job** menjawab "coroutine ini masih hidup, sudah selesai, atau sudah dibatalkan".

---

## `suspend`

`suspend` menandai fungsi yang **bisa berhenti sementara tanpa memblokir thread**, lalu dilanjutkan lagi nanti.

```kotlin
suspend fun getUser(id: String): User {
    val response = api.getUser(id)
    return response.toDomain()
}
```

Yang perlu dipahami:

- `suspend` **bukan** berarti "jalan di background". Fungsi `suspend` berjalan di thread pemanggilnya sampai ada yang memindahkan.
- `suspend fun` hanya bisa dipanggil dari `suspend fun` lain atau dari dalam coroutine.
- **Kontrak wajib:** setiap `suspend fun` publik harus **main-safe**, yaitu aman dipanggil dari `Dispatchers.Main`. Yang bertanggung jawab memastikan main-safety adalah fungsi itu sendiri, bukan pemanggilnya.

```kotlin
// Salah: pemanggil dipaksa tahu detail threading
suspend fun readFileUnsafe(): String = File("data.json").readText()

// Benar: fungsi menjaga main-safety sendiri
suspend fun readFile(): String = withContext(Dispatchers.IO) {
    File("data.json").readText()
}
```

---

## `CoroutineScope`

`CoroutineScope` adalah pemilik coroutine. Scope memegang `CoroutineContext` (yang di dalamnya ada `Job`), dan **membatalkan semua child** ketika scope dibatalkan.

```kotlin
class SyncManager(private val scope: CoroutineScope) {

    fun start() {
        scope.launch { syncEverything() }
    }
}
```

Di Android, hampir selalu memakai scope yang sudah disediakan framework:

| Scope | Pemilik | Dibatalkan saat |
|---|---|---|
| `viewModelScope` | ViewModel | `onCleared()` |
| `lifecycleScope` | Activity/Fragment | `onDestroy()` |
| `rememberCoroutineScope()` | Composition | composable keluar dari composition |

> Jangan buat `CoroutineScope` baru tanpa tahu siapa yang akan membatalkannya. Lihat [03 — Scope Android](03-scope-android.md).

---

## `CoroutineContext`

`CoroutineContext` adalah kumpulan elemen konfigurasi yang bisa digabungkan dengan operator `+`.

```kotlin
val context = Dispatchers.IO + SupervisorJob() + CoroutineName("sync")
val scope = CoroutineScope(context)
```

Elemen yang sering dipakai:

| Elemen | Fungsi |
|---|---|
| `Job` / `SupervisorJob` | Lifecycle dan hubungan parent-child. |
| `CoroutineDispatcher` | Menentukan thread pool. |
| `CoroutineName` | Nama untuk debugging/log. |
| `CoroutineExceptionHandler` | Penangan exception terakhir pada root coroutine. |

Child coroutine **mewarisi** context dari parent, kecuali `Job` (selalu dibuat baru sebagai child) dan elemen yang di-override.

```kotlin
viewModelScope.launch {                 // Dispatchers.Main.immediate
    withContext(Dispatchers.Default) {  // hanya dispatcher yang diganti
        heavyComputation()
    }
}
```

---

## `Job`

`Job` adalah handle ke lifecycle sebuah coroutine.

```kotlin
val job = viewModelScope.launch { loadData() }

job.isActive     // masih berjalan
job.isCompleted  // sudah selesai (sukses / gagal / dibatalkan)
job.isCancelled  // dibatalkan
job.cancel()     // batalkan coroutine ini beserta seluruh child-nya
job.join()       // suspend sampai job selesai
```

Pola yang sering dipakai: membatalkan request lama sebelum menjalankan yang baru.

```kotlin
private var searchJob: Job? = null

fun search(query: String) {
    searchJob?.cancel()
    searchJob = viewModelScope.launch {
        delay(300)
        _uiState.update { it.copy(results = repository.search(query)) }
    }
}
```

Aturan `Job` biasa: **satu child gagal → parent dibatalkan → semua sibling ikut dibatalkan.**

---

## `SupervisorJob`

`SupervisorJob` mengubah aturan di atas: kegagalan satu child **tidak** membatalkan sibling-nya.

```kotlin
val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

scope.launch { throw IllegalStateException("gagal") } // tidak menjatuhkan sibling
scope.launch { tetapJalan() }
```

- `viewModelScope` **sudah** memakai `SupervisorJob`, jadi satu `launch` yang gagal tidak mematikan `launch` lain di ViewModel yang sama.
- `SupervisorJob` hanya berlaku untuk **child langsung** dari scope tersebut. Untuk pembatasan di dalam satu coroutine, pakai `supervisorScope`.

---

## `launch`

`launch` menjalankan coroutine **tanpa nilai kembalian** (fire and forget dengan pemilik yang jelas). Mengembalikan `Job`.

```kotlin
fun refresh() {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        val user = getUserUseCase()
        _uiState.update { it.copy(isLoading = false, user = user) }
    }
}
```

Pakai `launch` ketika hasilnya **tidak diambil sebagai nilai**, melainkan diterapkan ke state atau side effect.

---

## `async` dan `await`

`async` menjalankan coroutine yang **menghasilkan nilai**. Mengembalikan `Deferred<T>`, nilainya diambil dengan `await()`.

```kotlin
suspend fun loadDashboard(): Dashboard = coroutineScope {
    val profile = async { repository.getProfile() }
    val transactions = async { repository.getTransactions() }

    Dashboard(profile.await(), transactions.await())
}
```

Aturan pemakaian:

- Gunakan `async` **hanya** ketika ada minimal dua pekerjaan yang benar-benar berjalan bersamaan.
- `async { ... }.await()` yang langsung berurutan **tidak paralel** dan hanya menambah kompleksitas — panggil fungsinya langsung.
- Panggil `async` di dalam `coroutineScope`/`supervisorScope`, bukan langsung dari `viewModelScope` untuk pekerjaan yang saling terkait.

```kotlin
// Salah: tidak ada concurrency, hanya boilerplate
val user = async { repository.getUser() }.await()

// Benar
val user = repository.getUser()
```

Detail lengkap di [06 — Sequential vs Concurrent](06-sequential-concurrent.md).

---

## `withContext`

`withContext` **memindahkan context** (biasanya dispatcher) untuk satu blok kode, menunggu hasilnya, lalu kembali ke context semula.

```kotlin
suspend fun parseLargeJson(raw: String): Report = withContext(Dispatchers.Default) {
    json.decodeFromString(raw)
}
```

- `withContext` **sequential**, bukan alat untuk paralel.
- Jangan pakai `withContext(Dispatchers.IO)` untuk membungkus Retrofit `suspend` atau Room `suspend` — keduanya sudah main-safe. Lihat [02 — Dispatcher](02-dispatcher.md).

---

## `coroutineScope`

`coroutineScope` membuat scope **anak** di dalam `suspend fun`, dan baru selesai setelah semua child-nya selesai.

```kotlin
suspend fun syncAll() = coroutineScope {
    launch { syncUsers() }
    launch { syncTransactions() }
}
```

Sifat penting:

- Fungsi tidak akan return sebelum semua child selesai — tidak ada coroutine yang bocor.
- Jika satu child gagal, **semua** child dibatalkan dan exception dilempar ke pemanggil.
- Inilah cara yang benar agar `suspend fun` bisa menjalankan pekerjaan paralel tanpa membuat `CoroutineScope` sendiri.

---

## `supervisorScope`

Sama seperti `coroutineScope`, tetapi kegagalan satu child **tidak** membatalkan child lainnya.

```kotlin
suspend fun loadDashboard(): DashboardData = supervisorScope {
    val profile = async { repository.getProfile() }
    val notifications = async { repository.getNotifications() }

    DashboardData(
        profile = profile.await(),
        notifications = runCatching { notifications.await() }.getOrNull(),
    )
}
```

Gunakan ketika sebagian data **boleh gagal** tanpa menggagalkan seluruh layar. Ingat: dengan `async`, exception tetap muncul saat `await()`, jadi tetap perlu `try/catch` atau `runCatching`.

---

## `delay`

`delay` menunda coroutine **tanpa memblokir thread**.

```kotlin
viewModelScope.launch {
    delay(300)              // debounce sederhana
    search(query)
}
```

- `delay` bersifat cancellable: begitu coroutine dibatalkan, `delay` langsung berhenti.
- Jangan pakai `Thread.sleep()` di dalam coroutine — itu memblokir thread sungguhan.

---

## Structured Concurrency

Structured concurrency berarti **setiap coroutine punya parent**, dan parent tidak selesai sebelum semua child selesai.

```text
viewModelScope (SupervisorJob)
        │
        └── launch  ──────────── Job A
                    │
                    ├── async ── Job A.1
                    └── async ── Job A.2
```

Yang dijamin oleh aturan ini:

| Jaminan | Artinya di praktik |
|---|---|
| Tidak ada coroutine yatim | ViewModel dibersihkan → semua request otomatis berhenti. |
| Error terpropagasi | Kegagalan child tidak hilang diam-diam. |
| Cancellation menurun ke bawah | `cancel()` di parent membatalkan seluruh subtree. |
| Parent menunggu child | `coroutineScope` selesai setelah semua pekerjaan tuntas. |

Yang **merusak** structured concurrency: `GlobalScope`, membuat `CoroutineScope(Job())` lokal di dalam fungsi, dan menjalankan `launch` dari dalam objek yang tidak punya lifecycle.

---

## Coroutine Cancellation

Cancellation di Kotlin bersifat **cooperative**: coroutine hanya berhenti pada titik suspend, atau saat kode memeriksa status aktif sendiri.

```kotlin
val job = viewModelScope.launch {
    repeat(1_000) { index ->
        ensureActive()          // titik pembatalan eksplisit
        processChunk(index)
    }
}

job.cancel()
```

- Semua fungsi suspend dari library coroutine (`delay`, `withContext`, `await`, `Flow.collect`) sudah cancellable.
- Loop CPU-bound tanpa suspend **tidak** akan berhenti sendiri. Sisipkan `ensureActive()` atau `yield()`.
- Cancellation dilaporkan lewat `CancellationException`. **Jangan ditelan.**

```kotlin
try {
    repository.getUser()
} catch (cancellation: CancellationException) {
    throw cancellation                     // wajib diteruskan
} catch (throwable: Throwable) {
    handleError(throwable)
}
```

Pola ini sudah dipakai di `core/network/ApiCall.kt` pada proyek ini — `runApiCatching` melempar ulang `CancellationException` sebelum menangani error lain. Bahasan lengkap di [08 — Cancellation](08-cancellation.md).

---

## Ringkasan bab

| API | Untuk apa | Jangan dipakai untuk |
|---|---|---|
| `suspend` | Menandai fungsi async yang main-safe | Menandai fungsi supaya "jalan di background" |
| `CoroutineScope` | Menentukan pemilik dan umur coroutine | Dibuat manual di layer data |
| `CoroutineContext` | Konfigurasi coroutine | Menyimpan state aplikasi |
| `Job` | Kontrol lifecycle satu coroutine | Menyimpan hasil (pakai `Deferred`) |
| `SupervisorJob` | Isolasi kegagalan antar child scope | Menutupi error yang seharusnya ditangani |
| `launch` | Menjalankan side effect | Mengambil nilai kembalian |
| `async` + `await` | Menjalankan pekerjaan paralel | Satu operasi tunggal |
| `withContext` | Ganti dispatcher untuk satu blok | Membungkus library yang sudah main-safe |
| `coroutineScope` | Paralel di dalam suspend fun | Menjalankan pekerjaan yang harus lanjut setelah caller batal |
| `supervisorScope` | Paralel yang sebagian boleh gagal | Kasus di mana semua data wajib ada |
| `delay` | Menunda tanpa blokir | Polling tanpa kondisi berhenti |

Lanjut ke [02 — Dispatcher](02-dispatcher.md).
