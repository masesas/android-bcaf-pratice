# 04 — Penggunaan Coroutine Berdasarkan Layer (PRIMARY)

> Bab paling sering dibuka saat coding. Aturannya sederhana: **hanya layer atas yang membuat coroutine; layer bawah hanya menyediakan `suspend fun` dan `Flow`.**

## Alur dan tanggung jawab

```text
UI (Activity / Fragment / Compose)
 ↓  memicu event, meng-collect state secara lifecycle-aware
ViewModel
 ↓  PEMILIK coroutine (viewModelScope), mengatur state & orkestrasi
UseCase
 ↓  suspend fun murni; boleh coroutineScope/async untuk paralel
Repository
 ↓  suspend fun / Flow; memilih sumber data; main-safety
Remote / Local Data Source
     Retrofit suspend, Room suspend/Flow, DataStore Flow
```

| Layer | Membuat coroutine? | Menentukan dispatcher? | Menangkap exception? | Mengekspos |
|---|---|---|---|---|
| Data Source | Tidak | Hanya bila blocking | Tidak | `suspend fun`, `Flow` |
| Repository | Tidak | Ya, bila memang perlu | Ya (jadi hasil bertipe) | `suspend fun`, `Flow` |
| UseCase | Tidak (boleh `coroutineScope`) | Tidak | Opsional | `suspend fun`, `Flow` |
| ViewModel | **Ya** | Tidak | Ya (jadi UI state) | `StateFlow`, `SharedFlow` |
| UI | Ya, untuk UI saja | Tidak | Tidak | — |

---

## Data Layer / Repository

### Bentuk kontrak

```kotlin
interface UserRepository {
    suspend fun getUser(): User
    fun observeUser(): Flow<User?>
}
```

### Apakah Repository perlu membuat `CoroutineScope`?

**Tidak.** Repository tidak punya lifecycle sendiri. Jika Repository membuat scope, coroutine-nya tidak akan ikut dibatalkan saat layar ditutup, dan hasilnya tidak bisa ditunggu pemanggil.

### Apakah Repository perlu `launch`?

**Tidak**, dengan satu pengecualian: cache/sinkronisasi background yang memang harus hidup selama aplikasi berjalan — dan itu pun memakai `CoroutineScope` yang **di-inject** dari luar, bukan dibuat sendiri.

```kotlin
// Hindari
class UserRepositoryImpl(private val api: UserApi) {

    private val scope = CoroutineScope(Dispatchers.IO)

    fun getUser() {
        scope.launch { api.getUser() }   // hasil tidak bisa diambil, tidak bisa dibatalkan
    }
}

// Rekomendasi
class UserRepositoryImpl(private val api: UserApi) : UserRepository {

    override suspend fun getUser(): User = api.getUser().toDomain()
}
```

### Kapan Repository memakai `withContext`?

Hanya ketika di dalamnya ada **operasi blocking sungguhan**.

```kotlin
class ProfileRepositoryImpl @Inject constructor(
    private val api: ProfileApi,
    private val dao: ProfileDao,
    private val cacheFile: File,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ProfileRepository {

    override suspend fun getProfile(): Profile = api.getProfile().toDomain()     // Retrofit: tanpa withContext

    override suspend fun cached(): Profile? = dao.findProfile()?.toDomain()      // Room: tanpa withContext

    override suspend fun readLegacyCache(): String? = withContext(io) {          // File: perlu withContext
        cacheFile.takeIf { it.exists() }?.readText()
    }
}
```

### Retrofit

Retrofit `suspend fun` sudah menjalankan request di luar main thread.

```kotlin
interface ProfileApi {

    @GET("v1/profile")
    suspend fun getProfile(): ApiEnvelope<ProfileDto>
}
```

```kotlin
override suspend fun getProfile(): AppResult<Profile> = runApiCatching(json) {
    api.getProfile().requirePayload().map(ProfileDto::toDomain)
}
```

`runApiCatching` di `core/network/ApiCall.kt` menerjemahkan `HttpException`/`IOException` menjadi `AppFailure` dan **melempar ulang `CancellationException`**. Itu perilaku yang wajib dipertahankan.

### Room

```kotlin
@Dao
interface ProfileDao {

    @Query("SELECT * FROM profile WHERE id = :id")
    suspend fun findProfile(id: String): ProfileEntity?

    @Query("SELECT * FROM profile WHERE id = :id")
    fun observeProfile(id: String): Flow<ProfileEntity?>

    @Upsert
    suspend fun upsert(entity: ProfileEntity)
}
```

- DAO `suspend` dan DAO yang mengembalikan `Flow` sudah main-safe.
- DAO yang mengembalikan tipe biasa (bukan `suspend`, bukan `Flow`) bersifat blocking dan **harus** dibungkus `withContext(io)`.
- Untuk beberapa tulisan yang harus atomik, pakai `@Transaction` atau `withTransaction`, bukan `launch` terpisah.

### Pola single source of truth

```kotlin
override fun observeProfile(id: String): Flow<Profile?> =
    dao.observeProfile(id).map { entity -> entity?.toDomain() }

override suspend fun refreshProfile(id: String): AppResult<Unit> = runApiCatching(json) {
    api.getProfile(id).requirePayload()
        .map { dto -> dao.upsert(dto.toEntity()) }
}
```

UI mengamati database, jaringan hanya memperbarui database. Tidak ada coroutine yang dibuat di Repository.

### Paralel di dalam Repository

Boleh, selama memakai `coroutineScope` (bukan scope buatan sendiri).

```kotlin
override suspend fun getProfileBundle(id: String): ProfileBundle = coroutineScope {
    val remote = async { api.getProfile(id).requirePayload() }
    val local = async { dao.findPreferences(id) }

    ProfileBundle(remote.await(), local.await())
}
```

---

## Domain / UseCase

### Bentuk paling umum

```kotlin
class GetUserUseCase @Inject constructor(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(): User = repository.getUser()
}
```

Kapan `suspend` polos sudah cukup:

- Meneruskan satu panggilan repository.
- Menambahkan validasi, mapping, atau aturan bisnis yang ringan.
- Menggabungkan dua panggilan yang memang **harus berurutan** (hasil pertama dipakai yang kedua).

```kotlin
class GetTransactionDetailUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(id: String): TransactionDetail {
        val transaction = repository.getTransaction(id)          // dibutuhkan lebih dulu
        val merchant = repository.getMerchant(transaction.merchantId)
        return TransactionDetail(transaction, merchant)
    }
}
```

### Kapan UseCase butuh concurrency

Ketika ada beberapa panggilan **independen** dan latensinya terasa.

```kotlin
class GetDashboardUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(): Dashboard = coroutineScope {
        val user = async { userRepository.getUser() }
        val transactions = async { transactionRepository.getRecent() }

        Dashboard(user.await(), transactions.await())
    }
}
```

Aturan untuk UseCase:

- **Tidak** boleh punya `CoroutineScope` sendiri.
- **Tidak** menentukan dispatcher — itu tugas layer data.
- Boleh memakai `coroutineScope` / `supervisorScope` untuk mengelompokkan pekerjaan paralel.
- UseCase yang mengembalikan `Flow` cukup mengembalikan `Flow` dari repository, biasanya dengan `map`/`combine`.

```kotlin
class ObserveDashboardUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(): Flow<Dashboard> = combine(
        userRepository.observeUser(),
        transactionRepository.observeRecent(),
    ) { user, transactions -> Dashboard(user, transactions) }
}
```

> Perhatikan: UseCase yang mengembalikan `Flow` **tidak** perlu `suspend`. `suspend` hanya untuk one-shot.

---

## ViewModel

ViewModel adalah **pemilik coroutine** untuk business logic.

### Struktur state

```kotlin
data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
)
```

### Loading, success, error

```kotlin
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUser: GetUserUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = getUser()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isLoading = false, user = result.data)
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.failure.toMessage())
                }
            }
        }
    }
}
```

Kalau use case melempar exception (bukan mengembalikan `AppResult`), bungkus dengan `try/catch` dan **teruskan `CancellationException`**:

```kotlin
viewModelScope.launch {
    _uiState.update { it.copy(isLoading = true) }
    try {
        val user = getUser()
        _uiState.update { it.copy(isLoading = false, user = user) }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        _uiState.update { it.copy(isLoading = false, error = throwable.message) }
    }
}
```

### Mencegah request ganda

```kotlin
fun submit() {
    if (_uiState.value.isLoading) return
    viewModelScope.launch { /* ... */ }
}
```

Atau simpan `Job` bila permintaan baru harus menggantikan yang lama:

```kotlin
private var searchJob: Job? = null

fun search(query: String) {
    searchJob?.cancel()
    searchJob = viewModelScope.launch {
        delay(SEARCH_DEBOUNCE_MS)
        val results = searchUseCase(query)
        _uiState.update { it.copy(results = results) }
    }
}
```

### Sequential request

Pakai ketika hasil pertama dibutuhkan oleh yang kedua.

```kotlin
viewModelScope.launch {
    val session = login(credentials)
    val profile = getProfile(session.userId)
    _uiState.update { it.copy(profile = profile) }
}
```

### Parallel request

```kotlin
viewModelScope.launch {
    _uiState.update { it.copy(isLoading = true) }

    val dashboard = coroutineScope {
        val user = async { getUser() }
        val transactions = async { getTransactions() }
        Dashboard(user.await(), transactions.await())
    }

    _uiState.update { it.copy(isLoading = false, dashboard = dashboard) }
}
```

Detail (termasuk versi `supervisorScope`) di [06 — Sequential vs Concurrent](06-sequential-concurrent.md).

### Cancellation di ViewModel

- `viewModelScope` dibatalkan otomatis di `onCleared()`.
- Jangan panggil `viewModelScope.cancel()` manual — scope tidak akan bisa dipakai lagi.
- Bila perlu membersihkan resource non-coroutine, override `onCleared()`.

```kotlin
override fun onCleared() {
    super.onCleared()
    connection.close()
}
```

### Yang tidak boleh ada di ViewModel

- Referensi ke `Context` Activity, `View`, atau `Fragment`.
- `withContext(Dispatchers.IO)` untuk membungkus repository.
- `runBlocking`.
- `GlobalScope`.

---

## Activity

Activity boleh membuat coroutine untuk urusan UI dan untuk meng-collect state.

```kotlin
@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }

        binding.retryButton.setOnClickListener { viewModel.load() }
    }

    private fun render(state: ProfileUiState) {
        binding.progress.isVisible = state.isLoading
        binding.name.text = state.user?.name.orEmpty()
        binding.error.isVisible = state.error != null
    }
}
```

---

## Fragment

Perbedaan pentingnya: gunakan `viewLifecycleOwner`, bukan Fragment itu sendiri.

```kotlin
@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }
}
```

Atau memakai helper proyek ini:

```kotlin
viewModel.uiState.collectOnLifecycle(viewLifecycleOwner) { state -> render(state) }
```

### Kapan Activity/Fragment boleh membuat coroutine

| Boleh | Sebaiknya di ViewModel |
|---|---|
| Collect `StateFlow`/`SharedFlow` dengan `repeatOnLifecycle` | Memanggil API atau database |
| Animasi, transisi, `delay` UI | Menyimpan hasil dan state layar |
| Menunggu hasil permission/activity result | Validasi form dan aturan bisnis |
| Operasi pada `View` yang butuh suspend | Retry, debounce, orkestrasi request |

Alasannya: coroutine di `lifecycleScope` mati saat rotasi layar, sedangkan pekerjaan bisnis harus selamat.

---

## Jetpack Compose

### Membaca state secara lifecycle-aware

```kotlin
@Composable
fun ProfileRoute(viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileScreen(
        state = state,
        onRetry = viewModel::load,
    )
}
```

`collectAsStateWithLifecycle()` (dari `androidx.lifecycle:lifecycle-runtime-compose`) menghentikan collection saat layar di background. Untuk state yang berasal dari ViewModel, ini selalu lebih tepat daripada `collectAsState()`.

### `LaunchedEffect`

Menjalankan coroutine **sebagai efek dari composition**, otomatis dibatalkan saat composable keluar atau saat key berubah.

```kotlin
@Composable
fun ProfileScreen(userId: String, viewModel: ProfileViewModel = hiltViewModel()) {
    LaunchedEffect(userId) {
        viewModel.load(userId)          // dijalankan ulang hanya bila userId berubah
    }
}
```

Aturan key:

| Key | Perilaku |
|---|---|
| `LaunchedEffect(Unit)` | Sekali saja selama composable ada di composition |
| `LaunchedEffect(userId)` | Batalkan dan jalankan ulang setiap `userId` berubah |
| `LaunchedEffect(state)` | Hati-hati: `state` yang sering berubah akan me-restart efek terus-menerus |

### `rememberCoroutineScope`

Menjalankan coroutine dari **callback**, bukan dari composition.

```kotlin
@Composable
fun ProfileScreen(state: ProfileUiState, onSave: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Button(
            modifier = Modifier.padding(padding),
            onClick = {
                onSave()
                scope.launch { snackbarHostState.showSnackbar("Tersimpan") }
            },
        ) {
            Text("Simpan")
        }
    }
}
```

### `LaunchedEffect` vs `rememberCoroutineScope`

| | `LaunchedEffect` | `rememberCoroutineScope` |
|---|---|---|
| Dipicu oleh | Composition / perubahan key | Event user (`onClick`, dll.) |
| Boleh dipanggil dari | Body composable | Callback non-composable |
| Otomatis restart saat key berubah | Ya | Tidak |
| Dibatalkan saat keluar composition | Ya | Ya |
| Contoh pemakaian | Load awal, observasi, auto-scroll saat data berubah | Snackbar, `animateScrollTo`, `sheetState.show()` |

```kotlin
// Salah: memulai coroutine langsung di body composable
@Composable
fun Screen(viewModel: ProfileViewModel) {
    viewModel.viewModelScope.launch { }        // jangan pernah
    rememberCoroutineScope().launch { }        // jalan setiap recomposition
}

// Benar
@Composable
fun Screen(viewModel: ProfileViewModel) {
    LaunchedEffect(Unit) { viewModel.load() }
}
```

### Event sekali-jalan di Compose

```kotlin
@Composable
fun ProfileRoute(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    ProfileEvent.SessionExpired -> onNavigateToLogin()
                }
            }
        }
    }
}
```

### Yang tidak boleh dilakukan di Compose

- Memanggil repository atau use case langsung dari composable.
- Menjalankan `launch` di body composable tanpa `LaunchedEffect`.
- Memakai `collectAsState()` untuk data ViewModel pada layar baru — pakai `collectAsStateWithLifecycle()`.
- Menyimpan hasil operasi async di variabel biasa (bukan `remember`/state).

Lanjut ke [05 — Flow Integration](05-flow-integration.md).
