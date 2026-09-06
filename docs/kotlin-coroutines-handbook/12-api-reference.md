# 12 — API Reference Table

> Tabel referensi cepat. Kolom "Hindari jika" sama pentingnya dengan kolom "Kapan digunakan".

## Coroutine Builders

| API | Layer umum | Fungsi | Kapan digunakan | Hindari jika |
|---|---|---|---|---|
| `launch` | ViewModel / UI | Menjalankan coroutine tanpa return value, mengembalikan `Job` | Menjalankan side effect, memperbarui state | Butuh nilai kembalian dari coroutine |
| `async` | Domain / ViewModel | Menjalankan coroutine yang menghasilkan `Deferred<T>` | Beberapa operasi I/O independen | Hanya satu operasi, atau langsung di-`await` |
| `await` | Domain / ViewModel | Menunggu hasil `Deferred` | Mengambil hasil `async` | Dipanggil tepat setelah `async` untuk satu pekerjaan |
| `awaitAll` | Domain | Menunggu banyak `Deferred` sekaligus | Memuat N item paralel | Jumlah item tidak terbatas tanpa pembatas |
| `runBlocking` | `main()` / test lama | Menjembatani kode blocking ke coroutine | Entry point non-Android | Kode produksi Android |
| `produce` | Domain | Menghasilkan `ReceiveChannel` | Producer-consumer channel | Kebutuhan bisa dipenuhi `Flow` |

---

## Coroutine Scope

| API | Layer umum | Fungsi | Kapan digunakan | Hindari jika |
|---|---|---|---|---|
| `viewModelScope` | ViewModel | Scope lifecycle-aware milik ViewModel | Orkestrasi business/UI state | Dipakai di Repository/UseCase |
| `lifecycleScope` | Activity / Fragment | Scope milik `LifecycleOwner` | Operasi UI terikat layar | Business logic |
| `viewLifecycleOwner.lifecycleScope` | Fragment | Scope milik lifecycle view Fragment | Menyentuh view/binding | Pekerjaan yang harus selamat dari perpindahan back stack |
| `rememberCoroutineScope()` | Compose | Scope terikat composition | Coroutine dari callback UI | Memanggil repository/use case |
| `CoroutineScope(...)` | Application / komponen | Membuat scope kustom | Pekerjaan lintas layar dengan pemilik jelas | Dibuat di dalam Repository/UseCase |
| `MainScope()` | Komponen View kustom | Scope `SupervisorJob + Main` | Kelas View kustom yang punya `onDetach` | Ada `lifecycleScope` yang bisa dipakai |
| `GlobalScope` | — | Scope selama proses hidup | Praktis tidak pernah | Selalu hindari di aplikasi |

---

## Dispatcher

| API | Layer umum | Fungsi | Kapan digunakan | Hindari jika |
|---|---|---|---|---|
| `Dispatchers.Main` | UI | Menjalankan di UI thread | Update view dari background | Sudah berada di `viewModelScope`/`lifecycleScope` |
| `Dispatchers.Main.immediate` | UI | Main tanpa re-dispatch | Update state tanpa delay satu frame | — |
| `Dispatchers.IO` | Data | Pool untuk blocking I/O | File, prefs blocking, SDK blocking | Retrofit/Room `suspend`, DataStore |
| `Dispatchers.Default` | Data / Domain | Pool untuk CPU-bound | Parsing besar, sorting besar, kalkulasi berat | Operasi ringan (< 16 ms) |
| `Dispatchers.Unconfined` | — | Tidak terikat thread | Kasus khusus library/test | Kode produksi Android |
| `Dispatchers.IO.limitedParallelism(n)` | Data | Membatasi paralelisme | Antrian upload, resource terbatas | Tidak ada batasan resource nyata |

---

## Context Switching & Structured Concurrency

| API | Layer umum | Fungsi | Kapan digunakan | Hindari jika |
|---|---|---|---|---|
| `withContext` | Data / Domain | Ganti context untuk satu blok, sequential | Memindahkan operasi blocking/CPU | Library sudah main-safe; ingin paralel |
| `coroutineScope` | Domain / Data | Scope anak; gagal satu → batal semua | Paralel di dalam `suspend fun` | Sebagian data boleh gagal |
| `supervisorScope` | Domain | Scope anak; kegagalan tidak menular | Paralel dengan kegagalan parsial | Semua data wajib berhasil |
| `CoroutineName` | Semua | Memberi nama untuk debug | Debug produksi/log | Tidak ada kebutuhan debug |
| `currentCoroutineContext()` | Semua | Membaca context aktif | Cek dispatcher/job saat debugging | Logika bisnis bergantung padanya |

---

## Job & Cancellation

| API | Layer umum | Fungsi | Kapan digunakan | Hindari jika |
|---|---|---|---|---|
| `Job` | ViewModel | Handle lifecycle coroutine | Membatalkan/mengganti request | Butuh hasil (pakai `Deferred`) |
| `SupervisorJob` | Scope kustom | Isolasi kegagalan antar child | Membuat scope aplikasi | Dipakai untuk menyembunyikan error |
| `job.cancel()` | ViewModel / UI | Membatalkan coroutine + child | Search/debounce, batal upload | Scope milik framework |
| `job.cancelAndJoin()` | ViewModel | Batalkan lalu tunggu selesai | Restart pekerjaan | Tidak perlu menunggu |
| `job.join()` | Domain | Menunggu job selesai | Sinkronisasi urutan | Bisa memakai `await`/`coroutineScope` |
| `isActive` | Semua | Cek status aktif | Kondisi loop berat | Kode sudah penuh titik suspend |
| `ensureActive()` | Domain / Data | Melempar bila sudah dibatalkan | Loop CPU-bound | — |
| `yield()` | Domain | Beri giliran + cek cancellation | Loop panjang | Ada `delay`/suspend lain |
| `NonCancellable` | Data | Menjalankan blok walau sudah dibatalkan | Pembersihan resource di `finally` | Pekerjaan panjang |
| `withTimeout` | Domain / Data | Batas waktu, melempar exception | SLA request yang wajib | Timeout sudah diatur OkHttp |
| `withTimeoutOrNull` | Domain / Data | Batas waktu, mengembalikan `null` | Timeout yang boleh diabaikan | Perlu tahu penyebab gagal |
| `CancellationException` | Semua | Sinyal pembatalan | Diteruskan ulang di `catch` | Ditangkap dan ditelan |

---

## Error Handling

| API | Layer umum | Fungsi | Kapan digunakan | Hindari jika |
|---|---|---|---|---|
| `try/catch` | Semua | Menangkap exception | Penanganan error utama | Tanpa `catch` untuk `CancellationException` |
| `runCatching` | Data | Membungkus hasil jadi `Result` | Operasi kecil non-cancellable | Coroutine bisa dibatalkan (tanpa guard) |
| `CoroutineExceptionHandler` | ViewModel / scope kustom | Jaring pengaman error tak tertangani | Logging & fallback global | Dipakai sebagai penanganan error utama; pada `async` |
| `Flow.catch` | ViewModel / Data | Menangkap error upstream | Fallback state pada stream | Perlu menangkap error collector |
| `Flow.retryWhen` | Data | Mengulang stream yang gagal | Error jaringan sementara | Error permanen (401, 404) |

---

## Android Coroutine API

| API | Layer umum | Fungsi | Kapan digunakan | Hindari jika |
|---|---|---|---|---|
| `viewModelScope` | ViewModel | Scope otomatis dibatalkan di `onCleared` | Semua pekerjaan bisnis layar | Layer data |
| `lifecycleScope` | Activity / Fragment | Scope otomatis dibatalkan di `onDestroy` | UI lifecycle operation | Business logic |
| `repeatOnLifecycle(state)` | Activity / Fragment | Menjalankan ulang blok mengikuti lifecycle | Collect `Flow` di View | Inisialisasi yang hanya boleh sekali |
| `flowWithLifecycle(lifecycle, state)` | Activity / Fragment | Operator versi Flow dari `repeatOnLifecycle` | Satu flow saja | Banyak flow (pakai `repeatOnLifecycle` + `launch`) |
| `LifecycleOwner.lifecycle.currentStateFlow` | UI | State lifecycle sebagai `StateFlow` | Logika bergantung state lifecycle | Kebutuhan sudah tertutup `repeatOnLifecycle` |
| `stateIn` | ViewModel | Cold `Flow` → `StateFlow` | State layar dari database/DataStore | Nilai one-shot |
| `shareIn` | ViewModel / Data | Cold `Flow` → `SharedFlow` | Berbagi stream ke banyak collector | Hanya satu collector |
| `WorkManager` | Data | Pekerjaan terjadwal & tahan proses mati | Upload/sync yang wajib selesai | Operasi cepat terkait layar |
| `DataStore` | Data | Penyimpanan berbasis `Flow` | Preferensi | Data relasional (pakai Room) |

---

## Compose Coroutine API

| API | Fungsi | Kapan digunakan | Hindari jika |
|---|---|---|---|
| `LaunchedEffect(key)` | Coroutine sebagai efek composition | Load awal, reaksi terhadap perubahan key | Dipicu event klik |
| `rememberCoroutineScope()` | Scope untuk callback UI | Snackbar, scroll, sheet | Memanggil repository/use case |
| `collectAsStateWithLifecycle()` | Collect `Flow` lifecycle-aware | State dari ViewModel | Flow lokal composable murni |
| `collectAsState()` | Collect `Flow` tanpa lifecycle | Flow yang dibuat di dalam Compose | State dari ViewModel |
| `produceState` | Mengubah suspend jadi `State` | Konversi sumber async sederhana | State sudah ada di ViewModel |
| `snapshotFlow` | Compose state → `Flow` | Bereaksi pada scroll/posisi list | Nilai sudah tersedia sebagai `Flow` |
| `DisposableEffect` | Efek dengan pembersihan (non-coroutine) | Listener, callback registrasi | Butuh coroutine (`LaunchedEffect`) |
| `rememberUpdatedState` | Menjaga lambda terbaru dalam efek panjang | `LaunchedEffect` berumur panjang | Efek pendek dengan key tepat |
| `awaitPointerEventScope` | Menangani gesture suspend | Custom gesture | Gesture standar sudah cukup |

---

## Flow (ringkas)

| API | Layer umum | Fungsi | Kapan digunakan | Hindari jika |
|---|---|---|---|---|
| `Flow` | Data / Domain | Stream dingin | Data berubah dari waktu ke waktu | Hasil sekali panggil |
| `StateFlow` | ViewModel | State panas dengan nilai awal | UI state | Event sekali-jalan |
| `SharedFlow` | ViewModel | Stream panas tanpa nilai awal | Event navigasi/snackbar | State layar |
| `MutableStateFlow` | ViewModel (private) | State yang bisa diubah | Sumber state internal | Diekspos langsung ke UI |
| `flowOn` | Data | Mengubah dispatcher upstream | Transformasi berat pada stream | Sumber sudah main-safe |
| `combine` | ViewModel / Domain | Menggabungkan beberapa flow | State dari banyak sumber | Sumber tunggal |
| `distinctUntilChanged` | ViewModel | Menyaring nilai identik | Mengurangi recomposition | `StateFlow` (sudah conflated) |
| `first()` | Domain | Mengambil satu nilai lalu berhenti | Membaca sekali dari DataStore | Perlu observasi terus-menerus |

Lanjut ke [13 — Diagram & Ownership](13-diagram-ownership.md).
