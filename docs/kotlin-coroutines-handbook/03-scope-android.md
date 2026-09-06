# 03 — Coroutine Scope pada Android

> Pertanyaan paling penting sebelum menulis `launch`: **siapa pemilik coroutine ini, dan kapan ia dibatalkan?**

## Tabel perbandingan

| Scope | Pemilik | Context bawaan | Dibatalkan saat | Untuk apa |
|---|---|---|---|---|
| `viewModelScope` | ViewModel | `SupervisorJob() + Dispatchers.Main.immediate` | `ViewModel.onCleared()` | Business/UI orchestration |
| `lifecycleScope` | `LifecycleOwner` (Activity/Fragment) | `SupervisorJob() + Dispatchers.Main.immediate` | `Lifecycle` mencapai `DESTROYED` | Operasi UI yang terikat layar |
| `repeatOnLifecycle` | Blok di dalam `lifecycleScope` | Mengikuti scope pemanggil | Turun di bawah state minimum (restart saat naik lagi) | Collect `Flow` di View |
| `rememberCoroutineScope()` | Composition | `Dispatchers.Main.immediate` + `Job` composition | Composable keluar dari composition | Coroutine yang dipicu event UI |
| Custom `CoroutineScope` | Kita sendiri | Ditentukan manual | Harus dibatalkan manual | Komponen dengan lifecycle sendiri |
| `GlobalScope` | Proses aplikasi | `EmptyCoroutineContext` | Tidak pernah | **Hindari** |

---

## `viewModelScope`

Scope default untuk hampir semua pekerjaan aplikasi.

```kotlin
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getProfile: GetProfileUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = getProfile()
            _uiState.update { it.reduce(result) }
        }
    }
}
```

Karakteristik:

- Context-nya `SupervisorJob() + Dispatchers.Main.immediate`, jadi satu `launch` yang gagal **tidak** membatalkan `launch` lain di ViewModel yang sama.
- Otomatis dibatalkan di `onCleared()` — tidak perlu `cancel()` manual.
- Bertahan melewati konfigurasi berubah (rotasi layar), berbeda dengan `lifecycleScope`.

**Gunakan untuk:** memanggil use case/repository, mengelola state layar, debounce input, retry.
**Jangan gunakan untuk:** pekerjaan yang harus selesai walaupun user meninggalkan layar (upload, sinkronisasi background) — pakai `WorkManager`.

---

## `lifecycleScope`

Scope milik Activity atau Fragment.

```kotlin
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            delay(200)
            binding.root.startEnterAnimation()
        }
    }
}
```

Karakteristik:

- Dibatalkan saat `onDestroy()`.
- **Untuk Fragment, gunakan `viewLifecycleOwner.lifecycleScope`** ketika menyentuh view. Lifecycle Fragment lebih panjang daripada lifecycle view-nya, sehingga `lifecycleScope` biasa bisa menyentuh view yang sudah dihancurkan.

```kotlin
// Salah pada Fragment yang menyentuh view
lifecycleScope.launch { binding.name.text = state.name }

// Benar
viewLifecycleOwner.lifecycleScope.launch { binding.name.text = state.name }
```

**Gunakan untuk:** animasi, delay UI, permintaan permission, navigasi bertunda, dan collect `Flow` (dibungkus `repeatOnLifecycle`).
**Jangan gunakan untuk:** business logic. Business logic ada di ViewModel supaya selamat dari rotasi layar.

---

## `repeatOnLifecycle`

Masalahnya: `lifecycleScope.launch { flow.collect { } }` tetap mengumpulkan data saat aplikasi di background. Ini memboroskan resource dan bisa menyebabkan crash saat update UI.

```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiState.collect { state -> render(state) }
        }
    }
}
```

Cara kerjanya:

```text
STARTED tercapai  → blok dijalankan (coroutine collect dimulai)
STOPPED           → blok dibatalkan (collect berhenti)
STARTED lagi      → blok dijalankan ulang dari awal
DESTROYED         → repeatOnLifecycle selesai permanen
```

Catatan:

- `repeatOnLifecycle` adalah `suspend fun`, jadi harus dipanggil dari dalam coroutine.
- Blok di dalamnya dijalankan **berulang kali**, jadi jangan meletakkan inisialisasi sekali-jalan di sana.
- Untuk mengumpulkan beberapa flow, gunakan `launch` terpisah di dalam satu blok `repeatOnLifecycle`.

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { viewModel.uiState.collect(::render) }
        launch { viewModel.events.collect(::handleEvent) }
    }
}
```

> Proyek ini sudah menyediakan helper `Flow<T>.collectOnLifecycle(owner)` di `core/FlowExt.kt` yang membungkus pola ini.

`launchWhenStarted` / `launchWhenResumed` sudah **deprecated** — jangan dipakai lagi karena hanya menunda coroutine, tidak membatalkannya.

---

## `rememberCoroutineScope`

Scope yang terikat pada posisi composable di composition.

```kotlin
@Composable
fun ProfileScreen(state: ProfileUiState) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            scope.launch { snackbarHostState.showSnackbar("Tersimpan") }
        },
    ) {
        Text("Simpan")
    }
}
```

Karakteristik:

- Dibatalkan ketika composable keluar dari composition.
- Memakai `Dispatchers.Main.immediate`.
- Dipakai untuk memulai coroutine dari **callback non-composable** (`onClick`, `onValueChange`).

**Gunakan untuk:** snackbar, `scrollState.animateScrollTo`, `sheetState.show()` — pekerjaan UI murni.
**Jangan gunakan untuk:** memanggil repository/use case. Panggil fungsi ViewModel, biarkan ViewModel yang membuat coroutine.

---

## Custom `CoroutineScope`

Kadang dibutuhkan scope yang hidup selama aplikasi berjalan, misalnya untuk operasi yang tidak boleh mati saat layar ditutup.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
```

```kotlin
class AnalyticsTracker @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun track(event: AnalyticsEvent) {
        scope.launch { remote.send(event) }
    }
}
```

Aturan wajib bila membuat scope sendiri:

- Selalu pakai `SupervisorJob()` supaya satu kegagalan tidak mematikan seluruh scope.
- Scope harus **di-inject**, bukan dibuat ad-hoc di dalam kelas — supaya bisa diganti saat testing.
- Kalau scope terikat objek yang bisa dihancurkan, panggil `scope.cancel()` di titik pembersihan.

```kotlin
class ConnectionManager : Closeable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun close() {
        scope.cancel()
    }
}
```

**Jangan** buat scope di dalam Repository hanya untuk memanggil API — Repository cukup mengekspos `suspend fun`.

---

## `GlobalScope`

```kotlin
// Hindari
GlobalScope.launch {
    repository.getUser()
}
```

Masalahnya:

- Tidak pernah dibatalkan → memory leak dan request yang terus jalan setelah layar ditutup.
- Keluar dari structured concurrency → error bisa hilang, cancellation tidak menurun.
- Tidak bisa dikontrol saat testing.

Alternatif berdasarkan kebutuhan:

| Kebutuhan | Solusi |
|---|---|
| Terkait layar/state layar | `viewModelScope` |
| Terkait lifecycle UI | `lifecycleScope` / `viewLifecycleOwner.lifecycleScope` |
| Harus lanjut walau layar tutup, tapi masih dalam sesi aplikasi | Application-scoped `CoroutineScope` yang di-inject |
| Harus selesai walau aplikasi mati | `WorkManager` |

---

## Memilih scope dalam 15 detik

```text
Apakah pekerjaannya menghasilkan/mengubah state layar?
    → viewModelScope

Apakah pekerjaannya murni UI (animasi, snackbar, scroll)?
    → lifecycleScope (View) / rememberCoroutineScope (Compose)

Apakah pekerjaannya mengumpulkan Flow untuk ditampilkan?
    → repeatOnLifecycle (View) / collectAsStateWithLifecycle (Compose)

Apakah pekerjaannya harus lanjut setelah layar ditutup?
    → application scope yang di-inject, atau WorkManager

Apakah ini di dalam Repository/UseCase?
    → jangan buat scope; ekspos suspend fun atau Flow
```

Lanjut ke [04 — Coroutine per Layer](04-coroutine-per-layer-primary.md).
