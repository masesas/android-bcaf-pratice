# 05 — Flow + Coroutines

> Overview singkat hubungan coroutine dengan `Flow`. Detail operator Flow adalah topik dokumentasi terpisah; di sini fokus pada pola yang dipakai antar layer.

## Hubungan dengan coroutine

`Flow` adalah **aliran nilai asinkron** yang dibangun di atas coroutine:

- `Flow.collect` adalah `suspend fun` → harus dijalankan di dalam coroutine.
- Cancellation coroutine otomatis menghentikan collection.
- Operator `map`, `filter`, `combine` berjalan di coroutine yang sama dengan collector (kecuali diubah dengan `flowOn`).

Aturan singkat: **`suspend fun` untuk satu hasil, `Flow` untuk banyak hasil dari waktu ke waktu.**

---

## Pola standar antar layer

```text
Repository
   ↓ Flow (cold, sumber data)
ViewModel
   ↓ StateFlow (hot, state layar)
Compose
   ↓ collectAsStateWithLifecycle()
UI
```

```kotlin
// Repository
override fun observeProfile(id: String): Flow<Profile?> =
    dao.observeProfile(id).map { it?.toDomain() }
```

```kotlin
// ViewModel
val uiState: StateFlow<ProfileUiState> = observeProfile(userId)
    .map { profile -> ProfileUiState(user = profile, isLoading = false) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = ProfileUiState(isLoading = true),
    )
```

```kotlin
// Compose
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

```kotlin
// View (Fragment/Activity)
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect(::render)
    }
}
```

`STOP_TIMEOUT_MS` yang lazim dipakai adalah `5_000`, supaya upstream tidak langsung berhenti saat rotasi layar atau perpindahan aplikasi sebentar.

---

## Cold stream vs hot stream

| | Cold | Hot |
|---|---|---|
| Contoh | `Flow` biasa, Room `Flow`, `flow { }` | `StateFlow`, `SharedFlow` |
| Kapan mulai berjalan | Saat ada collector | Sudah berjalan/menyimpan nilai, terlepas dari collector |
| Setiap collector | Mendapat eksekusi sendiri | Berbagi aliran yang sama |
| Nilai saat tidak ada collector | Tidak diproduksi | `StateFlow` tetap menyimpan nilai terakhir |

```kotlin
// Cold: setiap collect memicu query baru
fun observeUsers(): Flow<List<User>> = dao.observeUsers().map { it.toDomain() }

// Hot: satu sumber, banyak collector
val uiState: StateFlow<UiState> = _uiState.asStateFlow()
```

Konversi cold → hot dilakukan dengan `stateIn` atau `shareIn`, dan **selalu** butuh `CoroutineScope` (di ViewModel: `viewModelScope`).

---

## `StateFlow` untuk state

`StateFlow` selalu punya nilai, meng-emit nilai terakhir ke collector baru, dan melakukan **conflation** (nilai identik tidak di-emit ulang).

```kotlin
private val _uiState = MutableStateFlow(ProfileUiState())
val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

fun setName(name: String) {
    _uiState.update { it.copy(name = name) }
}
```

- Gunakan `update { }` (atomik), bukan `value = value.copy(...)` yang rawan race.
- `data class` untuk UI state penting karena `equals` menentukan apakah collector di-notify.
- Cocok untuk: loading, data, error, hasil form, nilai filter.

---

## `SharedFlow` untuk event

Event sekali-jalan (navigasi, snackbar, toast) **tidak boleh** disimpan sebagai state, karena akan terulang saat rotasi layar.

```kotlin
private val _events = MutableSharedFlow<ProfileEvent>(
    replay = 0,
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
)
val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()

private fun notifySessionExpired() {
    _events.tryEmit(ProfileEvent.SessionExpired)
}
```

Konfigurasi ini sudah dipakai `AuthViewModel` pada proyek ini: `replay = 0` supaya event tidak diputar ulang, dan `extraBufferCapacity` supaya `tryEmit` tidak gagal saat belum ada collector aktif.

---

## State vs event

| | State | Event |
|---|---|---|
| Tipe | `StateFlow<UiState>` | `SharedFlow<UiEvent>` / `Channel` |
| Sifat | Bertahan, bisa dibaca ulang | Sekali konsumsi |
| Contoh | `isLoading`, `user`, `errorMessage` di layar | Navigasi, snackbar, tutup layar |
| Saat rotasi | Ditampilkan kembali (benar) | Tidak boleh terulang |
| Collect di UI | `collectAsStateWithLifecycle()` | `LaunchedEffect` + `repeatOnLifecycle` |

> Alternatif modern lain: simpan pesan error sebagai bagian dari state, lalu UI memanggil `viewModel.consumeError()` setelah ditampilkan. Pola ini lebih mudah dites daripada `SharedFlow` dan konsisten dengan `AppFailure` di proyek ini.

---

## Menggabungkan beberapa Flow

```kotlin
val uiState: StateFlow<DashboardUiState> = combine(
    observeProfile(),
    observeTransactions(),
    observeNotifications(),
) { profile, transactions, notifications ->
    DashboardUiState(profile, transactions, notifications)
}.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = DashboardUiState(isLoading = true),
)
```

`combine` menunggu setiap sumber meng-emit minimal satu nilai sebelum menghasilkan output pertama.

---

## Kesalahan umum pada Flow

```kotlin
// Salah: collect di init membuat state ganda dan sulit dikontrol
init {
    viewModelScope.launch {
        repository.observeProfile().collect { profile ->
            _uiState.update { it.copy(user = profile) }
        }
    }
}

// Lebih baik: deklaratif dengan stateIn
val uiState: StateFlow<ProfileUiState> = repository.observeProfile()
    .map { ProfileUiState(user = it) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())
```

```kotlin
// Salah: collect tanpa lifecycle di View
lifecycleScope.launch { viewModel.uiState.collect(::render) }

// Benar
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect(::render)
    }
}
```

```kotlin
// Salah: flowOn di hilir tidak berpengaruh pada upstream setelahnya
flow.map { heavy(it) }.flowOn(Dispatchers.Default).map { alsoHeavy(it) }

// Benar: flowOn mempengaruhi operator DI ATASNYA
flow.map { heavy(it) }.map { alsoHeavy(it) }.flowOn(Dispatchers.Default)
```

---

## Ringkasan

| Butuh | Pakai |
|---|---|
| Satu hasil sekali panggil | `suspend fun` |
| Aliran data yang berubah dari database/DataStore | `Flow` (cold) |
| State layar yang selalu punya nilai | `StateFlow` |
| Event sekali-jalan | `SharedFlow` (`replay = 0`) atau state + consume |
| Mengubah cold menjadi state layar | `stateIn(viewModelScope, WhileSubscribed(5_000), initial)` |
| Membaca state di Compose | `collectAsStateWithLifecycle()` |
| Membaca state di View | `repeatOnLifecycle(STARTED) { collect { } }` |

Lanjut ke [06 — Sequential vs Concurrent](06-sequential-concurrent.md).
