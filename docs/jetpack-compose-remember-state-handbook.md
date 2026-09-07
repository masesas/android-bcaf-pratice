# Jetpack Compose `remember*` State API Handbook

> Referensi praktis mengenai API `remember*` dan state holder di Jetpack Compose.  
> Research snapshot: **7 September 2026**.

---

## 1. Scope Dokumentasi

Dokumentasi ini berfokus pada API resmi AndroidX Jetpack Compose yang:

1. namanya diawali `remember...` dan membuat / mempertahankan **state holder**, atau
2. merupakan API inti yang hampir selalu dipakai bersama mekanisme state Compose.

Scope utama:

- `androidx.compose.runtime`
- `androidx.compose.runtime.saveable`
- `androidx.compose.foundation`
- `androidx.compose.foundation.lazy`
- `androidx.compose.foundation.lazy.grid`
- `androidx.compose.foundation.lazy.staggeredgrid`
- `androidx.compose.foundation.pager`
- `androidx.compose.foundation.gestures`
- `androidx.compose.foundation.text.input`
- `androidx.compose.animation`
- `androidx.compose.animation.core`
- `androidx.compose.material3`
- `androidx.compose.material3.carousel`
- `androidx.compose.material3.pulltorefresh`
- `androidx.navigation.compose`
- Material 3 Adaptive

Tidak memasukkan API spesifik Wear OS, TV, XR, atau helper memoization yang bukan state holder seperti `rememberVectorPainter`, `rememberSnapFlingBehavior`, dan sejenisnya ke tabel utama.

### Versi AndroidX saat research

| Library | Stable | Latest preview |
|---|---:|---:|
| Compose Runtime | `1.12.0` | `1.13.0-alpha02` |
| Compose Foundation | `1.12.0` | `1.13.0-alpha02` |
| Compose Animation | `1.12.0` | `1.13.0-alpha02` |
| Compose Material 3 | `1.4.0` | `1.5.0-alpha27` |
| Material 3 Adaptive | `1.3.0` | `1.4.0-alpha01` |

> Beberapa API Material 3 Expressive yang dibahas di bawah hanya tersedia pada versi preview `1.5.x`. Selalu cek versi dependency proyek sebelum menggunakannya.

---

# 2. Mental Model: Apa Sebenarnya `remember`?

Composable bisa mengalami **recomposition** berkali-kali.

Tanpa `remember`:

```kotlin
@Composable
fun Example() {
    val objectA = SomeObject()
}
```

`SomeObject()` dapat dibuat ulang saat recomposition.

Dengan `remember`:

```kotlin
@Composable
fun Example() {
    val objectA = remember {
        SomeObject()
    }
}
```

Compose menyimpan object tersebut di Composition dan mengembalikan instance yang sama selama posisi composable tersebut tetap berada di Composition.

Secara sederhana:

```text
Initial Composition
       │
       ▼
remember { createState() }
       │
       ▼
State disimpan di Composition
       │
       ├──── Recomposition ────┐
       │                       │
       ▼                       │
instance yang sama ◄───────────┘
```

Tetapi:

```text
Composable keluar dari Composition
                ↓
state dari remember dapat dilupakan
```

Karena itu `remember` bukan pengganti `ViewModel`, database, repository, atau persistent storage.

---

# 3. `remember` vs `rememberSaveable` vs ViewModel

| Mechanism | Recomposition | Configuration change | System process recreation | Cocok untuk |
|---|:---:|:---:|:---:|---|
| `remember` | ✅ | ❌ | ❌ | ephemeral UI state |
| `rememberSaveable` | ✅ | ✅ | ✅* | input/form/filter sederhana |
| `rememberSerializable` | ✅ | ✅ | ✅* | object yang dapat diserialisasi |
| `ViewModel` | ✅ | ✅ | tergantung `SavedStateHandle` | screen/business state |
| Database/DataStore | ✅ | ✅ | ✅ | persistent application data |

`*` melalui saved instance state dan bukan persistence permanen.

Contoh:

```kotlin
var expanded by remember {
    mutableStateOf(false)
}
```

Cocok untuk:

- dropdown terbuka/tutup
- dialog sementara
- animasi lokal
- UI interaction state

Untuk input user yang sebaiknya tetap ada setelah rotation:

```kotlin
var query by rememberSaveable {
    mutableStateOf("")
}
```

Untuk data screen yang berasal dari backend:

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

Jangan memindahkan state domain/backend ke `remember` hanya karena UI membutuhkannya.

---

# 4. Core Runtime API

## 4.1 Tabel API

| API | Return | Alasan digunakan | Digunakan di mana | Persist setelah rotation? |
|---|---|---|---|:---:|
| `remember` | `T` | Memoize object/value selama masih berada di Composition | Hampir semua local UI object/state | ❌ |
| `rememberSaveable` | `T` / `MutableState<T>` | Menyimpan UI state melewati recomposition dan recreation | Form, selected tab, dialog flag, query | ✅ |
| `rememberSerializable` | `T` / `MutableState<T>` | Menyimpan complex state menggunakan `KSerializer` | Custom state object `@Serializable` | ✅ |
| `rememberSaveableStateHolder` | `SaveableStateHolder` | Menyimpan state subtree yang sementara keluar dari Composition | Custom navigation/container | ✅ |
| `rememberUpdatedState` | `State<T>` | Mengakses value/callback terbaru dari long-lived effect tanpa restart effect | `LaunchedEffect`, `DisposableEffect` | ❌ |
| `rememberCoroutineScope` | `CoroutineScope` | Coroutine scope yang lifecycle-nya mengikuti Composition | Button click yang memanggil suspend UI API | ❌ |
| `rememberCompositionContext` | `CompositionContext` | Membawa context Composition ke composition lain | Framework/advanced custom composition | ❌ |

---

## 4.2 `remember`

### Gunakan ketika

Anda ingin mempertahankan object selama recomposition.

```kotlin
var expanded by remember {
    mutableStateOf(false)
}
```

Contoh object:

```kotlin
val interactionSource = remember {
    MutableInteractionSource()
}
```

### Dengan key

```kotlin
val formatter = remember(locale) {
    DateFormatter(locale)
}
```

Ketika `locale` berubah:

```text
locale berubah
    ↓
remember invalidated
    ↓
DateFormatter dibuat ulang
```

### Jangan gunakan untuk

```kotlin
val users = remember {
    mutableListOf<User>()
}
```

`MutableList` biasa bukan observable Compose state. Perubahan isi list tidak otomatis menyebabkan recomposition.

Gunakan:

```kotlin
var users by remember {
    mutableStateOf<List<User>>(emptyList())
}
```

atau:

```kotlin
val users = remember {
    mutableStateListOf<User>()
}
```

---

# 5. Primitive Compose State

API berikut bukan `remember*`, tetapi sering menjadi isi dari `remember`.

| API | Tipe | Kapan digunakan |
|---|---|---|
| `mutableStateOf()` | generic `MutableState<T>` | State generic |
| `mutableIntStateOf()` | `MutableIntState` | State `Int` tanpa boxing |
| `mutableLongStateOf()` | `MutableLongState` | State `Long` |
| `mutableFloatStateOf()` | `MutableFloatState` | State `Float` |
| `mutableDoubleStateOf()` | `MutableDoubleState` | State `Double` |
| `mutableStateListOf()` | `SnapshotStateList<T>` | Mutable list yang observable |
| `mutableStateMapOf()` | `SnapshotStateMap<K,V>` | Mutable map yang observable |

Contoh:

```kotlin
var count by remember {
    mutableIntStateOf(0)
}
```

---

# 6. `rememberSaveable`

Gunakan ketika state UI kecil perlu survive:

- rotation
- Activity recreation
- system-initiated process recreation

```kotlin
var searchQuery by rememberSaveable {
    mutableStateOf("")
}
```

Contoh selected tab:

```kotlin
var selectedTab by rememberSaveable {
    mutableIntStateOf(0)
}
```

### Custom object

Jika object compatible dengan Android `Bundle`, dapat langsung disimpan.

Untuk object custom bisa gunakan:

- `Parcelable`
- `Saver`
- `listSaver`
- `mapSaver`
- `rememberSerializable`

### Catatan penting

`rememberSaveable` tidak dimaksudkan untuk menyimpan object besar seperti:

```text
❌ Bitmap
❌ response API besar
❌ List ribuan entity
❌ Repository
❌ Database object
```

State besar sebaiknya disimpan pada ViewModel / persistence layer.

---

# 7. `rememberSerializable`

API modern untuk menyimpan state menggunakan `kotlinx.serialization`.

Contoh:

```kotlin
@Serializable
data class FilterState(
    val keyword: String,
    val category: String?,
)
```

```kotlin
val filter = rememberSerializable {
    FilterState(
        keyword = "",
        category = null,
    )
}
```

Gunakan ketika:

- object bukan tipe Bundle sederhana
- object sudah menggunakan `@Serializable`
- ingin persistence saved-state yang type-safe

Jangan gunakan untuk object domain yang sangat besar.

---

# 8. `rememberUpdatedState`

Salah satu API yang paling sering disalahpahami.

Misalnya:

```kotlin
@Composable
fun TimeoutEffect(
    onTimeout: () -> Unit,
) {
    val currentOnTimeout by rememberUpdatedState(onTimeout)

    LaunchedEffect(Unit) {
        delay(5_000)
        currentOnTimeout()
    }
}
```

Tujuannya:

```text
LaunchedEffect tidak perlu restart
          +
callback tetap menggunakan value terbaru
```

Tanpa `rememberUpdatedState`, callback yang ditangkap effect bisa menjadi stale atau Anda terpaksa menjadikan callback sebagai key dan me-restart effect.

Gunakan terutama bersama:

- `LaunchedEffect`
- `DisposableEffect`
- callback yang berubah selama recomposition

---

# 9. `rememberCoroutineScope`

Contoh Material bottom sheet:

```kotlin
val scope = rememberCoroutineScope()

Button(
    onClick = {
        scope.launch {
            sheetState.hide()
        }
    }
) {
    Text("Hide")
}
```

Gunakan saat coroutine dimulai karena **event user**:

```text
click
swipe
button
menu action
```

Jika coroutine harus otomatis berjalan ketika composable masuk Composition, lebih tepat:

```kotlin
LaunchedEffect(Unit) {
    // ...
}
```

---

# 10. Derived State

`derivedStateOf()` bukan API `remember*`, tetapi pola resminya biasanya:

```kotlin
val listState = rememberLazyListState()

val showButton by remember {
    derivedStateOf {
        listState.firstVisibleItemIndex > 0
    }
}
```

Gunakan jika source state berubah lebih sering daripada kondisi yang sebenarnya dibutuhkan UI.

Contoh:

```text
scroll offset:
0,1,2,3,4,5,6,7,8...
        ↓
derived condition:
false,false,false,... true
```

Jangan menggunakan `derivedStateOf` sekadar untuk:

```kotlin
val fullName by remember {
    derivedStateOf {
        "$firstName $lastName"
    }
}
```

jika perhitungannya murah dan tidak memberi pengurangan recomposition yang berarti.

---

# 11. Scroll State

## 11.1 `rememberScrollState`

### Return

```kotlin
ScrollState
```

### Digunakan oleh

```kotlin
Modifier.verticalScroll()
Modifier.horizontalScroll()
```

Contoh:

```kotlin
val scrollState = rememberScrollState()

Column(
    modifier = Modifier.verticalScroll(scrollState)
) {
    // content
}
```

### Berguna untuk

- mengetahui scroll offset
- programmatic scroll
- show/hide toolbar berdasarkan posisi
- scroll to top

```kotlin
scrollState.value
scrollState.maxValue
scrollState.canScrollForward
scrollState.canScrollBackward
```

Programmatic:

```kotlin
scope.launch {
    scrollState.animateScrollTo(0)
}
```

---

# 12. Lazy Layout State

## 12.1 API Table

| API | State | Digunakan pada | Alasan |
|---|---|---|---|
| `rememberLazyListState()` | `LazyListState` | `LazyColumn`, `LazyRow` | Mengontrol/mengobservasi list scroll |
| `rememberLazyGridState()` | `LazyGridState` | `LazyVerticalGrid`, `LazyHorizontalGrid` | Scroll state untuk grid |
| `rememberLazyStaggeredGridState()` | `LazyStaggeredGridState` | Staggered grid | Scroll state masonry/staggered layout |
| `rememberPagerState()` | `PagerState` | `HorizontalPager`, `VerticalPager` | Current page, paging, programmatic page change |
| `rememberOverscrollEffect()` | `OverscrollEffect?` | Lazy/scrollable component | Mengontrol overscroll effect |

---

# 13. `rememberLazyListState`

```kotlin
val listState = rememberLazyListState()

LazyColumn(
    state = listState
) {
    items(products) { product ->
        ProductItem(product)
    }
}
```

Informasi penting:

```kotlin
listState.firstVisibleItemIndex
listState.firstVisibleItemScrollOffset
listState.layoutInfo
listState.isScrollInProgress
listState.canScrollForward
listState.canScrollBackward
```

### Pagination

```kotlin
val shouldLoadMore by remember {
    derivedStateOf {
        val lastVisibleItem =
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: return@derivedStateOf false

        lastVisibleItem >=
            listState.layoutInfo.totalItemsCount - 3
    }
}
```

### Analytics scroll

Untuk event yang tidak perlu menyebabkan UI recomposition:

```kotlin
LaunchedEffect(listState) {
    snapshotFlow {
        listState.firstVisibleItemIndex
    }.collect { index ->
        // analytics
    }
}
```

---

# 14. `rememberLazyGridState`

```kotlin
val gridState = rememberLazyGridState()

LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    state = gridState,
) {
    // items
}
```

Gunakan untuk:

- scroll position
- pagination
- programmatic scroll
- restore visible grid position
- observasi layout item

---

# 15. `rememberLazyStaggeredGridState`

Untuk:

```kotlin
LazyVerticalStaggeredGrid
LazyHorizontalStaggeredGrid
```

Contoh:

```kotlin
val state = rememberLazyStaggeredGridState()

LazyVerticalStaggeredGrid(
    columns = StaggeredGridCells.Fixed(2),
    state = state,
) {
    // items
}
```

Umum digunakan untuk:

- Pinterest-style grid
- image gallery
- card dengan tinggi berbeda
- pagination masonry

---

# 16. `rememberPagerState`

```kotlin
val pagerState = rememberPagerState(
    initialPage = 0,
) {
    products.size
}
```

```kotlin
HorizontalPager(
    state = pagerState
) { page ->
    ProductPage(products[page])
}
```

Property penting:

```kotlin
pagerState.currentPage
pagerState.settledPage
pagerState.targetPage
pagerState.currentPageOffsetFraction
```

Programmatic navigation:

```kotlin
scope.launch {
    pagerState.animateScrollToPage(2)
}
```

Cocok untuk:

- onboarding
- image slider
- banners
- tab paging
- product gallery

---

# 17. Text Input State

## `rememberTextFieldState`

API state-based text field modern.

```kotlin
val emailState = rememberTextFieldState()

OutlinedTextField(
    state = emailState,
    label = {
        Text("Email")
    },
)
```

`TextFieldState` menyimpan:

```text
text
selection
cursor
composition/IME state
undo/redo state
text styles (API baru)
```

Akses:

```kotlin
emailState.text
emailState.selection
```

Edit:

```kotlin
emailState.edit {
    replace(0, length, "user@example.com")
}
```

### Kapan digunakan

Gunakan terutama dengan state-based:

```kotlin
BasicTextField(state = ...)
TextField(state = ...)
OutlinedTextField(state = ...)
BasicSecureTextField(...)
```

State-based text fields makin penting karena dapat mengelola input, selection, cursor, transformation, dan IME state tanpa harus bolak-balik merekonstruksi `String`.

---

# 18. Gesture State

## API Table

| API | Return | Digunakan dengan | Use case |
|---|---|---|---|
| `rememberDraggableState()` | `DraggableState` | `Modifier.draggable()` | Drag satu axis |
| `rememberDraggable2DState()` | `Draggable2DState` | 2D drag APIs | Drag X + Y |
| `rememberScrollableState()` | `ScrollableState` | `Modifier.scrollable()` | Custom scrolling 1 axis |
| `rememberScrollable2DState()` | `Scrollable2DState` | 2D scrolling | Canvas/viewport dua dimensi |
| `rememberTransformableState()` | `TransformableState` | `Modifier.transformable()` | Zoom + pan + rotate |

---

# 19. `rememberDraggableState`

```kotlin
var offsetX by remember {
    mutableFloatStateOf(0f)
}

val dragState = rememberDraggableState { delta ->
    offsetX += delta
}

Box(
    Modifier.draggable(
        state = dragState,
        orientation = Orientation.Horizontal,
    )
)
```

Cocok untuk:

- slider custom
- swipe panel
- resizable component
- drag interaction custom

Untuk komponen anchored/snapping, pertimbangkan `AnchoredDraggableState` daripada membangun semuanya dari nol.

---

# 20. `rememberDraggable2DState`

Gunakan ketika perubahan terjadi pada dua axis.

Konsep:

```text
drag delta
   ↓
Offset(x, y)
```

Contoh use case:

- draggable floating object
- diagram node
- canvas element
- custom map-like viewport

---

# 21. `rememberScrollableState`

Berbeda dengan `rememberScrollState`.

`rememberScrollState()`:

> State untuk container standar yang benar-benar mempunyai scroll position.

`rememberScrollableState()`:

> Low-level state untuk menerima dan mengonsumsi delta scroll secara manual.

Contoh:

```kotlin
var offset by remember {
    mutableFloatStateOf(0f)
}

val scrollableState = rememberScrollableState { delta ->
    offset += delta
    delta
}
```

Gunakan untuk custom scroll behavior.

---

# 22. `rememberScrollable2DState`

Versi dua dimensi dari `rememberScrollableState`.

Cocok untuk:

- large canvas
- diagram editor
- spreadsheet-like viewport
- custom board
- pan horizontal + vertical sekaligus

---

# 23. `rememberTransformableState`

```kotlin
var scale by remember {
    mutableFloatStateOf(1f)
}

var rotation by remember {
    mutableFloatStateOf(0f)
}

var offset by remember {
    mutableStateOf(Offset.Zero)
}

val transformState = rememberTransformableState {
        zoomChange,
        panChange,
        rotationChange ->

    scale *= zoomChange
    offset += panChange
    rotation += rotationChange
}
```

Kemudian:

```kotlin
Modifier.transformable(
    state = transformState
)
```

Cocok untuk:

- image viewer
- crop editor
- map-like UI
- diagram canvas

---

# 24. Foundation Tooltip & Overscroll

## `rememberBasicTooltipState`

Low-level Foundation tooltip state.

```kotlin
val tooltipState = rememberBasicTooltipState()
```

Dipakai bersama:

```kotlin
BasicTooltipBox(...)
```

Material 3 memiliki API yang lebih tinggi:

```kotlin
rememberTooltipState()
```

Jika aplikasi memakai Material 3, biasanya pilih Material 3 `TooltipBox`.

---

## `rememberOverscrollEffect`

```kotlin
val overscrollEffect = rememberOverscrollEffect()
```

Mengelola efek visual ketika user mencapai edge dari scrollable content.

Pada banyak komponen Lazy, effect ini sudah menjadi default parameter sehingga Anda tidak perlu membuatnya sendiri kecuali ingin:

- membagikan effect
- render effect secara custom
- mengubah behavior overscroll

---

# 25. Material 3 State API

## Tabel Utama

| API | State | Komponen utama | Alasan menggunakan |
|---|---|---|---|
| `rememberDrawerState()` | `DrawerState` | `ModalNavigationDrawer` | Open/close drawer |
| `rememberBottomSheetState()` | `SheetState` | `BottomSheet`, `ModalBottomSheet`, `BottomSheetScaffold` | Unified bottom-sheet state pada M3 terbaru |
| `rememberModalBottomSheetState()` | `SheetState` | `ModalBottomSheet` | Legacy/current stable modal sheet API |
| `rememberStandardBottomSheetState()` | `SheetState` | `BottomSheetScaffold` | Legacy standard sheet state |
| `rememberBottomSheetScaffoldState()` | `BottomSheetScaffoldState` | `BottomSheetScaffold` | State container scaffold + sheet/snackbar |
| `rememberTopAppBarState()` | `TopAppBarState` | Top app bar scroll behavior | Observe collapse/offset |
| `rememberBottomAppBarState()` | `BottomAppBarState` | Bottom app bar scroll behavior | Observe collapse/offset |
| `rememberSwipeToDismissBoxState()` | `SwipeToDismissBoxState` | `SwipeToDismissBox` | Swipe-to-dismiss progress/state |
| `rememberDatePickerState()` | `DatePickerState` | `DatePicker` | Selected date/month/mode |
| `rememberDateRangePickerState()` | `DateRangePickerState` | `DateRangePicker` | Start/end date |
| `rememberTimePickerState()` | `TimePickerState` | `TimePicker`, `TimeInput` | Hour/minute/mode |
| `rememberSliderState()` | `SliderState` | `Slider` | Value/range/steps |
| `rememberRangeSliderState()` | `RangeSliderState` | `RangeSlider` | Start/end slider values |
| `rememberTooltipState()` | `TooltipState` | `TooltipBox` | Show/dismiss tooltip |
| `rememberPullToRefreshState()` | `PullToRefreshState` | `PullToRefreshBox` | Pull distance/progress |
| `rememberCarouselState()` | `CarouselState` | Material 3 carousel | Current carousel scroll state |
| `rememberFloatingToolbarState()` | `FloatingToolbarState` | Floating toolbar | Toolbar visibility/scroll interaction |
| `rememberWideNavigationRailState()` | `WideNavigationRailState` | Wide Navigation Rail | Expanded/collapsed rail |
| `rememberSearchBarState()` | `SearchBarState` | Search bar APIs | Expanded/collapsed search surface |
| `rememberContainedSearchBarState()` | `SearchBarState` | Contained search APIs | State variant untuk contained search |
| `rememberSearchBarWithGapState()` | `SearchBarState` | Docked/gapped search | Search layout state |
| `rememberSearchBarScrollState()` | `SearchBarScrollState` | Search bar scroll behavior | Scroll-driven search bar |
| `rememberScrollFieldState()` | `ScrollFieldState` | `ScrollField` | Material expressive scroll field |
| `rememberBottomSheetScaffoldState()` | `BottomSheetScaffoldState` | `BottomSheetScaffold` | Koordinasi scaffold state |

> `rememberBottomSheetState()` adalah API unified yang diperkenalkan pada Material 3 `1.5.x` preview. Pada jalur API terbaru, `rememberModalBottomSheetState()` dan `rememberStandardBottomSheetState()` sudah diarahkan/deprecated menuju API unified ini. Jika proyek memakai stable Material 3 `1.4.0`, API yang tersedia dapat berbeda.

---

# 26. `rememberDrawerState`

```kotlin
val drawerState = rememberDrawerState(
    initialValue = DrawerValue.Closed
)

val scope = rememberCoroutineScope()

ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
        ModalDrawerSheet {
            // menu
        }
    },
) {
    Button(
        onClick = {
            scope.launch {
                drawerState.open()
            }
        }
    ) {
        Text("Menu")
    }
}
```

Gunakan untuk:

```text
open()
close()
currentValue
targetValue
isOpen
isClosed
```

---

# 27. Bottom Sheet State

## Stable/common pattern

```kotlin
val sheetState = rememberModalBottomSheetState()
```

```kotlin
ModalBottomSheet(
    sheetState = sheetState,
    onDismissRequest = {
        // ...
    },
) {
    // content
}
```

Programmatic:

```kotlin
scope.launch {
    sheetState.show()
}
```

atau:

```kotlin
scope.launch {
    sheetState.hide()
}
```

### Material 3 terbaru

Preview Material 3 `1.5.x` memperkenalkan:

```kotlin
rememberBottomSheetState()
```

untuk menyatukan state:

```text
BottomSheet
ModalBottomSheet
BottomSheetScaffold
```

Jika memulai project baru dan menggunakan versi Material 3 yang sudah mempunyai API tersebut, prefer API unified sesuai dokumentasi versi yang digunakan.

---

# 28. `rememberBottomSheetScaffoldState`

Digunakan dengan:

```kotlin
BottomSheetScaffold
```

Contoh:

```kotlin
val scaffoldState =
    rememberBottomSheetScaffoldState()

BottomSheetScaffold(
    scaffoldState = scaffoldState,
    sheetContent = {
        // ...
    },
) {
    // screen
}
```

Gunakan ketika screen memang menggunakan struktur `BottomSheetScaffold`.

---

# 29. App Bar State

## `rememberTopAppBarState`

```kotlin
val topAppBarState = rememberTopAppBarState()

val scrollBehavior =
    TopAppBarDefaults.enterAlwaysScrollBehavior(
        state = topAppBarState
    )
```

Dapat membaca:

```kotlin
topAppBarState.collapsedFraction
topAppBarState.heightOffset
topAppBarState.contentOffset
```

Cocok untuk collapsing toolbar.

---

## `rememberBottomAppBarState`

Konsep serupa, tetapi untuk bottom app bar.

```kotlin
val bottomBarState =
    rememberBottomAppBarState()
```

Gunakan ketika bottom app bar merespons scrolling content.

---

# 30. `rememberSwipeToDismissBoxState`

```kotlin
val dismissState =
    rememberSwipeToDismissBoxState()

SwipeToDismissBox(
    state = dismissState,
    backgroundContent = {
        // delete background
    },
) {
    // row
}
```

Cocok untuk:

- swipe delete
- archive
- mark complete
- dismiss notification

---

# 31. Picker State

## Date Picker

```kotlin
val datePickerState =
    rememberDatePickerState()
```

Digunakan untuk:

```kotlin
datePickerState.selectedDateMillis
datePickerState.displayedMonthMillis
datePickerState.displayMode
```

---

## Date Range Picker

```kotlin
val state =
    rememberDateRangePickerState()
```

Menyimpan:

```text
start date
end date
displayed month
display mode
```

---

## Time Picker

```kotlin
val state =
    rememberTimePickerState(
        initialHour = 9,
        initialMinute = 30,
    )
```

Akses:

```kotlin
state.hour
state.minute
state.is24hour
```

---

# 32. Slider State

## `rememberSliderState`

```kotlin
val sliderState =
    rememberSliderState(
        value = 50f,
        valueRange = 0f..100f,
    )
```

Dipakai pada state-based `Slider`.

---

## `rememberRangeSliderState`

```kotlin
val rangeState =
    rememberRangeSliderState(
        activeRangeStart = 20f,
        activeRangeEnd = 80f,
        valueRange = 0f..100f,
    )
```

Cocok untuk:

- price range
- age range
- loan tenor range
- rating filter

---

# 33. `rememberTooltipState`

```kotlin
val tooltipState =
    rememberTooltipState()

TooltipBox(
    state = tooltipState,
    positionProvider =
        TooltipDefaults.rememberPlainTooltipPositionProvider(),
    tooltip = {
        PlainTooltip {
            Text("Information")
        }
    },
    content = {
        IconButton(...) {
            // ...
        }
    },
)
```

Gunakan untuk Material tooltip yang perlu dikontrol secara programmatic.

---

# 34. `rememberPullToRefreshState`

Material 3:

```kotlin
val pullState =
    rememberPullToRefreshState()

PullToRefreshBox(
    isRefreshing = uiState.isRefreshing,
    onRefresh = onRefresh,
    state = pullState,
) {
    LazyColumn {
        // ...
    }
}
```

State ini biasanya menyimpan interaction progress seperti:

```text
pull distance
distance fraction
refresh gesture progress
```

Business state `isRefreshing` tetap sebaiknya berasal dari ViewModel.

```text
ViewModel
   ↓
isRefreshing

rememberPullToRefreshState
   ↓
gesture/UI mechanics
```

Ini contoh bagus perbedaan **business state** dan **component UI state**.

---

# 35. `rememberCarouselState`

Package:

```text
androidx.compose.material3.carousel
```

Contoh konsep:

```kotlin
val carouselState =
    rememberCarouselState {
        items.size
    }
```

Digunakan oleh Material 3 carousel seperti:

```text
HorizontalMultiBrowseCarousel
HorizontalUncontainedCarousel
HorizontalCenteredHeroCarousel
```

Gunakan untuk:

- banner
- promotional cards
- gallery
- horizontally browsable content

---

# 36. Search Bar State

Material 3 terbaru mempunyai beberapa API search state.

## `rememberSearchBarState`

General-purpose state untuk search bar.

## `rememberContainedSearchBarState`

Untuk contained/full-screen search variant tertentu.

## `rememberSearchBarWithGapState`

Untuk expanded docked search layout yang menggunakan gap.

## `rememberSearchBarScrollState`

Untuk state search bar yang bereaksi terhadap scrolling.

### Prinsip

Jangan menaruh search result di state ini.

Pisahkan:

```text
SearchBarState
   ↓
expanded/collapsed UI behavior

ViewModel UiState
   ↓
query/result/loading/error
```

---

# 37. Floating Toolbar & Navigation Rail

## `rememberFloatingToolbarState`

Dipakai untuk Material 3 floating toolbar yang memiliki visibility / scroll interaction state.

## `rememberWideNavigationRailState`

Dipakai untuk wide navigation rail, terutama adaptive desktop/tablet layout.

Contoh responsibility:

```text
WideNavigationRailState
    ↓
expanded/collapsed presentation state
```

Bukan tempat menyimpan:

```text
❌ permission
❌ selected domain data
❌ backend menu response
```

---

# 38. Navigation State

## `rememberNavController`

```kotlin
val navController =
    rememberNavController()

NavHost(
    navController = navController,
    startDestination = HomeRoute,
) {
    // graphs
}
```

`NavController` menyimpan:

- navigation back stack
- destination state
- navigation operations

Biasanya dideklarasikan di level tinggi:

```text
App
 │
 ├── rememberNavController()
 │
 └── NavHost
      ├── HomeGraph
      ├── TransactionGraph
      └── ProfileGraph
```

Jangan membuat satu `NavController` baru di setiap screen tanpa alasan karena setiap controller memiliki back stack sendiri.

---

# 39. Material 3 Adaptive State

## API Table

| API | Return | Digunakan untuk |
|---|---|---|
| `rememberNavigationSuiteScaffoldState()` | `NavigationSuiteScaffoldState` | Show/hide adaptive navigation suite |
| `rememberPaneExpansionState()` | `PaneExpansionState` | Resizable list/detail/supporting panes |
| `rememberDragToResizeState()` | `DragToResizeState` | Drag-based pane resizing |
| `rememberListDetailPaneScaffoldNavigator()` | `ThreePaneScaffoldNavigator<T>` | Navigation dalam list-detail adaptive layout |
| `rememberSupportingPaneScaffoldNavigator()` | `ThreePaneScaffoldNavigator<T>` | Navigation supporting-pane layout |
| `rememberListDetailSceneStrategy()` | `ListDetailSceneStrategy` | Navigation 3 adaptive list-detail |
| `rememberSupportingPaneSceneStrategy()` | `SupportingPaneSceneStrategy` | Navigation 3 adaptive supporting pane |

---

# 40. `rememberNavigationSuiteScaffoldState`

Digunakan pada:

```kotlin
NavigationSuiteScaffold
```

Cocok untuk aplikasi responsive:

```text
Phone
  ↓
NavigationBar

Tablet
  ↓
NavigationRail

Large screen
  ↓
Wide Navigation Rail
```

State dapat digunakan untuk mengontrol visibility navigation suite.

---

# 41. `rememberPaneExpansionState`

```kotlin
val paneExpansionState =
    rememberPaneExpansionState(...)
```

Digunakan untuk:

- list-detail layout
- tablet
- foldable
- resizable dual-pane UI

Contoh:

```text
┌─────────────┬──────────────────────┐
│ List        │ Detail               │
│             │                      │
│             │                      │
└─────────────┴──────────────────────┘
              ↑
         drag handle
```

`PaneExpansionState` menyimpan posisi pembagian pane.

---

# 42. Adaptive Pane Navigator

Contoh:

```kotlin
val navigator =
    rememberListDetailPaneScaffoldNavigator<Item>()
```

Navigator mengelola navigation state internal pada multi-pane layout.

Ini berbeda dari `NavController`.

```text
NavController
   ↓
navigation antar screen / destination besar

ThreePaneScaffoldNavigator
   ↓
navigation antar pane di adaptive scaffold
```

Keduanya bisa digunakan bersamaan.

---

# 43. Animation `remember*`

## API Table

| API | Return | Kapan digunakan |
|---|---|---|
| `rememberInfiniteTransition()` | `InfiniteTransition` | Animasi looping |
| `rememberTransition()` | `Transition<S>` | Transition state yang dikelola eksplisit |
| `rememberDeferredTransition()` | `DeferredTransition` | Advanced deferred animation transition |

---

## `rememberInfiniteTransition`

```kotlin
val infiniteTransition =
    rememberInfiniteTransition(
        label = "pulse"
    )

val alpha by infiniteTransition.animateFloat(
    initialValue = 0.5f,
    targetValue = 1f,
    animationSpec =
        infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
    label = "alpha",
)
```

Cocok untuk:

- shimmer
- pulse
- loading decoration
- repeating indicator

Jangan gunakan infinite animation jika komponen tidak terlihat atau animasi tidak diperlukan.

---

# 44. State yang TIDAK Memiliki Dedicated `rememberXState()`

Tidak semua state class memiliki helper bernama `remember...`.

Contoh penting:

## `SnackbarHostState`

Material 3 tidak memakai:

```kotlin
rememberSnackbarHostState()
```

Pola resminya:

```kotlin
val snackbarHostState =
    remember {
        SnackbarHostState()
    }
```

Kemudian:

```kotlin
Scaffold(
    snackbarHost = {
        SnackbarHost(snackbarHostState)
    }
) {
    // ...
}
```

Untuk show:

```kotlin
scope.launch {
    snackbarHostState.showSnackbar(
        message = "Saved"
    )
}
```

---

## `MutableInteractionSource`

Tidak ada kewajiban API:

```kotlin
rememberMutableInteractionSource()
```

Umumnya:

```kotlin
val interactionSource =
    remember {
        MutableInteractionSource()
    }
```

---

## `FocusRequester`

Umumnya:

```kotlin
val focusRequester =
    remember {
        FocusRequester()
    }
```

Lalu:

```kotlin
Modifier.focusRequester(focusRequester)
```

---

## `Animatable`

Umumnya:

```kotlin
val alpha =
    remember {
        Animatable(0f)
    }
```

---

# 45. Local State vs ViewModel State

Ini salah satu keputusan terpenting.

Misalnya screen:

```kotlin
@Composable
fun LoanProductRoute(
    viewModel: LoanProductViewModel,
) {
    val uiState by
        viewModel.uiState.collectAsStateWithLifecycle()

    val listState =
        rememberLazyListState()

    LoanProductScreen(
        uiState = uiState,
        listState = listState,
        onRetry = viewModel::refresh,
    )
}
```

Pembagian:

```text
ViewModel
├── products
├── loading
├── error
├── pagination
└── selected filters

Composable local state
├── lazy list scroll position
├── drawer state
├── tooltip state
├── animation state
└── transient component state
```

Rule sederhana:

> Jika state berkaitan dengan **apa yang ditampilkan**, pertimbangkan ViewModel.  
> Jika state berkaitan dengan **bagaimana komponen UI sedang berinteraksi/terposisi**, biasanya component state/`remember*`.

---

# 46. State Hoisting

Daripada:

```kotlin
@Composable
fun ProductList() {
    val listState =
        rememberLazyListState()

    LazyColumn(
        state = listState
    ) {
        // ...
    }
}
```

Anda dapat membuat composable lebih reusable:

```kotlin
@Composable
fun ProductList(
    listState: LazyListState =
        rememberLazyListState(),
) {
    LazyColumn(
        state = listState
    ) {
        // ...
    }
}
```

Parent yang tidak peduli state:

```kotlin
ProductList()
```

Parent yang ingin mengontrol:

```kotlin
val state =
    rememberLazyListState()

ProductList(
    listState = state
)
```

Ini pola state hoisting yang sangat umum di Compose.

---

# 47. Kapan State Perlu Dijadikan Parameter?

Gunakan parameter ketika parent perlu:

- membaca state
- mengontrol state
- sinkronisasi dengan komponen lain
- melakukan testing
- memasukkan fake state
- mempertahankan state pada level yang lebih tinggi

Contoh:

```kotlin
@Composable
fun MessagesList(
    listState: LazyListState =
        rememberLazyListState(),
)
```

Pattern ini memberi dua opsi:

```text
Simple caller
    ↓
gunakan default rememberLazyListState()

Advanced caller
    ↓
inject LazyListState sendiri
```

---

# 48. Jangan Memindahkan Semua State ke ViewModel

Contoh yang biasanya tidak perlu masuk ViewModel:

```text
LazyListState
PagerState
DrawerState
TooltipState
SheetState
ScrollState
TransformableState
DraggableState
```

Alasannya:

- state tersebut tightly coupled dengan Compose UI
- memiliki coroutine/animation/layout behavior
- sebagian menyimpan reference internal Compose
- ViewModel sebaiknya tidak bergantung pada UI toolkit bila tidak diperlukan

ViewModel cukup menyimpan logical state seperti:

```text
selectedProductId
searchQuery
filter
loading
error
data
page
hasMore
```

---

# 49. `rememberLazyListState` dan ViewModel Pagination

Recommended flow:

```text
LazyListState
    ↓
mendeteksi near-end
    ↓
onLoadMore()
    ↓
ViewModel.loadMore()
    ↓
repository/API
    ↓
UiState.products bertambah
    ↓
Compose recompose
```

Contoh:

```kotlin
@Composable
fun LoanProductRoute(
    viewModel: LoanProductViewModel,
) {
    val uiState by
        viewModel.uiState.collectAsStateWithLifecycle()

    val listState =
        rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo =
                listState.layoutInfo

            val lastItem =
                layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index
                    ?: return@derivedStateOf false

            lastItem >=
                layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    LazyColumn(
        state = listState
    ) {
        items(uiState.products) {
            ProductItem(it)
        }
    }
}
```

Di sini responsibility jelas:

```text
LazyListState
    =
UI position

ViewModel UiState
    =
application data
```

---

# 50. `remember` + Key

Contoh:

```kotlin
val expensiveObject =
    remember(userId) {
        ExpensiveObject(userId)
    }
```

Jika:

```text
userId = 1
```

object dibuat.

Recomposition dengan:

```text
userId = 1
```

object lama digunakan.

Ketika:

```text
userId = 2
```

object dibuat ulang.

---

# 51. Jangan Menggunakan Dynamic Key untuk Memaksa Reset Sembarangan

Bad pattern:

```kotlin
remember(System.currentTimeMillis()) {
    SomeState()
}
```

Karena key terus berubah dan manfaat `remember` hilang.

Gunakan key hanya ketika lifecycle state memang tergantung pada input tertentu.

---

# 52. State Lifetime Cheat Sheet

```text
Application / persistent
        │
        ├── Room
        ├── DataStore
        └── Repository
        │
Screen / business
        │
        ├── ViewModel
        ├── StateFlow
        └── SavedStateHandle
        │
UI Component
        │
        ├── rememberSaveable
        ├── rememberLazyListState
        ├── rememberPagerState
        ├── rememberDrawerState
        └── rememberTextFieldState
        │
Very transient
        │
        └── remember
```

---

# 53. Decision Table

| Kebutuhan | Pilihan |
|---|---|
| Boolean dialog terbuka | `rememberSaveable { mutableStateOf(false) }` bila perlu survive recreation |
| Dropdown expanded | `remember { mutableStateOf(false) }` |
| Search query screen | ViewModel atau `rememberSaveable`, tergantung ownership |
| API result | ViewModel |
| Loading API | ViewModel |
| LazyColumn position | `rememberLazyListState()` |
| Grid position | `rememberLazyGridState()` |
| Pager page | `rememberPagerState()` |
| Normal Column scroll | `rememberScrollState()` |
| Text field modern | `rememberTextFieldState()` |
| Drawer | `rememberDrawerState()` |
| Bottom sheet | `rememberBottomSheetState()` / API sesuai versi M3 |
| Date picker | `rememberDatePickerState()` |
| Time picker | `rememberTimePickerState()` |
| Pull-to-refresh gesture | `rememberPullToRefreshState()` |
| Is refreshing API | ViewModel |
| Tooltip | `rememberTooltipState()` |
| Snackbar queue | `remember { SnackbarHostState() }` |
| Infinite animation | `rememberInfiniteTransition()` |
| Gesture pan/zoom | `rememberTransformableState()` |
| Coroutine dari button click | `rememberCoroutineScope()` |
| Callback terbaru dalam effect | `rememberUpdatedState()` |
| Object custom survive recreation | `rememberSerializable()` / `rememberSaveable` + Saver |
| Navigation back stack | `rememberNavController()` |
| Adaptive list-detail navigation | `rememberListDetailPaneScaffoldNavigator()` |

---

# 54. Material 2 Legacy APIs

Jika masih menemukan project lama dengan:

```kotlin
androidx.compose.material.*
```

Anda mungkin melihat state seperti:

| API | Kegunaan | Catatan |
|---|---|---|
| `rememberScaffoldState()` | Scaffold + snackbar + drawer | Material 2; tidak ada equivalent `ScaffoldState` di M3 |
| `rememberDrawerState()` | Drawer | M2 dan M3 punya API dengan package berbeda |
| `rememberBottomSheetState()` | Standard bottom sheet | Material 2 |
| `rememberModalBottomSheetState()` | Modal bottom sheet | Material 2 |
| `rememberBottomSheetScaffoldState()` | Bottom sheet scaffold | Material 2 |
| `rememberBottomDrawerState()` | Bottom drawer | Material 2 |
| `rememberDismissState()` | Swipe-to-dismiss | M2 |
| `rememberPullRefreshState()` | Pull refresh | M2 |

Untuk project Material 3 baru, jangan otomatis mengikuti tutorial lama yang memakai package:

```kotlin
androidx.compose.material.*
```

Pastikan import berasal dari:

```kotlin
androidx.compose.material3.*
```

jika aplikasi memang menggunakan Material 3.

---

# 55. `ScaffoldState` Material 2 vs Material 3

Material 2:

```kotlin
val scaffoldState =
    rememberScaffoldState()
```

Material 3 tidak memiliki `ScaffoldState` yang sama.

Snackbar pada Material 3:

```kotlin
val snackbarHostState =
    remember {
        SnackbarHostState()
    }

Scaffold(
    snackbarHost = {
        SnackbarHost(
            hostState = snackbarHostState
        )
    }
) {
    // ...
}
```

Drawer juga dipisahkan menjadi component sendiri:

```kotlin
val drawerState =
    rememberDrawerState(
        DrawerValue.Closed
    )

ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
        // ...
    }
) {
    Scaffold {
        // ...
    }
}
```

---

# 56. `remember*` yang Bukan State Holder Utama

Compose juga mempunyai API bernama `remember...` yang tujuan utamanya bukan menyimpan application/UI state.

Contoh:

```text
rememberSnapFlingBehavior(...)
rememberSplineBasedDecay(...)
rememberPlatformOverscrollFactory(...)
TooltipDefaults.remember...PositionProvider(...)
rememberVectorPainter(...)
rememberTextMeasurer(...)
```

Fungsinya tetap memanfaatkan Composition memoization tetapi object yang dihasilkan biasanya:

- strategy
- factory
- behavior
- painter
- calculation helper
- layout helper

Jadi jangan menganggap semua function dengan prefix `remember` adalah `MutableState`.

---

# 57. Rule of Thumb

Gunakan mental model berikut:

```text
Apakah ini data bisnis?
    │
    ├── YES
    │    ↓
    │ ViewModel / domain / repository
    │
    └── NO
         ↓
Apakah ini state mekanik UI component?
         │
         ├── YES
         │    ↓
         │ rememberXState()
         │
         └── NO
              ↓
Apakah nilainya perlu survive rotation?
              │
              ├── YES
              │    ↓
              │ rememberSaveable /
              │ rememberSerializable
              │
              └── NO
                   ↓
                  remember
```

---

# 58. Recommended Screen Architecture

```kotlin
@Composable
fun HomeRoute(
    viewModel: LoanProductViewModel,
) {
    val uiState by
        viewModel.uiState
            .collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onLoadMore = viewModel::loadMore,
        onRetry = viewModel::refresh,
    )
}
```

Pure UI:

```kotlin
@Composable
fun HomeScreen(
    uiState: LoanProductUiState,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    val listState =
        rememberLazyListState()

    LazyColumn(
        state = listState
    ) {
        // render uiState
    }
}
```

Atau state di-hoist:

```kotlin
@Composable
fun HomeScreen(
    uiState: LoanProductUiState,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    listState: LazyListState =
        rememberLazyListState(),
) {
    LazyColumn(
        state = listState
    ) {
        // ...
    }
}
```

Pattern ini menjaga:

```text
ViewModel
    =
business / screen state

Route
    =
state collection + event mapping

Screen
    =
rendering UI

rememberXState
    =
component-specific UI state
```

---

# 59. Ringkasan API Berdasarkan Kategori

## Runtime

```text
remember
rememberSaveable
rememberSerializable
rememberSaveableStateHolder
rememberUpdatedState
rememberCoroutineScope
rememberCompositionContext
```

## Foundation

```text
rememberScrollState
rememberOverscrollEffect
rememberBasicTooltipState
```

## Lazy / Pager

```text
rememberLazyListState
rememberLazyGridState
rememberLazyStaggeredGridState
rememberPagerState
```

## Text Input

```text
rememberTextFieldState
```

## Gesture

```text
rememberDraggableState
rememberDraggable2DState
rememberScrollableState
rememberScrollable2DState
rememberTransformableState
```

## Material 3

```text
rememberDrawerState
rememberBottomSheetState
rememberModalBottomSheetState
rememberStandardBottomSheetState
rememberBottomSheetScaffoldState
rememberTopAppBarState
rememberBottomAppBarState
rememberSwipeToDismissBoxState
rememberDatePickerState
rememberDateRangePickerState
rememberTimePickerState
rememberSliderState
rememberRangeSliderState
rememberTooltipState
rememberPullToRefreshState
rememberCarouselState
rememberSearchBarState
rememberContainedSearchBarState
rememberSearchBarWithGapState
rememberSearchBarScrollState
rememberFloatingToolbarState
rememberWideNavigationRailState
rememberScrollFieldState
```

## Navigation

```text
rememberNavController
```

## Adaptive

```text
rememberNavigationSuiteScaffoldState
rememberPaneExpansionState
rememberDragToResizeState
rememberListDetailPaneScaffoldNavigator
rememberSupportingPaneScaffoldNavigator
rememberListDetailSceneStrategy
rememberSupportingPaneSceneStrategy
```

## Animation

```text
rememberInfiniteTransition
rememberTransition
rememberDeferredTransition
```

---

# 60. Referensi Resmi

Research dokumentasi ini menggunakan sumber resmi Android Developers / AndroidX:

- State in Compose  
  https://developer.android.com/develop/ui/compose/state

- State lifespans in Compose  
  https://developer.android.com/develop/ui/compose/state-lifespans

- State hoisting  
  https://developer.android.com/develop/ui/compose/state-hoisting

- Compose Runtime API  
  https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary

- `rememberSerializable`  
  https://developer.android.com/reference/kotlin/androidx/compose/runtime/saveable/rememberSerializable.composable

- `SaveableStateHolder`  
  https://developer.android.com/reference/kotlin/androidx/compose/runtime/saveable/SaveableStateHolder

- Foundation API  
  https://developer.android.com/reference/kotlin/androidx/compose/foundation/package-summary

- Foundation gestures  
  https://developer.android.com/reference/kotlin/androidx/compose/foundation/gestures/package-summary

- Lazy list API  
  https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/package-summary

- Lazy grid API  
  https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/grid/rememberLazyGridState.composable

- Lazy staggered grid API  
  https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/staggeredgrid/package-summary

- Pager API  
  https://developer.android.com/reference/kotlin/androidx/compose/foundation/pager/package-summary

- State-based text fields  
  https://developer.android.com/reference/kotlin/androidx/compose/foundation/text/input/package-summary

- Lazy lists guide  
  https://developer.android.com/develop/ui/compose/lists

- Material 3 API  
  https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary

- Material 3 release notes  
  https://developer.android.com/jetpack/androidx/releases/compose-material3

- Bottom sheets  
  https://developer.android.com/develop/ui/compose/components/bottom-sheets

- Pull to refresh  
  https://developer.android.com/develop/ui/compose/components/pull-to-refresh

- Material 3 Carousel API  
  https://developer.android.com/reference/kotlin/androidx/compose/material3/carousel/package-summary

- Material 3 Adaptive  
  https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive

- Adaptive layout API  
  https://developer.android.com/reference/kotlin/androidx/compose/material3/adaptive/layout/package-summary

- Adaptive navigation suite  
  https://developer.android.com/reference/kotlin/androidx/compose/material3/adaptive/navigationsuite/package-summary

- Animation Core API  
  https://developer.android.com/reference/kotlin/androidx/compose/animation/core/package-summary

- Navigation Compose  
  https://developer.android.com/develop/ui/compose/navigation

- Material 2 → Material 3 migration  
  https://developer.android.com/develop/ui/compose/designsystems/material2-material3

---

# 61. Final Cheat Sheet

```text
remember
│
├── generic local object/state
│
├── rememberSaveable
│      └── survive recreation
│
├── rememberSerializable
│      └── save serializable custom object
│
├── Scroll
│      ├── rememberScrollState
│      ├── rememberLazyListState
│      ├── rememberLazyGridState
│      ├── rememberLazyStaggeredGridState
│      └── rememberPagerState
│
├── Input
│      └── rememberTextFieldState
│
├── Gesture
│      ├── rememberDraggableState
│      ├── rememberScrollableState
│      └── rememberTransformableState
│
├── Material
│      ├── rememberDrawerState
│      ├── rememberBottomSheetState
│      ├── rememberTooltipState
│      ├── rememberDatePickerState
│      ├── rememberTimePickerState
│      ├── rememberSliderState
│      └── rememberPullToRefreshState
│
├── Navigation
│      └── rememberNavController
│
└── Effect/helper
       ├── rememberUpdatedState
       └── rememberCoroutineScope
```

---

## Kesimpulan

Prefix `remember` berarti object/value tersebut dikaitkan dengan **Composition lifecycle**, tetapi setiap API mempunyai responsibility yang berbeda.

Pemisahan paling sehat adalah:

```text
Business state
    → ViewModel / StateFlow

Persistent state
    → repository / database / DataStore

Small restorable UI state
    → rememberSaveable / rememberSerializable

Component mechanics
    → rememberLazyListState()
    → rememberPagerState()
    → rememberDrawerState()
    → rememberTextFieldState()
    → rememberXState()

Temporary local object
    → remember { ... }
```

Dengan pembagian ini, composable tetap reusable, ViewModel tidak tercemar oleh detail UI toolkit, dan state ownership menjadi lebih jelas.
