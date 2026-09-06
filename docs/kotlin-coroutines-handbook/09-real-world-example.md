# 09 — Real-World Example: Load User Profile

> Implementasi utuh dari Retrofit sampai Compose. Kode ini mengikuti konvensi proyek: `ApiEnvelope`, `AppResult`, Hilt, `StateFlow`, dan `collectAsStateWithLifecycle`.

## Alur

```text
Compose Screen
     ↓ event: load / retry
ViewModel  (viewModelScope, StateFlow<ProfileUiState>)
     ↓ suspend
UseCase    (GetUserProfileUseCase)
     ↓ suspend
Repository (UserRepositoryImpl, AppResult)
     ↓ suspend
Retrofit API (UserApi)
```

---

## 1. Model domain

```kotlin
// domain/user/model/User.kt
data class User(
    val id: String,
    val name: String,
    val email: String,
    val photoUrl: String?,
)
```

---

## 2. Remote layer

```kotlin
// data/user/remote/UserDto.kt
@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    @SerialName("photo_url") val photoUrl: String? = null,
)

fun UserDto.toDomain(): User = User(
    id = id,
    name = name,
    email = email,
    photoUrl = photoUrl,
)
```

```kotlin
// data/user/remote/UserApi.kt
interface UserApi {

    @GET("v1/users/me")
    suspend fun getProfile(): ApiEnvelope<UserDto>
}
```

Retrofit `suspend fun` sudah menjalankan request di luar main thread — **tidak** perlu `withContext(Dispatchers.IO)`.

---

## 3. Repository

```kotlin
// domain/user/repository/UserRepository.kt
interface UserRepository {
    suspend fun getProfile(): AppResult<User>
}
```

```kotlin
// data/user/repository/UserRepositoryImpl.kt
class UserRepositoryImpl @Inject constructor(
    private val api: UserApi,
    private val json: Json,
) : UserRepository {

    override suspend fun getProfile(): AppResult<User> = runApiCatching(json) {
        api.getProfile().requirePayload().map(UserDto::toDomain)
    }
}
```

Repository tidak membuat `CoroutineScope`, tidak memanggil `launch`, dan tidak memindahkan dispatcher. Tugasnya hanya menyediakan data dan menerjemahkan error menjadi `AppFailure`.

---

## 4. UseCase

```kotlin
// domain/user/usecase/GetUserProfileUseCase.kt
class GetUserProfileUseCase @Inject constructor(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(): AppResult<User> = repository.getProfile()
}
```

Untuk kasus sesederhana ini, UseCase hanya meneruskan. Ia mulai berguna saat ada validasi, penggabungan sumber data, atau aturan bisnis.

---

## 5. Hilt module

```kotlin
// di/UserModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class UserModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    companion object {

        @Provides
        @Singleton
        fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)
    }
}
```

---

## 6. UI state

```kotlin
// presentation/viewmodel/profile/ProfileUiState.kt
data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
)
```

Satu `data class` immutable untuk seluruh layar. Setiap perubahan menghasilkan objek baru lewat `copy`.

---

## 7. ViewModel

```kotlin
// presentation/viewmodel/profile/ProfileViewModel.kt
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserProfile: GetUserProfileUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = getUserProfile()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isLoading = false, user = result.data)
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.failure.toUiMessage())
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
```

Yang terjadi di sini:

- `viewModelScope` adalah pemilik coroutine → otomatis dibatalkan di `onCleared()`.
- Tidak ada `withContext` karena Retrofit sudah main-safe.
- Guard `isLoading` mencegah request ganda saat user menekan retry berkali-kali.
- Error menjadi bagian dari state, bukan exception yang lolos ke UI.

---

## 8. Compose screen

```kotlin
// presentation/compose/profile/ProfileRoute.kt
@Composable
fun ProfileRoute(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileScreen(
        state = state,
        onRetry = viewModel::load,
        onDismissError = viewModel::dismissError,
        modifier = modifier,
    )
}
```

```kotlin
// presentation/compose/profile/ProfileScreen.kt
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        val message = state.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissError()
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.user != null -> ProfileContent(state.user)
                else -> RetryMessage(onRetry = onRetry)
            }
        }
    }
}

@Composable
private fun ProfileContent(user: User, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = user.name, style = MaterialTheme.typography.titleLarge)
        Text(text = user.email, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RetryMessage(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Data profil belum tersedia.")
        Button(onClick = onRetry) { Text("Coba lagi") }
    }
}
```

Catatan Compose:

- `collectAsStateWithLifecycle()` menghentikan collection saat layar di background.
- `LaunchedEffect(state.error)` menampilkan snackbar sebagai efek dari perubahan state, lalu membersihkan error supaya tidak muncul lagi saat rotasi.
- Composable tidak memanggil repository/use case sama sekali — hanya memanggil fungsi ViewModel.
- `ProfileScreen` stateless dan bisa di-preview tanpa ViewModel.

---

## 9. Versi Fragment (bila layar masih View-based)

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

        binding.retryButton.setOnClickListener { viewModel.load() }
    }

    private fun render(state: ProfileUiState) {
        binding.progress.isVisible = state.isLoading
        binding.name.text = state.user?.name.orEmpty()
        binding.email.text = state.user?.email.orEmpty()
        binding.retryButton.isVisible = !state.isLoading && state.user == null
    }
}
```

---

## Ringkasan tanggung jawab

| Komponen | Coroutine yang dipakai | Alasan |
|---|---|---|
| `UserApi` | `suspend fun` | Retrofit menjalankan request async sendiri |
| `UserRepositoryImpl` | `suspend fun` + `runApiCatching` | Tidak punya lifecycle; menerjemahkan error |
| `GetUserProfileUseCase` | `suspend fun` | Aturan bisnis, tanpa scope |
| `ProfileViewModel` | `viewModelScope.launch` | Pemilik coroutine, mengelola state |
| `ProfileRoute` | `collectAsStateWithLifecycle` | Membaca state secara lifecycle-aware |
| `ProfileScreen` | `LaunchedEffect` | Efek UI dari perubahan state |
| `ProfileFragment` | `repeatOnLifecycle` | Collect aman terhadap lifecycle view |

Lanjut ke [10 — Testing](10-testing.md).
