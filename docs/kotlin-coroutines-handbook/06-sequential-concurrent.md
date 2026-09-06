# 06 — Sequential vs Concurrent Operation

> `async`/`await` sering dipakai berlebihan. Bab ini menentukan kapan concurrency benar-benar memberi manfaat, lengkap dengan contoh dashboard nyata.

## Sequential

```kotlin
suspend fun load(): Pair<User, List<Transaction>> {
    val user = repository.getUser()                 // 400 ms
    val transactions = repository.getTransactions() // 600 ms
    return user to transactions
}
// Total ≈ 1000 ms
```

Gunakan sequential ketika:

- Panggilan kedua **membutuhkan** hasil panggilan pertama.
- Latensi total masih dapat diterima.
- Operasinya berhubungan dengan resource yang sama dan tidak boleh bersamaan (misalnya transaksi database).

```kotlin
// Wajib sequential: butuh token dari login
val session = repository.login(credentials)
val profile = repository.getProfile(session.userId)
```

---

## Concurrent

```kotlin
suspend fun load(): Pair<User, List<Transaction>> = coroutineScope {
    val user = async { repository.getUser() }                 // 400 ms
    val transactions = async { repository.getTransactions() } // 600 ms
    user.await() to transactions.await()
}
// Total ≈ 600 ms
```

Syarat agar concurrency bermanfaat:

1. Operasinya **independen** (tidak saling membutuhkan hasil).
2. Operasinya benar-benar asinkron (network/disk), bukan hanya kalkulasi ringan.
3. Ada minimal dua operasi.

---

## Kapan `async` TIDAK berguna

```kotlin
// 1. Satu operasi saja: tidak ada yang diparalelkan
val user = async { repository.getUser() }.await()
// Benar:
val user = repository.getUser()
```

```kotlin
// 2. await langsung setelah async: tetap sequential, hanya lebih rumit
val user = async { repository.getUser() }.await()
val transactions = async { repository.getTransactions() }.await()
// Benar: panggil langsung, atau paralelkan dengan benar
```

```kotlin
// 3. Operasi ringan: overhead lebih besar daripada manfaatnya
val a = async { list.map(::format) }
```

```kotlin
// 4. Menulis ke resource yang sama secara paralel
coroutineScope {
    launch { dao.upsert(a) }
    launch { dao.upsert(b) }   // gunakan transaksi, bukan paralel
}
```

---

## `awaitAll` untuk banyak item

```kotlin
suspend fun loadDetails(ids: List<String>): List<Detail> = coroutineScope {
    ids.map { id -> async { repository.getDetail(id) } }.awaitAll()
}
```

Kalau jumlah item bisa besar, batasi paralelisme agar tidak membanjiri server:

```kotlin
suspend fun loadDetails(ids: List<String>): List<Detail> = coroutineScope {
    val semaphore = Semaphore(MAX_PARALLEL_REQUEST)

    ids.map { id ->
        async { semaphore.withPermit { repository.getDetail(id) } }
    }.awaitAll()
}
```

---

## Contoh nyata: Dashboard

Layar dashboard membutuhkan tiga data independen:

```text
User Profile
Transactions
Notifications
```

### Versi 1 — semua data wajib ada (`coroutineScope`)

Bila salah satu gagal, seluruh layar dianggap gagal.

```kotlin
data class Dashboard(
    val profile: Profile,
    val transactions: List<Transaction>,
    val notifications: List<Notification>,
)

class GetDashboardUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val transactionRepository: TransactionRepository,
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(): Dashboard = coroutineScope {
        val profile = async { profileRepository.getProfile() }
        val transactions = async { transactionRepository.getRecent() }
        val notifications = async { notificationRepository.getUnread() }

        Dashboard(
            profile = profile.await(),
            transactions = transactions.await(),
            notifications = notifications.await(),
        )
    }
}
```

Perilakunya: satu `async` gagal → `coroutineScope` membatalkan dua `async` lain → exception dilempar ke pemanggil. Ini benar bila layar memang tidak berguna tanpa salah satu data.

### Versi 2 — sebagian data boleh gagal (`supervisorScope`)

Dashboard tetap tampil walau notifikasi gagal dimuat.

```kotlin
data class DashboardPartial(
    val profile: Profile,
    val transactions: List<Transaction>,
    val notifications: List<Notification>,
    val notificationFailed: Boolean,
)

class GetDashboardUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val transactionRepository: TransactionRepository,
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(): DashboardPartial = supervisorScope {
        val profile = async { profileRepository.getProfile() }
        val transactions = async { transactionRepository.getRecent() }
        val notifications = async { notificationRepository.getUnread() }

        val notificationResult = runCatching { notifications.await() }

        DashboardPartial(
            profile = profile.await(),
            transactions = transactions.await(),
            notifications = notificationResult.getOrDefault(emptyList()),
            notificationFailed = notificationResult.isFailure,
        )
    }
}
```

Dua hal yang wajib diingat pada `supervisorScope` + `async`:

1. Exception **tidak hilang** — ia muncul saat `await()`, jadi tetap perlu `runCatching`/`try-catch`.
2. `runCatching` menangkap `CancellationException` juga. Bila coroutine bisa dibatalkan, periksa ulang:

```kotlin
val notificationResult = try {
    Result.success(notifications.await())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    Result.failure(throwable)
}
```

### Versi 3 — dengan `AppResult` (konvensi proyek ini)

Karena repository di proyek ini mengembalikan `AppResult` alih-alih melempar exception, versi paralelnya menjadi lebih sederhana: kegagalan **tidak** membatalkan sibling karena tidak ada exception yang dilempar.

```kotlin
class GetDashboardUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val transactionRepository: TransactionRepository,
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(): DashboardResult = coroutineScope {
        val profile = async { profileRepository.getProfile() }
        val transactions = async { transactionRepository.getRecent() }
        val notifications = async { notificationRepository.getUnread() }

        DashboardResult(
            profile = profile.await(),
            transactions = transactions.await(),
            notifications = notifications.await(),
        )
    }
}

data class DashboardResult(
    val profile: AppResult<Profile>,
    val transactions: AppResult<List<Transaction>>,
    val notifications: AppResult<List<Notification>>,
)
```

ViewModel kemudian memutuskan apa yang ditampilkan per bagian:

```kotlin
fun load() {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }

        val result = getDashboard()

        _uiState.update { state ->
            state.copy(
                isLoading = false,
                profile = (result.profile as? AppResult.Success)?.data,
                transactions = (result.transactions as? AppResult.Success)?.data.orEmpty(),
                notifications = (result.notifications as? AppResult.Success)?.data.orEmpty(),
                error = (result.profile as? AppResult.Failure)?.failure?.toMessage(),
            )
        }
    }
}
```

---

## Ringkasan keputusan

```text
Apakah operasi kedua butuh hasil operasi pertama?
    Ya  → sequential (panggil langsung, tanpa async)
    Tidak → lanjut

Apakah ada ≥ 2 operasi I/O yang latensinya terasa?
    Tidak → sequential
    Ya   → lanjut

Apakah semua data wajib ada?
    Ya    → coroutineScope + async + await
    Tidak → supervisorScope + async + runCatching per bagian
            (atau kembalikan AppResult per bagian)
```

| Situasi | Pilihan |
|---|---|
| Login lalu ambil profil | Sequential |
| 3 endpoint dashboard independen | `coroutineScope` + `async` |
| 3 endpoint, sebagian boleh gagal | `supervisorScope` + `async` + `runCatching` |
| N detail dari list id | `map { async { } }.awaitAll()` (+ `Semaphore` bila banyak) |
| Kalkulasi ringan | Sequential, tanpa coroutine tambahan |
| Menulis ke database berkali-kali | Satu transaksi, bukan paralel |

Lanjut ke [07 — Error Handling](07-error-handling.md).
