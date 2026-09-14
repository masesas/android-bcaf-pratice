# Jetpack Compose Composition API Handbook

> Referensi praktis mengenai API yang berkaitan dengan **composition, recomposition, CompositionLocal, side effect, lifecycle composition, dan identity** di Jetpack Compose.

---

## 1. Gambaran Singkat

Di Jetpack Compose, **Composition** adalah proses ketika Compose menjalankan fungsi `@Composable` untuk membangun UI tree dan mencatat dependency state yang dibaca oleh UI.

Ketika state berubah, Compose tidak selalu menjalankan ulang seluruh UI. Compose melakukan **recomposition** pada bagian composition yang terdampak.

```text
State berubah
     ↓
Compose mendeteksi consumer state
     ↓
Recomposition
     ↓
UI yang relevan diperbarui
```

Tiga konsep utama:

- **Composition** → pertama kali composable masuk ke UI tree.
- **Recomposition** → composable dijalankan ulang karena state/dependency berubah.
- **Leaving Composition** → composable keluar dari UI tree dan resource/effect terkait perlu dibersihkan.

---

# 2. API Table — Composition & Recomposition

| API / Konsep | Kapan Digunakan | Alasan Menggunakan | Jangan Digunakan Ketika | Hal Penting |
|---|---|---|---|---|
| `@Composable` | Membuat fungsi UI Compose | Menandai fungsi sebagai bagian dari composition | Untuk business logic murni atau operasi I/O | Fungsi dapat dipanggil ulang berkali-kali saat recomposition |
| `remember` | Menyimpan value selama composable tetap berada di composition | Menghindari pembuatan ulang object/value setiap recomposition | Untuk state yang harus survive process death | Hilang ketika composable keluar dari composition |
| `rememberSaveable` | State UI yang perlu survive activity recreation | Menggunakan mekanisme saved instance state | Untuk object kompleks yang tidak saveable tanpa saver | Cocok untuk input form, selected tab, query sederhana |
| `key` | Memberikan identity eksplisit pada subtree | Membantu Compose mempertahankan state berdasarkan identity | Jika identity natural sudah stabil | Sangat penting pada dynamic/reordered UI |
| `currentCompositeKeyHash` | Debug/internal identity composition | Membantu kasus tooling/framework tertentu | Untuk business UI biasa | Jarang diperlukan pada aplikasi normal |
| `ReusableContent` | Framework/UI infra yang perlu me-reuse composition content | Optimasi reuse node/composition | UI aplikasi normal | API advanced; hindari tanpa kebutuhan jelas |
| `ReusableContentHost` | Mengontrol apakah reusable composition aktif | Framework-level optimization | UI biasa | Advanced API |

---

# 3. `remember`

## Fungsi

Menyimpan sebuah value selama composable masih berada di composition.

```kotlin
@Composable
fun Counter() {
    var count by remember {
        mutableStateOf(0)
    }

    Button(onClick = { count++ }) {
        Text("Count: $count")
    }
}
```

Tanpa `remember`:

```kotlin
var count = mutableStateOf(0)
```

object tersebut dapat dibuat ulang setiap recomposition sehingga state tidak bertahan sebagaimana yang diharapkan.

## Gunakan ketika

- Membuat local UI state.
- Menyimpan object yang mahal dibuat ulang.
- Menyimpan controller/state object seperti `LazyListState`.
- Menyimpan hasil kalkulasi yang bergantung pada key tertentu.

```kotlin
val listState = rememberLazyListState()
```

## Jangan gunakan ketika

- State harus dimiliki ViewModel.
- State harus survive process death.
- Value seharusnya selalu dihitung langsung dan murah.

## Dengan key

```kotlin
val formatted = remember(user.id) {
    expensiveFormatting(user)
}
```

Ketika `user.id` berubah, block `remember` dieksekusi ulang.

---

# 4. `rememberSaveable`

Digunakan untuk menyimpan state yang perlu survive configuration change dan recreation yang didukung SavedState.

```kotlin
var query by rememberSaveable {
    mutableStateOf("")
}
```

## Cocok untuk

- text field value sederhana
- selected tab
- checkbox
- page index
- filter sederhana

## Tidak cocok untuk

```kotlin
rememberSaveable {
    Repository(...)
}
```

atau object besar seperti repository, network client, bitmap, database object, dan dependency lain.

---

# 5. `key`

`key` memberikan identity pada composition subtree.

```kotlin
users.forEach { user ->
    key(user.id) {
        UserCard(user)
    }
}
```

Ini penting jika posisi item dapat berubah.

Tanpa identity stabil:

```text
Index 0 → User A
Index 1 → User B
```

Setelah reorder:

```text
Index 0 → User B
Index 1 → User A
```

Compose berpotensi mengasosiasikan remembered state berdasarkan posisi call-site.

Dengan `key(user.id)`, identity mengikuti user.

## Pada LazyColumn

Lebih umum gunakan:

```kotlin
items(
    items = users,
    key = { it.id }
) { user ->
    UserCard(user)
}
```

---

# 6. CompositionLocal API

`CompositionLocal` memungkinkan suatu value tersedia secara implicit kepada seluruh composable di bawah sebuah subtree.

## API Table

| API | Kapan Digunakan | Alasan | Jangan Digunakan | Catatan |
|---|---|---|---|---|
| `compositionLocalOf` | Local dependency yang dapat berubah | Consumer yang membaca value dapat dilacak untuk recomposition | Data screen biasa | Cocok untuk dynamic environment value |
| `staticCompositionLocalOf` | Value yang jarang berubah | Lebih ringan jika provider hampir selalu statis | Value yang sering berubah | Perubahan dapat memicu recomposition subtree yang lebih luas |
| `CompositionLocalProvider` | Menyediakan value pada subtree | Menghindari parameter drilling untuk environment dependency | Sebagai pengganti semua parameter | Scope hanya berlaku pada child composition |
| `provides` | Memberi value pada CompositionLocal | Syntax utama provider | — | `LocalX provides value` |
| `providesDefault` | Memberi default jika belum ada provider | Framework/design system tertentu | Bila override eksplisit dibutuhkan | Lebih jarang digunakan |
| `.current` | Membaca value CompositionLocal | Mengambil value aktif pada scope | Di luar composable | Membuat composable menjadi consumer local |

---

# 7. `compositionLocalOf`

```kotlin
val LocalFeatureFlags = compositionLocalOf {
    FeatureFlags()
}
```

Gunakan jika value memang bisa berubah dan consumer perlu mendapat recomposition secara lebih terarah.

Contoh:

```kotlin
val LocalAppMode = compositionLocalOf {
    AppMode.Standard
}
```

Provider:

```kotlin
CompositionLocalProvider(
    LocalAppMode provides AppMode.Admin
) {
    AppContent()
}
```

Consumer:

```kotlin
val mode = LocalAppMode.current
```

---

# 8. `staticCompositionLocalOf`

```kotlin
val LocalSpacing = staticCompositionLocalOf {
    AppSpacing()
}
```

Cocok untuk value yang secara praktis tidak sering berubah.

Contoh:

```kotlin
data class AppSpacing(
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
)

val LocalSpacing = staticCompositionLocalOf {
    AppSpacing()
}
```

Provider:

```kotlin
CompositionLocalProvider(
    LocalSpacing provides AppSpacing()
) {
    MaterialTheme {
        AppContent()
    }
}
```

Consumer:

```kotlin
val spacing = LocalSpacing.current

Column(
    modifier = Modifier.padding(spacing.medium)
) {
    // ...
}
```

---

# 9. Kapan CompositionLocal Layak Digunakan?

Gunakan untuk dependency yang bersifat **environment-like** dan digunakan luas di subtree.

Contoh yang masuk akal:

- theme configuration
- spacing system
- typography extension
- locale
- layout direction
- feature/environment configuration
- analytics interface tertentu
- permission provider tertentu
- dependency UI infrastructure

Compose sendiri menggunakan pattern ini untuk:

```kotlin
LocalContext.current
LocalDensity.current
LocalConfiguration.current
LocalLayoutDirection.current
LocalLifecycleOwner.current
```

---

# 10. Kapan Jangan Menggunakan CompositionLocal?

## Jangan untuk screen state biasa

Kurang baik:

```kotlin
val LocalLoanApplications = compositionLocalOf<List<LoanApplication>> {
    emptyList()
}
```

Kemudian child membaca:

```kotlin
val applications = LocalLoanApplications.current
```

Lebih baik:

```kotlin
@Composable
fun LoanApplicationList(
    applications: List<LoanApplication>,
)
```

Alasannya:

- dependency eksplisit
- lebih mudah diuji
- lebih mudah digunakan ulang
- lebih mudah preview
- data flow jelas

---

## Jangan menjadikan ViewModel sebagai CompositionLocal tanpa alasan kuat

Hindari:

```kotlin
val LocalLoanViewModel = compositionLocalOf<LoanViewModel> {
    error("LoanViewModel not provided")
}
```

lalu di banyak child:

```kotlin
val viewModel = LocalLoanViewModel.current
```

Lebih baik gunakan container/presentation pattern:

```kotlin
@Composable
fun LoanRoute(
    viewModel: LoanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LoanScreen(
        uiState = uiState,
        onRefresh = viewModel::refresh,
    )
}
```

Child menerima data secara eksplisit:

```kotlin
@Composable
fun LoanScreen(
    uiState: LoanUiState,
    onRefresh: () -> Unit,
)
```

---

# 11. Side Effect APIs

Side effect adalah operasi yang terjadi di luar proses rendering UI murni.

Contoh:

- menjalankan coroutine
- analytics
- listener registration
- callback ke object non-Compose
- membersihkan resource

## API Table

| API | Kapan Digunakan | Alasan | Jangan Digunakan | Lifecycle |
|---|---|---|---|---|
| `LaunchedEffect` | Menjalankan coroutine berdasarkan lifecycle composition/key | Coroutine otomatis dibatalkan saat keluar composition | Untuk event langsung dari click jika tidak perlu composition lifecycle | Restart jika key berubah |
| `rememberCoroutineScope` | Coroutine dari callback/event handler | Scope mengikuti composition | Untuk otomatis menjalankan effect saat composition | Dibatalkan saat composable keluar |
| `DisposableEffect` | Register listener/resource + cleanup | Menyediakan `onDispose` | Coroutine biasa | Dispose saat key berubah / keluar composition |
| `SideEffect` | Sinkronisasi state Compose ke object eksternal setelah successful composition | Dijamin setelah composition sukses | Network call / coroutine | Bisa berjalan setiap recomposition |
| `produceState` | Mengubah source async/non-Compose menjadi Compose `State` | Menyatukan producer lifecycle dengan composition | Jika sudah punya `StateFlow` yang bisa collect langsung | Producer dibatalkan saat keluar composition |
| `derivedStateOf` | Derived state yang seharusnya berubah lebih jarang dari input | Mengurangi unnecessary recomposition | Perhitungan sederhana yang berubah sama seringnya | Berbasis snapshot state |
| `snapshotFlow` | Mengubah pembacaan Compose State menjadi Flow | Integrasi Compose state dengan Flow operators | StateFlow biasa | Berjalan dalam coroutine |
| `rememberUpdatedState` | Effect perlu callback/value terbaru tanpa restart effect | Menghindari stale lambda/value | Jika effect memang perlu restart saat value berubah | Sangat berguna pada long-lived effect |

---

# 12. `LaunchedEffect`

```kotlin
LaunchedEffect(userId) {
    viewModel.loadUser(userId)
}
```

Ketika `userId` berubah:

```text
old coroutine
    ↓ cancel
new coroutine
    ↓ start
```

## Gunakan untuk

- menjalankan suspend function saat masuk composition
- menjalankan logic saat key berubah
- collect Flow yang lifecycle-nya memang terikat ke composable
- timer/delay UI

Contoh:

```kotlin
LaunchedEffect(Unit) {
    delay(2_000)
    onTimeout()
}
```

## Hati-hati dengan

```kotlin
LaunchedEffect(Unit) {
    repository.loadData()
}
```

Jika business logic lebih tepat dimiliki ViewModel, pindahkan ke ViewModel.

---

# 13. `rememberCoroutineScope`

Gunakan ketika coroutine dipicu oleh user event.

```kotlin
val scope = rememberCoroutineScope()

Button(
    onClick = {
        scope.launch {
            snackbarHostState.showSnackbar("Saved")
        }
    }
) {
    Text("Save")
}
```

Rule praktis:

```text
Effect karena composable masuk / key berubah
→ LaunchedEffect

Effect karena user event
→ rememberCoroutineScope
```

---

# 14. `DisposableEffect`

Digunakan ketika Anda perlu register dan unregister resource.

```kotlin
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        // handle lifecycle
    }

    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
    }
}
```

Cocok untuk:

- BroadcastReceiver manual
- listener
- observer
- callback registration
- sensor listener

Jangan gunakan jika tidak membutuhkan cleanup.

---

# 15. `SideEffect`

Menjalankan block setelah composition berhasil.

```kotlin
SideEffect {
    analytics.setUserProperty("screen", screenName)
}
```

Gunakan untuk menyinkronkan Compose state ke object yang bukan Compose-aware.

Jangan lakukan operasi mahal di sini karena bisa dipanggil berulang pada recomposition.

---

# 16. `rememberUpdatedState`

Masalah umum:

```kotlin
LaunchedEffect(Unit) {
    delay(5_000)
    onTimeout()
}
```

Jika `onTimeout` berubah selama 5 detik tetapi `LaunchedEffect(Unit)` tidak restart, effect dapat memiliki referensi callback lama tergantung pola penggunaannya.

Gunakan:

```kotlin
val currentOnTimeout by rememberUpdatedState(onTimeout)

LaunchedEffect(Unit) {
    delay(5_000)
    currentOnTimeout()
}
```

Ini menjaga effect tetap berjalan tetapi menggunakan callback terbaru.

---

# 17. `derivedStateOf`

Contoh:

```kotlin
val showScrollToTop by remember {
    derivedStateOf {
        listState.firstVisibleItemIndex > 0
    }
}
```

`firstVisibleItemIndex` dapat berubah sering, tetapi UI hanya peduli apakah hasil boolean berubah.

## Cocok

```text
input berubah sangat sering
↓
derived result berubah lebih jarang
↓
UI hanya membutuhkan derived result
```

## Jangan gunakan berlebihan

Tidak perlu:

```kotlin
val fullName by remember {
    derivedStateOf {
        "$firstName $lastName"
    }
}
```

jika hasil memang harus berubah setiap kali salah satu input berubah dan kalkulasinya murah.

Cukup:

```kotlin
val fullName = "$firstName $lastName"
```

---

# 18. `snapshotFlow`

Mengubah Compose Snapshot State menjadi Flow.

```kotlin
LaunchedEffect(listState) {
    snapshotFlow {
        listState.firstVisibleItemIndex
    }
        .distinctUntilChanged()
        .collect { index ->
            analytics.trackScroll(index)
        }
}
```

Cocok ketika Anda membutuhkan operator Flow seperti:

```text
map
filter
distinctUntilChanged
debounce
collect
```

pada Compose state.

---

# 19. `produceState`

Mengubah asynchronous source menjadi Compose `State`.

```kotlin
@Composable
fun loadUser(userId: Long): State<User?> {
    return produceState<User?>(
        initialValue = null,
        key1 = userId,
    ) {
        value = repository.getUser(userId)
    }
}
```

Namun pada arsitektur Android modern, jika data sudah tersedia sebagai `StateFlow`, biasanya lebih baik langsung:

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

---

# 20. State Conversion APIs

| API | Source | Output | Umum Digunakan Untuk |
|---|---|---|---|
| `collectAsState()` | `Flow` / `StateFlow` | Compose `State` | Platform Compose umum |
| `collectAsStateWithLifecycle()` | `Flow` / `StateFlow` | Compose `State` | Android UI; lifecycle-aware |
| `observeAsState()` | LiveData | Compose `State` | Integrasi legacy LiveData |
| `subscribeAsState()` | RxJava | Compose `State` | Integrasi RxJava |

Untuk Android modern, prefer:

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

---

# 21. `CompositionLocalProvider` vs Parameter

## Parameter

Gunakan jika dependency merupakan bagian dari kontrak composable.

```kotlin
@Composable
fun LoanCard(
    loan: Loan,
    onClick: () -> Unit,
)
```

Keuntungan:

- dependency terlihat jelas
- reusable
- testable
- preview mudah

## CompositionLocal

Gunakan jika dependency adalah environment dari seluruh subtree.

```kotlin
val spacing = LocalSpacing.current
```

### Rule sederhana

| Jenis Data | Pendekatan |
|---|---|
| Screen UI state | Parameter |
| Item/model | Parameter |
| User event callback | Parameter |
| ViewModel | Route/container level |
| Theme | CompositionLocal |
| Density | CompositionLocal |
| Locale | CompositionLocal |
| Spacing design system | CompositionLocal |
| Context | `LocalContext` |
| LifecycleOwner | `LocalLifecycleOwner` |

---

# 22. Built-in CompositionLocal yang Sering Digunakan

| API | Isi | Contoh Penggunaan |
|---|---|---|
| `LocalContext` | Android `Context` | Toast, resource, dependency tertentu |
| `LocalConfiguration` | Android configuration | Orientation, screen config |
| `LocalDensity` | Density / font scale | px ↔ dp conversion |
| `LocalLayoutDirection` | LTR / RTL | Custom layout |
| `LocalLifecycleOwner` | Lifecycle owner saat ini | Lifecycle-aware observer |
| `LocalView` | Android View host | Interop / system interaction |
| `LocalInspectionMode` | Apakah berjalan di preview/tooling | Mock behavior saat preview |
| `LocalFocusManager` | Focus manager | Clear/focus control |
| `LocalSoftwareKeyboardController` | Keyboard controller | Hide/show keyboard |
| `LocalUriHandler` | URI handler | Membuka URL |
| `LocalClipboardManager` / clipboard APIs | Clipboard | Copy/paste |
| `LocalHapticFeedback` | Haptic feedback | Vibrasi feedback |
| `LocalAccessibilityManager` | Accessibility service info | Accessibility-aware UI |

---

# 23. Stability dan Recomposition

Compose menentukan apakah composable dapat di-skip berdasarkan dependency dan stability.

Contoh immutable model yang baik:

```kotlin
data class UserUiModel(
    val id: Long,
    val name: String,
)
```

Hindari mutable object yang diubah langsung tanpa Compose State:

```kotlin
class User {
    var name: String = ""
}
```

kemudian:

```kotlin
user.name = "John"
```

Compose belum tentu mengetahui perubahan tersebut.

Gunakan immutable state update:

```kotlin
_uiState.update {
    it.copy(name = "John")
}
```

---

# 24. State Hoisting

State hoisting berarti memindahkan state ke caller sehingga child lebih stateless.

Sebelum:

```kotlin
@Composable
fun SearchField() {
    var query by remember {
        mutableStateOf("")
    }

    TextField(
        value = query,
        onValueChange = { query = it }
    )
}
```

Hoisted:

```kotlin
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
    )
}
```

Keuntungan:

- testable
- reusable
- single source of truth
- lebih mudah terintegrasi ViewModel

---

# 25. Container vs Stateless Screen

Pattern yang direkomendasikan:

```kotlin
@Composable
fun TransactionRoute(
    viewModel: TransactionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TransactionScreen(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onTransactionClick = viewModel::selectTransaction,
    )
}
```

Stateless UI:

```kotlin
@Composable
fun TransactionScreen(
    uiState: TransactionUiState,
    onRefresh: () -> Unit,
    onTransactionClick: (Long) -> Unit,
) {
    // UI only
}
```

CompositionLocal tidak diperlukan untuk menyembunyikan `uiState` atau ViewModel.

---

# 26. Recomposition Pitfalls

## 26.1 Melakukan operasi berat langsung di composable

Hindari:

```kotlin
@Composable
fun Screen(users: List<User>) {
    val sorted = users.sortedBy { it.name }
}
```

jika datanya besar dan recomposition sering terjadi.

Pertimbangkan:

```kotlin
val sorted = remember(users) {
    users.sortedBy { it.name }
}
```

atau lakukan transformasi di ViewModel jika memang bagian presentation state.

---

## 26.2 Network call langsung di composition

Salah:

```kotlin
@Composable
fun Screen() {
    repository.getUsers()
}
```

Karena composable dapat dipanggil ulang berkali-kali.

Gunakan ViewModel atau effect yang sesuai.

---

## 26.3 Membuat object mahal setiap recomposition

Kurang baik:

```kotlin
val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
```

Pertimbangkan:

```kotlin
val formatter = remember {
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
}
```

Jika locale perlu diperhitungkan:

```kotlin
val locale = Locale.getDefault()

val formatter = remember(locale) {
    SimpleDateFormat("dd MMM yyyy", locale)
}
```

---

# 27. Effect Decision Table

| Saya ingin... | Gunakan |
|---|---|
| Menjalankan suspend function ketika composable masuk | `LaunchedEffect` |
| Menjalankan ulang coroutine ketika ID berubah | `LaunchedEffect(id)` |
| Menjalankan coroutine dari button click | `rememberCoroutineScope` |
| Register + unregister listener | `DisposableEffect` |
| Sinkronisasi Compose state ke object eksternal | `SideEffect` |
| Menjaga callback terbaru dalam long-running effect | `rememberUpdatedState` |
| Mengubah Compose State menjadi Flow | `snapshotFlow` |
| Mengubah async source menjadi Compose State | `produceState` |
| Membuat calculated state dengan perubahan lebih jarang | `derivedStateOf` |

---

# 28. CompositionLocal Decision Table

| Pertanyaan | Jika Ya | Jika Tidak |
|---|---|---|
| Apakah value bersifat environment untuk banyak descendant? | Pertimbangkan CompositionLocal | Parameter biasa |
| Apakah value merupakan UI state screen? | Parameter / state hoisting | Lanjut evaluasi |
| Apakah hanya 1–2 child yang membutuhkan value? | Parameter biasanya lebih baik | CompositionLocal mungkin relevan |
| Apakah dependency perlu terlihat jelas pada API component? | Parameter | CompositionLocal bisa dipertimbangkan |
| Apakah value seperti theme, spacing, density, locale? | CompositionLocal sangat cocok | Evaluasi kembali |
| Apakah Anda ingin menyembunyikan ViewModel agar mudah diakses semua child? | Biasanya jangan | Gunakan route/container |

---

# 29. Cheatsheet

```text
remember
→ pertahankan value selama composition

rememberSaveable
→ pertahankan state UI + saved state

key
→ berikan identity pada composition subtree

CompositionLocalProvider
→ provide environment dependency ke descendants

compositionLocalOf
→ dynamic composition local

staticCompositionLocalOf
→ mostly-static composition local

LaunchedEffect
→ composition-driven coroutine

rememberCoroutineScope
→ event-driven coroutine

DisposableEffect
→ register + cleanup resource

SideEffect
→ push Compose state ke external object

rememberUpdatedState
→ gunakan value terbaru tanpa restart effect

derivedStateOf
→ derived state yang berubah lebih jarang

snapshotFlow
→ Compose State → Flow

produceState
→ async/external source → Compose State
```

---

# 30. Rule of Thumb untuk Project Android

Gunakan struktur berikut sebagai default:

```text
ViewModel
   ↓ StateFlow<UiState>
Route / Stateful Composable
   ↓ collectAsStateWithLifecycle()
Screen / Stateless Composable
   ↓ parameter + callback
Reusable Components
```

Contoh:

```kotlin
@Composable
fun LoanApplicationRoute(
    viewModel: LoanApplicationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LoanApplicationScreen(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onApplicationClick = viewModel::selectApplication,
    )
}
```

Environment/design-system dependency dapat menggunakan CompositionLocal:

```text
AppTheme
   ↓
CompositionLocalProvider
   ├── LocalSpacing
   ├── LocalAppShapes
   └── LocalCustomTypography
        ↓
      Screens
```

---

# 31. Anti-Pattern Checklist

Hindari pola berikut:

```text
❌ Menaruh network request langsung di composable body
❌ Menyimpan semua state menggunakan remember
❌ Menjadikan ViewModel sebagai CompositionLocal hanya agar mudah diakses
❌ Menjadikan semua screen data sebagai CompositionLocal
❌ Menggunakan derivedStateOf untuk setiap computed value
❌ Menggunakan LaunchedEffect sebagai pengganti seluruh business logic ViewModel
❌ Menggunakan LaunchedEffect dengan key yang salah sehingga effect restart terus
❌ Membuat object mahal setiap recomposition
❌ Mengubah mutable object tanpa Snapshot State / Flow
❌ Tidak memberikan stable key pada dynamic list yang mempunyai local state
```

---

# 32. Mental Model Terpenting

Jangan menganggap composable seperti function UI imperative yang hanya dipanggil sekali.

Anggap:

```text
@Composable
fun Screen(...)
```

sebagai **description of UI** yang Compose bebas jalankan kembali ketika diperlukan.

Karena itu composable sebaiknya:

- cepat
- idempotent sebisa mungkin
- tidak melakukan uncontrolled side effect
- menerima immutable state
- mengirim event melalui callback
- menggunakan effect API untuk pekerjaan yang memang bergantung pada lifecycle composition

---

## Ringkasan

Composition API pada Jetpack Compose dapat dibagi menjadi beberapa kelompok utama:

1. **State retention** — `remember`, `rememberSaveable`
2. **Identity** — `key`, lazy item keys
3. **Environment propagation** — `CompositionLocal`, `CompositionLocalProvider`
4. **Side effect management** — `LaunchedEffect`, `DisposableEffect`, `SideEffect`
5. **Coroutine integration** — `rememberCoroutineScope`
6. **State transformation** — `derivedStateOf`, `snapshotFlow`, `produceState`
7. **Architecture** — state hoisting, stateless composable, route/container pattern

Rule yang paling aman:

> **UI state dan event sebaiknya eksplisit melalui parameter. CompositionLocal digunakan terutama untuk dependency yang bersifat environment dan berlaku luas pada sebuah composition subtree.**

