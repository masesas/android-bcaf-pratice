# 10 — Testing Coroutine

> Pengenalan praktis untuk menguji `suspend fun`, `Flow`, dan ViewModel tanpa menunggu waktu nyata.

## Dependency

```toml
# gradle/libs.versions.toml
[versions]
coroutines = "1.10.2"
turbine = "1.2.1"

[libraries]
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
```

```kotlin
// app/build.gradle.kts
testImplementation(libs.kotlinx.coroutines.test)
testImplementation(libs.turbine)
```

> Sesuaikan versi dengan `kotlinx-coroutines-core` yang dibawa Kotlin `2.4.10` di proyek ini agar tidak ada konflik versi.

---

## `runTest`

`runTest` menjalankan blok test di dalam coroutine dengan **virtual time**: `delay(10_000)` selesai seketika.

```kotlin
@Test
fun `delay tidak memakan waktu nyata`() = runTest {
    val elapsed = testScheduler.currentTime
    delay(10_000)
    assertEquals(10_000, testScheduler.currentTime - elapsed)
}
```

Yang perlu diingat:

- `runTest` menggantikan `runBlocking` untuk test.
- Test gagal bila ada coroutine yang belum selesai di akhir (kecuali dijalankan di `backgroundScope`).
- Untuk coroutine yang berjalan terus (mis. `stateIn`), pakai `backgroundScope`.

---

## Test dispatcher

| Dispatcher | Perilaku | Untuk |
|---|---|---|
| `StandardTestDispatcher()` | Coroutine antre, baru jalan saat `advanceUntilIdle()`/`runCurrent()` | Menguji urutan dan state antara (mis. `isLoading`) |
| `UnconfinedTestDispatcher()` | Coroutine langsung jalan sampai suspend pertama | Test sederhana yang hanya memeriksa hasil akhir |

```kotlin
@Test
fun `state loading muncul sebelum hasil`() = runTest {
    val viewModel = ProfileViewModel(getUserProfile)

    viewModel.load()
    assertTrue(viewModel.uiState.value.isLoading)   // belum dijalankan

    advanceUntilIdle()
    assertFalse(viewModel.uiState.value.isLoading)  // sudah selesai
}
```

Kontrol waktu yang tersedia: `advanceUntilIdle()`, `advanceTimeBy(ms)`, `runCurrent()`.

---

## `MainDispatcherRule`

`viewModelScope` memakai `Dispatchers.Main`, yang tidak tersedia di unit test JVM. Ganti dengan test dispatcher.

```kotlin
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

```kotlin
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
}
```

Tanpa rule ini, test yang menyentuh `viewModelScope` akan gagal dengan `Module with the Main dispatcher had failed to initialize`.

---

## Testing suspend function (Repository)

```kotlin
class UserRepositoryImplTest {

    private val api = FakeUserApi()
    private val repository = UserRepositoryImpl(api, Json)

    @Test
    fun `mengembalikan Success ketika API mengirim payload`() = runTest {
        api.response = ApiEnvelope(data = UserDto("1", "Rina", "rina@mail.com"))

        val result = repository.getProfile()

        assertTrue(result is AppResult.Success)
        assertEquals("Rina", (result as AppResult.Success).data.name)
    }

    @Test
    fun `mengembalikan Failure Network ketika terjadi IOException`() = runTest {
        api.error = IOException("offline")

        val result = repository.getProfile()

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).failure is CommonFailure.Network)
    }
}
```

```kotlin
private class FakeUserApi : UserApi {

    var response: ApiEnvelope<UserDto>? = null
    var error: Throwable? = null

    override suspend fun getProfile(): ApiEnvelope<UserDto> {
        error?.let { throw it }
        return requireNotNull(response)
    }
}
```

---

## Testing ViewModel

```kotlin
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val useCase = FakeGetUserProfileUseCase()

    @Test
    fun `sukses memperbarui state dengan user`() = runTest {
        useCase.result = AppResult.success(User("1", "Rina", "rina@mail.com", null))

        val viewModel = ProfileViewModel(useCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Rina", state.user?.name)
        assertNull(state.error)
    }

    @Test
    fun `gagal mengisi pesan error dan menghentikan loading`() = runTest {
        useCase.result = AppResult.failure(CommonFailure.Network(IOException()))

        val viewModel = ProfileViewModel(useCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.user)
        assertNotNull(state.error)
    }

    @Test
    fun `load kedua diabaikan selama masih loading`() = runTest {
        val viewModel = ProfileViewModel(useCase)

        viewModel.load()
        viewModel.load()
        advanceUntilIdle()

        assertEquals(1, useCase.invocationCount)
    }
}
```

---

## Testing `StateFlow` dengan Turbine

```kotlin
@Test
fun `state berpindah dari loading ke success`() = runTest {
    useCase.result = AppResult.success(user)
    val viewModel = ProfileViewModel(useCase)

    viewModel.uiState.test {
        assertTrue(awaitItem().isLoading)
        val loaded = awaitItem()
        assertEquals(user, loaded.user)
        cancelAndIgnoreRemainingEvents()
    }
}
```

Tanpa Turbine, `StateFlow` bisa diuji dengan `backgroundScope`:

```kotlin
@Test
fun `mengumpulkan state tanpa menggantung test`() = runTest {
    val emissions = mutableListOf<ProfileUiState>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.uiState.toList(emissions)
    }

    advanceUntilIdle()
    assertTrue(emissions.last().user != null)
}
```

---

## Testing operasi paralel

```kotlin
@Test
fun `dashboard memuat tiga sumber secara paralel`() = runTest {
    profileRepository.delayMs = 300
    transactionRepository.delayMs = 300
    notificationRepository.delayMs = 300

    val start = testScheduler.currentTime
    getDashboard()
    val duration = testScheduler.currentTime - start

    assertEquals(300, duration)   // bukan 900 → benar-benar paralel
}
```

Virtual time membuat pengujian paralelisme cepat dan deterministik.

---

## Testing dispatcher yang di-inject

```kotlin
@Test
fun `membaca cache di dispatcher yang diberikan`() = runTest {
    val cache = FileProfileCache(file, StandardTestDispatcher(testScheduler))

    val content = cache.read()

    assertEquals("{}", content)
}
```

Ini alasan dispatcher sebaiknya di-inject bila memang ada `withContext` di layer data.

---

## Kesalahan umum saat testing

| Kesalahan | Akibat | Perbaikan |
|---|---|---|
| Memakai `runBlocking` | Test menunggu waktu nyata | `runTest` |
| Lupa `Dispatchers.setMain` | `IllegalStateException` di ViewModel test | `MainDispatcherRule` |
| `Thread.sleep` untuk menunggu coroutine | Flaky | `advanceUntilIdle()` |
| Meng-collect `StateFlow` di `runTest` tanpa `backgroundScope` | Test menggantung | Turbine atau `backgroundScope` |
| Dispatcher di-hardcode di production code | Tidak bisa dikontrol test | Injeksi `CoroutineDispatcher` |
| Assert sebelum coroutine dijalankan | Hasil belum ada | `advanceUntilIdle()` sebelum assert |

---

## Checklist testing

- [ ] Semua test coroutine memakai `runTest`.
- [ ] Test ViewModel memakai `MainDispatcherRule`.
- [ ] Fake/stub repository dibuat sebagai implementasi interface, bukan mock berat.
- [ ] Jalur sukses, gagal, dan loading diuji.
- [ ] Perilaku cancellation diuji untuk fitur yang memakai `Job` (search/debounce).
- [ ] Tidak ada `Thread.sleep` di test.

Lanjut ke [11 — Anti-Pattern](11-anti-pattern.md).
