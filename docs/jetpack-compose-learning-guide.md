# Panduan Belajar Jetpack Compose

## Insight utama

Inti Jetpack Compose bukan menghafal banyak komponen, melainkan memahami alur berikut:

```text
State -> Composable -> UI
  ^                    |
  +------- event ------+
```

UI sebaiknya menjadi hasil dari state. Ketika state berubah, Compose menjalankan *recomposition* hanya pada bagian yang membutuhkannya. Karena itu, composable idealnya cepat, idempotent, dan bebas *side effect*.

Referensi: [Thinking in Compose](https://developer.android.com/develop/ui/compose/mental-model)

## Urutan materi yang direkomendasikan

### 1. Mental model deklaratif

- Pelajari `@Composable`, Composition, recomposition, dan lifecycle composable.
- Pahami bahwa composable dapat dijalankan berkali-kali, dilewati, atau recomposition-nya dibatalkan.
- Jangan melakukan operasi berat atau mengubah state eksternal langsung di dalam proses render.

### 2. Layout dan Modifier

- Pahami constraints: parent memberikan batas ukuran, kemudian child memilih ukuran di dalam batas tersebut.
- Kuasai `Row`, `Column`, `Box`, dan `Scaffold`.
- Pelajari scope-specific modifier seperti `weight`, `align`, dan `matchParentSize`.
- Ingat bahwa urutan `Modifier` berpengaruh. Sebagai contoh, `clickable().padding()` memiliki area klik berbeda dari `padding().clickable()`.

Referensi: [Compose modifiers](https://developer.android.com/develop/ui/compose/modifiers)

### 3. State management

- Kuasai `remember`, `rememberSaveable`, dan state hoisting.
- Biasakan membuat stateless composable dan menggunakan unidirectional data flow.
- Gunakan `ViewModel`, `StateFlow`, dan `collectAsStateWithLifecycle()` untuk screen state pada Android.
- Pisahkan screen state seperti loading, content, empty, dan error secara eksplisit.

Referensi: [State in Compose](https://developer.android.com/develop/ui/compose/state)

### 4. Effect dan coroutine

- Bedakan proses render UI dengan pekerjaan yang memiliki *side effect*.
- Kuasai key pada `LaunchedEffect` karena perubahan key membatalkan coroutine sebelumnya dan menjalankan effect baru.
- Gunakan effect hanya untuk pekerjaan yang terkait dengan lifecycle UI, bukan sebagai pengganti business logic di `ViewModel`.

### 5. Reusable component dan design system

- Gunakan slot API agar komponen tetap fleksibel.
- Setiap composable UI yang reusable sebaiknya menerima `modifier: Modifier = Modifier`.
- Gunakan `MaterialTheme` untuk warna, typography, dan shape yang konsisten.
- Pisahkan route/container yang mengambil data dari content composable yang stateless.

### 6. List, navigation, dan adaptive UI

- Gunakan lazy layout untuk collection besar dan selalu pertimbangkan stable key.
- Gunakan type-safe navigation, bukan string route yang tersebar.
- Bangun layout berdasarkan ruang window yang tersedia, bukan asumsi bahwa perangkat pasti berupa ponsel atau tablet.

### 7. Testing dan accessibility

- Pelajari semantics karena digunakan bersama oleh accessibility service dan Compose UI Test.
- Uji perilaku dan state yang terlihat pengguna, bukan detail implementasi composable.
- Pastikan icon interaktif, custom component, heading, dan state memiliki semantics yang benar.

Referensi: [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)

### 8. Performance

- Pelajari performance setelah aliran state dan arsitektur UI sudah benar.
- Ukur dengan Layout Inspector, compiler report, dan benchmark sebelum melakukan optimasi.
- Hindari premature optimization menggunakan `@Stable`, `@Immutable`, atau `derivedStateOf` tanpa masalah yang sudah terukur.

Referensi: [Compose stability](https://developer.android.com/develop/ui/compose/performance/stability)

## Tabel API utama

| Area | API | Alasan menggunakan |
|---|---|---|
| Fondasi | `@Composable` | Menandai fungsi yang mendeskripsikan UI. Usahakan kecil, cepat, dan bebas side effect. |
| Layout | `Row`, `Column`, `Box` | Fondasi penyusunan elemen horizontal, vertikal, dan bertumpuk. |
| Struktur layar | `Scaffold` | Menyusun app bar, FAB, snackbar, navigation bar, dan content padding secara konsisten. |
| Layout | `Modifier` | Mengatur ukuran, posisi, drawing, input, semantics, dan interaksi tanpa membuat wrapper berlebihan. |
| Layout | `Modifier.padding`, `size`, `fillMaxWidth`, `weight` | API layout yang paling sering digunakan. `weight` bersifat scope-specific untuk `Row` atau `Column`. |
| Design system | `MaterialTheme` | Menyediakan color scheme, typography, dan shape yang konsisten di seluruh aplikasi. |
| Container visual | `Surface`, `Card` | Mengaplikasikan warna, elevation, shape, dan content color sesuai Material. |
| State lokal | `remember` | Menyimpan objek selama composable masih berada di dalam Composition. Tidak bertahan setelah activity atau process recreation. |
| State observable | `mutableStateOf` | Perubahan `value` otomatis menjadwalkan recomposition pada composable yang membaca state tersebut. |
| State UI tersimpan | `rememberSaveable` | Untuk input pengguna, selected tab, atau state UI kecil yang perlu bertahan saat rotasi dan process recreation. |
| State hoisting | `value` dan `onValueChange` | Membuat composable stateless, reusable, mudah diuji, dan memiliki single source of truth. |
| Screen state | `ViewModel` dan `StateFlow` | Menempatkan business logic dan screen state di luar composable. Cocok untuk loading, content, dan error state. |
| Flow di Android | `collectAsStateWithLifecycle()` | Mengubah Flow menjadi Compose State dengan collection yang mengikuti lifecycle Android. |
| Effect suspend | `LaunchedEffect(key)` | Menjalankan coroutine yang mengikuti lifecycle composable dan restart ketika key berubah. |
| Effect dari event | `rememberCoroutineScope()` | Menjalankan suspend function dari event seperti klik, misalnya `showSnackbar()` atau `animateScrollToItem()`. |
| Cleanup | `DisposableEffect` | Untuk listener atau observer yang harus dilepas melalui `onDispose`. |
| Callback terbaru | `rememberUpdatedState()` | Menggunakan callback atau value terbaru di dalam effect tanpa me-restart effect tersebut. |
| Sinkronisasi eksternal | `SideEffect` | Mempublikasikan Compose state kepada objek non-Compose setelah recomposition berhasil. |
| Optimasi state | `derivedStateOf` | Cocok ketika input sering berubah tetapi UI hanya perlu bereaksi pada hasil tertentu, seperti threshold posisi scroll. |
| Compose ke Flow | `snapshotFlow` | Mengubah pembacaan Compose State menjadi Flow, misalnya untuk analytics atau penggunaan operator Flow. |
| Data eksternal ke State | `produceState` | Mengadaptasi callback, subscription, atau sumber data non-Compose menjadi `State<T>`. |
| List | `LazyColumn`, `LazyRow` | Hanya melakukan compose dan layout terhadap item yang diperlukan; cocok untuk daftar panjang. |
| Grid | `LazyVerticalGrid` | Menampilkan collection berbentuk grid, termasuk kolom adaptif menggunakan `GridCells.Adaptive`. |
| Identitas item | `items(key = { it.id })` | Mempertahankan identitas dan state item ketika posisinya berubah serta menghindari pekerjaan ulang yang tidak perlu. |
| Scroll state | `rememberLazyListState()` | Mengontrol atau mengamati posisi scroll dan menjalankan scroll secara programatik. |
| Animasi sederhana | `animate*AsState` | Pilihan termudah untuk menganimasikan satu nilai berdasarkan target state. |
| Visibility | `AnimatedVisibility` | Membuat animasi enter dan exit tanpa mengelola animator secara manual. |
| Pergantian konten | `AnimatedContent` | Menganimasikan transisi antara konten berdasarkan perubahan state. |
| Animasi kompleks | `updateTransition`, `Animatable` | Untuk beberapa properti terkoordinasi atau animasi yang memerlukan kontrol `animateTo`, `snapTo`, dan cancellation. |
| Navigation 3 | `rememberNavBackStack`, `NavDisplay`, `entryProvider` | Model navigasi Compose-first yang membuat aplikasi memiliki dan mengubah back stack secara eksplisit. |
| Adaptive | `currentWindowAdaptiveInfo()` | Menentukan layout berdasarkan ruang window yang tersedia dan perubahan ukurannya saat runtime. |
| Adaptive navigation | `NavigationSuiteScaffold` | Menyesuaikan navigation bar atau navigation rail berdasarkan ukuran window. |
| Multi-pane | `ListDetailPaneScaffold` | Menghasilkan pola list-detail satu pane di layar kecil dan beberapa pane di layar besar. |
| Accessibility | `contentDescription`, `Modifier.semantics` | Memberikan makna kepada screen reader serta membuat node dapat ditemukan oleh test. |
| Testing | `createComposeRule()` | Menyediakan environment untuk menguji composable secara terisolasi. |
| Testing | `onNode...`, `performClick`, `assert...` | Mencari elemen melalui semantics, menjalankan aksi, dan memverifikasi hasil UI. |
| Preview | `@Preview`, `PreviewParameter` | Memeriksa variasi state, tema, dan ukuran layar tanpa selalu menjalankan aplikasi. |
| Interoperability | `AndroidView`, `ComposeView` | Memungkinkan migrasi bertahap antara View/XML dan Compose tanpa menulis ulang seluruh aplikasi sekaligus. |

Referensi tambahan:

- [Side-effects in Compose](https://developer.android.com/develop/ui/compose/side-effects)
- [Lazy lists and grids](https://developer.android.com/develop/ui/compose/lists)
- [Test your Compose layout](https://developer.android.com/develop/ui/compose/testing)
- [Get started with adaptive apps](https://developer.android.com/develop/adaptive-apps/guides/get-started-with-adaptive-apps)

## Pola arsitektur screen yang perlu dilatih

```kotlin
data class ProductUiState(
    val isLoading: Boolean = false,
    val products: List<ProductUi> = emptyList(),
    val errorMessage: String? = null,
)

sealed interface ProductEvent {
    data object Refresh : ProductEvent
    data class ProductClicked(val id: String) : ProductEvent
}

@Composable
fun ProductRoute(
    viewModel: ProductViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ProductScreen(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
fun ProductScreen(
    state: ProductUiState,
    onEvent: (ProductEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Stateless UI
}
```

Alasan menggunakan pola tersebut:

- `ProductRoute` menghubungkan UI dengan `ViewModel`.
- `ProductScreen` hanya merender state sehingga mudah di-preview dan diuji.
- State mengalir ke bawah, sedangkan event mengalir ke atas.
- `NavController`, repository, dan business logic tidak menyebar ke komponen UI kecil.

## Catatan Navigation terbaru

Untuk proyek baru yang sepenuhnya Compose, pelajari **Navigation 3** terlebih dahulu. Library ini menggunakan back stack berbasis state dengan `NavDisplay` dan memberikan aplikasi kontrol langsung terhadap back stack.

Referensi:

- [Navigation 3](https://developer.android.com/guide/navigation/navigation-3)
- [Navigation 3 releases](https://developer.android.com/jetpack/androidx/releases/navigation3)

Untuk aplikasi existing yang masih menggunakan Fragment, Views, atau Navigation Compose 2, type-safe route `composable<Route>()` tetap layak digunakan. Jangan melakukan migrasi hanya karena ada API yang lebih baru. Periksa kebutuhan deep link, nested navigation, modularisasi, dan konfigurasi SDK aplikasi terlebih dahulu.

Referensi: [Navigation type safety](https://developer.android.com/guide/navigation/design/type-safety)

## Urutan proyek latihan

1. **Counter atau form sederhana**: state dan recomposition.
2. **Todo list**: state hoisting dan `LazyColumn`.
3. **Aplikasi katalog**: `ViewModel`, `StateFlow`, serta loading, empty, content, dan error state.
4. **Master-detail**: Navigation 3 dan saved state.
5. **Dashboard tablet**: adaptive layout dan multi-pane.
6. **Production hardening**: UI test, accessibility, animation, lalu performance profiling.

## Lima hal terpenting

Jika hanya mengingat lima hal, prioritaskan:

1. Recomposition dan declarative UI.
2. Modifier beserta urutannya.
3. State hoisting dan unidirectional data flow.
4. Effect lifecycle dan pemilihan key yang benar.
5. Stable key pada lazy list.

Kelima hal tersebut paling sering membedakan kode Compose yang sekadar berjalan dari kode yang mudah diuji, dipelihara, dan dikembangkan.
