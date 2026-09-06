# Kotlin Coroutines Handbook (Android Modern)

> Referensi kerja harian untuk memakai coroutine di Data Layer, Domain Layer, ViewModel, Activity, Fragment, dan Jetpack Compose.

Handbook ini ditulis sebagai **referensi saat development**, bukan pengantar teori. Setiap bab menjawab tiga pertanyaan yang sama: *apa fungsinya*, *kapan dipakai*, dan **kapan justru tidak perlu dipakai**.

> **Bab utama: [04 — Penggunaan Coroutine Berdasarkan Layer](04-coroutine-per-layer-primary.md) (PRIMARY).** Kalau hanya sempat membaca satu bab, baca itu. Bab lain adalah pendalaman dari aturan yang ditetapkan di sana.

## Cara memakai handbook

| Situasi | Mulai dari |
|---|---|
| Baru pertama kali serius memakai coroutine | [01 — Fundamental](01-fundamental.md) → [02 — Dispatcher](02-dispatcher.md) → [03 — Scope Android](03-scope-android.md) |
| Sedang menulis kode dan bingung "ini taruh di layer mana" | [04 — Coroutine per Layer](04-coroutine-per-layer-primary.md) |
| Butuh jawaban cepat saat coding | [14 — Decision Guide](14-decision-guide.md) dan [12 — API Reference](12-api-reference.md) |
| Sedang review kode orang lain / diri sendiri | [11 — Anti-Pattern](11-anti-pattern.md) dan [15 — Best Practices](15-best-practices.md) |
| Butuh contoh utuh dari API sampai UI | [09 — Real-World Example](09-real-world-example.md) dan [06 — Sequential vs Concurrent](06-sequential-concurrent.md) |

## Daftar isi

| Bab | Isi utama |
|---|---|
| [01 — Fundamental](01-fundamental.md) | `suspend`, `CoroutineScope`, `CoroutineContext`, `Job`, `SupervisorJob`, `launch`, `async`/`await`, `withContext`, `coroutineScope`, `supervisorScope`, `delay`, structured concurrency. |
| [02 — Dispatcher](02-dispatcher.md) | `Main`, `IO`, `Default`, `Unconfined`, main-safety, dan kesalahan `withContext(Dispatchers.IO)` berlebihan. |
| [03 — Scope Android](03-scope-android.md) | `viewModelScope`, `lifecycleScope`, `repeatOnLifecycle`, `rememberCoroutineScope`, custom scope, `GlobalScope`. |
| **[04 — Coroutine per Layer](04-coroutine-per-layer-primary.md) — PRIMARY** | Tanggung jawab coroutine di Repository, UseCase, ViewModel, Activity/Fragment, dan Compose. **Bab utama handbook ini.** |
| [05 — Flow Integration](05-flow-integration.md) | Hubungan coroutine dengan `Flow`, `StateFlow`, `SharedFlow`, cold vs hot, state vs event. |
| [06 — Sequential vs Concurrent](06-sequential-concurrent.md) | Kapan `async`/`await` benar-benar berguna, plus contoh dashboard paralel dengan `supervisorScope`. |
| [07 — Error Handling](07-error-handling.md) | `try/catch`, propagasi exception, `CoroutineExceptionHandler`, `SupervisorJob`, `supervisorScope`. |
| [08 — Cancellation](08-cancellation.md) | Cooperative cancellation, `CancellationException`, `NonCancellable`, `withTimeout`, kaitan dengan lifecycle. |
| [09 — Real-World Example](09-real-world-example.md) | Load User Profile end-to-end: Retrofit → Repository → UseCase → ViewModel → Compose. |
| [10 — Testing](10-testing.md) | `runTest`, test dispatcher, `MainDispatcherRule`, testing suspend function dan ViewModel. |
| [11 — Anti-Pattern](11-anti-pattern.md) | Kode yang salah dan versi yang direkomendasikan. |
| [12 — API Reference](12-api-reference.md) | Tabel API per kategori: builder, scope, dispatcher, job, structured concurrency, Android, Compose. |
| [13 — Diagram & Ownership](13-diagram-ownership.md) | Diagram alur layer, ownership coroutine, dan apa yang terjadi saat ViewModel di-clear. |
| [14 — Decision Guide](14-decision-guide.md) | Cheat sheet "kalau begini → pakai ini". |
| [15 — Best Practices](15-best-practices.md) | Rangkuman rekomendasi final per topik. |

## Prinsip utama

```text
CoroutineScope
    ↓ punya
CoroutineContext
    ↓ minimal berisi
Dispatcher + Job
```

- **Coroutine punya pemilik.** Pemilik menentukan kapan coroutine dibatalkan. Kalau tidak ada pemilik yang jelas, itu bug.
- **Layer bawah tidak membuat coroutine.** Repository dan UseCase mengekspos `suspend fun` atau `Flow`, bukan `launch`.
- **Layer atas yang menjalankan coroutine.** ViewModel (`viewModelScope`) atau UI (`lifecycleScope`, `LaunchedEffect`).
- **`suspend fun` wajib main-safe.** Pemanggil boleh memanggilnya dari `Dispatchers.Main` tanpa memikirkan thread.
- **Jangan pindah thread kalau library sudah melakukannya.** Retrofit `suspend` dan Room `suspend`/`Flow` sudah main-safe.
- **Cancellation adalah fitur, bukan error.** `CancellationException` harus diteruskan, bukan ditelan.
- **Structured concurrency adalah default.** Gunakan `coroutineScope`/`supervisorScope` supaya child job tidak bocor.

## Aturan singkat per layer

| Layer | Boleh membuat coroutine? | API yang dipakai | Yang diekspos |
|---|---|---|---|
| Remote / Local Data Source | Tidak | `suspend fun`, `Flow` | `suspend fun`, `Flow` |
| Repository | Tidak (kecuali cache scope yang di-inject sadar lifecycle) | `withContext` bila memang perlu, `coroutineScope` untuk paralel | `suspend fun`, `Flow` |
| UseCase | Tidak | `coroutineScope`, `supervisorScope`, `async` bila perlu paralel | `suspend fun`, `Flow` |
| ViewModel | Ya | `viewModelScope.launch`, `stateIn` | `StateFlow`, `SharedFlow`, fungsi biasa |
| Activity / Fragment | Ya, untuk urusan UI | `lifecycleScope` + `repeatOnLifecycle` | — |
| Compose | Ya, untuk urusan UI | `LaunchedEffect`, `rememberCoroutineScope`, `collectAsStateWithLifecycle` | — |

## Baseline versi

Handbook ini ditinjau pada **4 September 2026** dan mengikuti dependency proyek ini:

| Dependency | Versi | Relevansi |
|---|---|---|
| Kotlin | `2.4.10` | Membawa `kotlinx-coroutines-core` secara transitif. |
| Lifecycle (`androidx.lifecycle`) | `2.11.0` | `viewModelScope`, `lifecycleScope`, `repeatOnLifecycle`, `collectAsStateWithLifecycle`. |
| Compose BOM | `2026.08.00` | `LaunchedEffect`, `rememberCoroutineScope`, `produceState`. |
| Retrofit | `3.0.0` | Dukungan `suspend fun` native, sudah main-safe. |
| Room | `2.8.4` | `suspend` DAO dan `Flow` query, sudah main-safe. |
| Hilt | `2.60.1` | Injeksi repository/use case ke ViewModel. |

Untuk testing, tambahkan `org.jetbrains.kotlinx:kotlinx-coroutines-test` (lihat [10 — Testing](10-testing.md)).

## API yang sudah tidak dipakai lagi

| Jangan pakai | Pakai ini |
|---|---|
| `lifecycleScope.launchWhenStarted { }` | `lifecycleScope.launch { repeatOnLifecycle(STARTED) { } }` |
| `flow.collectAsState()` untuk data dari ViewModel | `flow.collectAsStateWithLifecycle()` |
| `GlobalScope.launch { }` | `viewModelScope` / `lifecycleScope` / scope aplikasi yang di-inject |
| `Dispatchers.IO` dengan `newFixedThreadPoolContext` | `Dispatchers.IO.limitedParallelism(n)` |
| `runBlocking` di kode produksi Android | `suspend fun` + scope yang sesuai |
| `LiveData` sebagai state utama pada layar Compose baru | `StateFlow` + `collectAsStateWithLifecycle()` |
