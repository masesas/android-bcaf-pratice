# 15 — Best Practices

> Rangkuman rekomendasi final. Setiap poin punya alasannya, karena aturan tanpa alasan akan dilanggar saat kondisi berubah.

## Coroutine Ownership

- **Setiap coroutine harus punya pemilik yang jelas.** Sebelum menulis `launch`, jawab dulu: siapa yang membatalkannya?
- **Layer bawah tidak membuat coroutine.** Repository, data source, dan use case cukup mengekspos `suspend fun` atau `Flow`. Alasannya: mereka tidak punya lifecycle, sehingga tidak tahu kapan pekerjaan harus berhenti.
- **Layer atas yang memulai coroutine.** ViewModel untuk business logic, UI untuk efek tampilan.
- **Scope kustom selalu di-inject**, bukan dibuat di dalam kelas yang memakainya, supaya bisa diganti saat testing dan dibatalkan dari luar.
- **Jangan pakai `GlobalScope`.** Tidak ada kasus di aplikasi Android yang tidak bisa diselesaikan dengan `viewModelScope`, application scope, atau `WorkManager`.

## Lifecycle

- **ViewModel untuk pekerjaan yang harus selamat dari rotasi**, `lifecycleScope` hanya untuk urusan tampilan.
- **Fragment memakai `viewLifecycleOwner`** setiap kali menyentuh view atau binding.
- **Collect `Flow` di View selalu dengan `repeatOnLifecycle(STARTED)`**, bukan `launchWhenStarted` (deprecated) dan bukan `collect` polos.
- **Di Compose, pakai `collectAsStateWithLifecycle()`** untuk state dari ViewModel, supaya collection berhenti saat layar di background.
- **`LaunchedEffect` untuk efek composition, `rememberCoroutineScope` untuk callback.** Jangan tertukar.

## Cancellation

- **`CancellationException` selalu diteruskan.** Menelannya merusak structured concurrency dan menghasilkan error palsu.
- **Urutan `catch`**: `CancellationException` dulu, `Throwable` belakangan.
- **Loop CPU-bound memeriksa `isActive`/`ensureActive()`**, karena cancellation bersifat cooperative.
- **Pembersihan resource di `finally`**, dengan `withContext(NonCancellable)` bila pembersihannya `suspend`.
- **Simpan `Job` untuk pekerjaan yang bisa diganti** (search, filter, autocomplete) dan batalkan yang lama.
- **Jangan `cancel()` scope milik framework** (`viewModelScope`, `lifecycleScope`).

## Dispatcher

- **Setiap `suspend fun` publik wajib main-safe.** Tanggung jawab ada pada fungsi itu, bukan pemanggilnya.
- **Jangan bungkus library yang sudah main-safe.** Retrofit `suspend`, Room `suspend`/`Flow`, dan DataStore tidak butuh `withContext(Dispatchers.IO)`.
- **`Dispatchers.IO` untuk blocking sungguhan**, `Dispatchers.Default` untuk CPU-bound.
- **Dispatcher ditentukan di layer data**, sedekat mungkin dengan operasinya, supaya perubahan sumber data tidak merembet ke ViewModel.
- **Injeksikan dispatcher hanya bila memang ada `withContext`**, untuk kebutuhan testing.
- **Batasi paralelisme dengan `limitedParallelism`**, bukan dengan membuat thread pool baru.

## Structured Concurrency

- **Pakai `coroutineScope` untuk paralel di dalam `suspend fun`**, bukan membuat `CoroutineScope` baru.
- **`async` hanya untuk pekerjaan yang benar-benar bersamaan.** `async { }.await()` berurutan adalah kode mati.
- **Semua data wajib berhasil → `coroutineScope`; sebagian boleh gagal → `supervisorScope`.**
- **Batasi jumlah coroutine paralel** dengan `Semaphore` bila jumlahnya bergantung input.
- **Setiap `async` harus di-`await`** di suatu tempat, agar error tidak hilang.

## Error Handling

- **Terjemahkan error di layer data menjadi tipe hasil** (`AppResult`/`AppFailure`), sehingga jalur error terlihat di signature.
- **`try/catch` adalah alat utama**, bukan `CoroutineExceptionHandler`.
- **`CoroutineExceptionHandler` hanya jaring pengaman** pada root coroutine, dan tidak berlaku untuk `async`.
- **Jangan pakai `runCatching` polos di kode yang bisa dibatalkan** — periksa `CancellationException`.
- **Pastikan state kembali konsisten di jalur error** (`isLoading = false`, pesan error terisi).
- **Pesan ke user tidak membocorkan detail teknis**; simpan detail di log.

## ViewModel

- **Satu `data class` immutable untuk state layar**, diperbarui dengan `update { }`.
- **Ekspos `StateFlow`, simpan `MutableStateFlow` sebagai `private`.**
- **Guard operasi ganda** dengan pemeriksaan `isLoading` atau pembatalan `Job` lama.
- **Tidak ada referensi `Context` Activity, `View`, atau `Fragment` di ViewModel.**
- **Tidak ada `withContext` untuk membungkus repository** — itu tanda main-safety bocor ke layer atas.
- **Gunakan `stateIn(WhileSubscribed(5_000))`** untuk mengubah `Flow` menjadi state layar.
- **Event sekali-jalan lewat `SharedFlow(replay = 0)`** atau state yang dikonsumsi eksplisit, jangan disimpan sebagai state permanen.

## Repository & Data Layer

- **Kontrak repository: `suspend fun` untuk one-shot, `Flow` untuk observasi.**
- **Tidak ada `launch` dan tidak ada `CoroutineScope`** di dalam repository.
- **Database sebagai single source of truth**: UI mengamati Room, jaringan hanya memperbarui Room.
- **Mapping DTO → domain dilakukan di layer data**, sehingga domain bebas dari detail transport.
- **Tulisan yang harus atomik memakai transaksi**, bukan beberapa coroutine paralel.
- **Pertahankan pelemparan ulang `CancellationException`** pada helper seperti `runApiCatching`.

## Jetpack Compose

- **Composable tidak memanggil repository atau use case.** Panggil fungsi ViewModel.
- **Jangan memulai coroutine di body composable** tanpa `LaunchedEffect`.
- **Pilih key `LaunchedEffect` dengan hati-hati**; key yang sering berubah membuat efek restart terus-menerus.
- **Pisahkan `Route` (stateful, terhubung ViewModel) dan `Screen` (stateless, bisa di-preview).**
- **`rememberCoroutineScope` untuk UI murni saja**: snackbar, scroll, bottom sheet.
- **Pakai `collectAsStateWithLifecycle()`**, bukan `collectAsState()`, untuk state dari ViewModel.

## Testing

- **`runTest` menggantikan `runBlocking`** agar `delay` memakai virtual time.
- **`MainDispatcherRule` wajib** untuk test yang menyentuh `viewModelScope`.
- **Uji tiga jalur**: loading, sukses, gagal.
- **Gunakan fake berbasis interface** daripada mock yang berat.
- **Uji paralelisme dengan `testScheduler.currentTime`** untuk memastikan `async` benar-benar paralel.

---

## Checklist final sebelum merge

```text
Ownership
  [ ] Tidak ada GlobalScope
  [ ] Tidak ada CoroutineScope( di layer data/domain
  [ ] Scope kustom di-inject dan punya jalur cancel

Lifecycle
  [ ] Fragment memakai viewLifecycleOwner untuk view
  [ ] Collect Flow di View memakai repeatOnLifecycle
  [ ] Compose memakai collectAsStateWithLifecycle

Dispatcher
  [ ] Tidak ada withContext(IO) membungkus Retrofit/Room
  [ ] Operasi blocking memakai Dispatchers.IO
  [ ] Kalkulasi berat memakai Dispatchers.Default

Concurrency
  [ ] async hanya untuk pekerjaan paralel nyata
  [ ] Paralel memakai coroutineScope/supervisorScope
  [ ] Setiap async di-await

Error & Cancellation
  [ ] CancellationException selalu di-throw ulang
  [ ] State kembali konsisten di jalur error
  [ ] Loop berat memeriksa isActive

ViewModel & State
  [ ] MutableStateFlow private, StateFlow publik
  [ ] State diperbarui dengan update { }
  [ ] Event sekali-jalan tidak disimpan sebagai state permanen

Testing
  [ ] runTest + MainDispatcherRule
  [ ] Jalur sukses, gagal, dan loading tercakup
```

Kembali ke [daftar isi](README.md).
