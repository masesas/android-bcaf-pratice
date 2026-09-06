# 09 — Compose UI Cheat Sheet dan Component Priority

[← Quality, Accessibility, dan Preview](08-quality-accessibility-preview.md) · [Index](README.md) · [Loan Product Detail Case Study →](10-loan-product-detail-case-study.md)

## Saya ingin membuat...

| Saya ingin membuat... | Gunakan | Catatan cepat |
|---|---|---|
| Layout horizontal | `Row` | `horizontalArrangement`, `verticalAlignment` |
| Layout vertikal | `Column` | Bukan untuk list panjang |
| Overlay/stack | `Box` | Child memakai `Modifier.align` |
| Ruang fleksibel | `Spacer(Modifier.weight(1f))` | Gap seragam gunakan `spacedBy` |
| Item wrap seperti tags | `FlowRow` | Dataset kecil/menengah |
| List panjang vertikal | `LazyColumn` | `items(key = ...)` |
| List panjang horizontal | `LazyRow` | Carousel continuous |
| Grid katalog | `LazyVerticalGrid` | `GridCells.Adaptive` untuk responsif |
| Masonry grid | `LazyVerticalStaggeredGrid` | Tinggi item berbeda |
| Swipe per halaman | `HorizontalPager` | `rememberPagerState` |
| Responsive child lokal | `BoxWithConstraints` | App-level gunakan adaptive APIs |
| Custom geometry | `Canvas` / draw modifiers | Pilihan setelah standard component tidak cukup |
| Heading/body/label | `Text` + `MaterialTheme.typography` | Hindari `fontSize` tersebar |
| Rich text | `AnnotatedString` + `SpanStyle` | Paragraph memakai `ParagraphStyle` |
| Link dalam text | `LinkAnnotation` | Jangan gunakan deprecated `ClickableText` |
| Gambar lokal | `Image(painterResource(...))` | Description `null` jika dekoratif |
| Icon | `Icon` | Action icon dibungkus `IconButton` |
| Gambar remote | Coil `AsyncImage` | Default untuk URL |
| Avatar | `AsyncImage/Image` + `clip(CircleShape)` | Biasanya `ContentScale.Crop` |
| Banner | Image + `aspectRatio` + `Crop` | Jangan fixed height tanpa adaptive check |
| Primary action | `Button` | Satu emphasis tertinggi per area |
| Secondary action | `OutlinedButton` | Cocok berdampingan dengan primary |
| Tertiary action | `TextButton` | Dialog/low emphasis |
| Significant tonal action | `FilledTonalButton` | Lebih lembut dari filled |
| Icon action | `IconButton` | Label aksesibilitas wajib |
| Floating action | FAB/Extended FAB | Satu aksi utama screen |
| Input form | `OutlinedTextField` / `TextField` | Gunakan state-based API bila dependency mendukung |
| Password | `SecureTextField` / `OutlinedSecureTextField` | API modern state-based |
| Search custom | `BasicTextField` + decorator | Standard field lebih murah dirawat |
| Agreement | `Checkbox` | Label row ikut clickable |
| Satu dari banyak pilihan | `RadioButton` | Parent `selectableGroup` bila custom group |
| Setting langsung on/off | `Switch` | Bukan untuk pilihan yang butuh submit |
| Rentang nilai | `Slider` | Number field untuk presisi |
| Rentang min–max | `RangeSlider` | Filter harga/range |
| Filter compact | `FilterChip` | Banyak filter kompleks pindah ke sheet |
| Entity/tag terpilih | `InputChip` | Sediakan remove semantics |
| Suggestion | `SuggestionChip` | Rekomendasi dinamis |
| 2–5 option switcher | `SegmentedButton` | Jangan untuk label panjang |
| Container biasa | `Box` | Hanya layout/overlay |
| Material container | `Surface` | Color/content/elevation/shape |
| Group satu subject | `Card` | Clickable overload bila seluruh card action |
| Card dengan shadow | `ElevatedCard` | Gunakan elevation bermakna |
| Card dengan outline | `OutlinedCard` | Boundary tanpa shadow |
| Root struktur screen | `Scaffold` | Pakai `innerPadding` |
| Toolbar | `TopAppBar` | Small title/action |
| Collapsing title | Medium/Large top app bar | Sambungkan `nestedScroll` |
| Bottom navigation | `NavigationBar` | 3–5 top-level destinations |
| Navigation tablet | `NavigationRail` | Medium/expanded |
| Banyak destination | Navigation drawer | Modal atau permanent |
| Navigation adaptif | `NavigationSuiteScaffold` | Bar/rail/drawer sesuai window |
| Back stack Compose-first | Navigation 3 `NavDisplay` | App memiliki back stack |
| Confirmation | `AlertDialog` | Title, message, actions |
| Custom modal | `Dialog` / `BasicAlertDialog` | Harus menyediakan inner container |
| Bottom modal | `ModalBottomSheet` | Secondary task mobile |
| Persistent bottom sheet | `BottomSheetScaffold` | Bagian dari struktur screen |
| Notification transient | Snackbar | Optional undo/action |
| Loading indeterminate | `CircularProgressIndicator` | Area kecil/centered |
| Progress determinate | `LinearProgressIndicator` | Berikan nilai progress |
| Overflow action | `DropdownMenu` | Tidak lazy untuk daftar sangat panjang |
| Select/dropdown field | `ExposedDropdownMenuBox` | Searchable picker untuk opsi banyak |
| Notification count | `BadgedBox` + `Badge` | Clamp count seperti `99+` |
| Separator | `HorizontalDivider` / `VerticalDivider` | Coba spacing dulu |
| Penjelasan icon | `TooltipBox` + `PlainTooltip` | Informasi wajib tetap visible |
| Penjelasan fitur kaya | `RichTooltip` | Jangan menggantikan onboarding kritis |
| Pull refresh | `PullToRefreshBox` | State refreshing dari state holder |
| Adaptive list-detail | `ListDetailPaneScaffold` | Beberapa pane pada expanded |
| Layout dan behavior | `Modifier` | Urutan chain berpengaruh |
| Custom clickable | `clickable` | Prefer Button jika semantik button |
| Toggle row | `toggleable` | Semantics Role sesuai control |
| Custom separator/drawing | `drawBehind` | `drawWithCache` untuk object mahal |
| Accessibility custom UI | `semantics` | Periksa merged/unmerged tree |
| UI preview | `@Preview` | Buat matrix state/width/theme/font |
| UI test | `createComposeRule` + `onNode...` | Interaksi melalui semantics |

## Tier 1 — Wajib dikuasai

API/pola yang dipakai hampir di setiap proyek:

| API/pola | Mengapa Tier 1 |
|---|---|
| `@Composable` dan recomposition | Mental model dasar seluruh Compose. |
| `Modifier` dan modifier order | Mengontrol layout, drawing, input, semantics. |
| `Row`, `Column`, `Box`, `Spacer` | Primitive mayoritas hierarchy UI. |
| Constraints, arrangement, alignment | Menentukan apakah slicing responsif atau rapuh. |
| `Text`, Typography | Hampir semua screen berisi hierarchy text. |
| `Image`, `Icon`, `painterResource` | Media lokal dan icon standard. |
| `Button`, `OutlinedButton`, `TextButton`, `IconButton` | Hierarchy action dasar. |
| `OutlinedTextField` / `TextField` | Form dan search. |
| `LazyColumn` + stable key | List production. |
| `Surface`, `Card` | Containment dan Material hierarchy. |
| `Scaffold`, `TopAppBar` | Struktur screen. |
| State hoisting | Reuse, testing, dan single source of truth. |
| `remember`, `rememberSaveable` | State lokal sesuai lifespan. |
| ViewModel + `StateFlow` + `collectAsStateWithLifecycle` | Screen state modern pada Android. |
| Semantics/content description | Accessibility dan UI testing. |

Target belajar: dapat membuat satu screen stateless lengkap, responsive, dan previewable tanpa menebak-nebak modifier.

## Tier 2 — Sering digunakan

| API/pola | Mengapa Tier 2 |
|---|---|
| `LazyRow`, `LazyVerticalGrid` | Katalog, horizontal menu, dashboard. |
| Coil `AsyncImage` | Hampir semua aplikasi networked menampilkan image. |
| Checkbox, radio, switch | Form dan settings. |
| Chips dan segmented button | Filter dan compact selection. |
| Navigation bar dan Navigation 3/Nav2 | Multi-screen application. |
| Snackbar | Feedback non-blocking. |
| `AlertDialog`, `ModalBottomSheet` | Konfirmasi dan secondary task. |
| `DropdownMenu` | Overflow/action selection. |
| Progress indicators | Loading/progress states. |
| Pager | Gallery/onboarding/tab swipe. |
| `LaunchedEffect`, `rememberCoroutineScope`, `DisposableEffect` | Menghubungkan UI lifecycle dan side effect. |
| `@Preview` dan Compose UI test | Iterasi serta regression protection. |

Target belajar: dapat menyelesaikan aplikasi multi-screen dengan state lengkap dan feedback yang benar.

## Tier 3 — Situasional

| API/pola | Kapan naik prioritas |
|---|---|
| `FlowRow` / `FlowColumn` | Produk banyak memakai chip/tag wrap. |
| Staggered grid | Feed foto/masonry. |
| Rail/drawer/adaptive suite | Tablet, foldable, desktop windowing. |
| List-detail/supporting pane | Master-detail atau productivity apps. |
| Tooltip | Mouse/keyboard/ikon kompleks. |
| Pull-to-refresh | Feed dengan refresh manual. |
| Exposed dropdown | Form dengan opsi terbatas. |
| `AnnotatedString`/`LinkAnnotation` | Legal text/rich copy/link inline. |
| `BasicTextField` | Design system input custom. |
| Custom dialog/persistent bottom sheet | Flow modal khusus. |
| `derivedStateOf`, `snapshotFlow` | Scroll reaction dan analytics berbasis Compose state. |

Target belajar: pilih berdasarkan kebutuhan produk; jangan memaksa semua API masuk setiap proyek.

## Tier 4 — Advanced/specialized

| API/pola | Risiko/biaya |
|---|---|
| `Canvas`, `drawWithContent`, `drawWithCache` | Butuh pemahaman drawing, state read, dan performance. |
| Custom `Layout` | Bertanggung jawab atas measure/place dan RTL. |
| `ConstraintLayout` | Dependency dan mental model tambahan; primitive Compose sering lebih jelas. |
| Low-level pointer input | Gesture conflicts, accessibility, cancellation. |
| Custom semantics/actions | Harus diuji dengan assistive technology. |
| `graphicsLayer` kompleks | Render layer, clipping, dan animation behavior. |
| Custom state holder/Saver | Lifespan dan restoration contract. |

Target belajar: gunakan setelah kebutuhan terbukti dan isolasi dalam primitive/component khusus.

## Lima pertanyaan sebelum memilih API

1. **Semantics:** pengguna menganggap elemen ini sebagai apa—action, selection, navigation, content, atau feedback?
2. **Cardinality:** item sedikit atau collection panjang/unknown?
3. **State:** siapa yang memiliki nilai, dan berapa lama nilai harus bertahan?
4. **Adaptation:** apa yang terjadi ketika width, font scale, text length, atau input method berubah?
5. **Abstraction:** apakah Material component sudah cocok, atau design benar-benar membutuhkan Foundation/custom implementation?

