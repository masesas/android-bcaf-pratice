# 08 — Quality, Accessibility, dan Preview

[← Common Patterns](07-common-slicing-patterns.md) · [Index](README.md) · [Cheat Sheet dan Priority →](09-cheat-sheet-priority.md)

## 1. Anti-pattern saat slicing

### List besar di `Column`

```kotlin
// BAD: semua item langsung di-compose; tidak ada stable identity.
Column { products.forEach { ProductCard(it) } }

// GOOD
LazyColumn {
    items(products, key = { it.id }) { ProductCard(it) }
}
```

Gunakan `Column` bila jumlah item kecil, diketahui, dan semua memang harus hadir; masalahnya bukan `forEach` semata, melainkan kebutuhan collection/scroll.

### Menyalin nested frame Figma mentah-mentah

```kotlin
// BAD: wrapper tidak memiliki fungsi layout/semantics.
Box { Box { Column { Row { Text(title) } } } }

// GOOD
Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    Text(description, style = MaterialTheme.typography.bodyMedium)
}
```

Nested `Row`/`Column` tidak otomatis buruk untuk performance. Hapus wrapper yang tidak bermakna demi readability; jangan mengejar hierarchy flat seperti View system lama.

### Hardcoded style tersebar

```kotlin
// BAD
Text("Sukses", color = Color(0xFF0A7D35), fontSize = 14.sp)

// GOOD
Text(
    "Sukses",
    color = LocalExtendedColors.current.success,
    style = MaterialTheme.typography.labelLarge,
)
```

### Satu composable menangani semuanya

```kotlin
// BAD
@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    // collect data, transform model, navigation, 300 lines UI, dialogs, analytics...
}

// GOOD
@Composable
fun ProfileRoute(viewModel: ProfileViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(state, viewModel::onEvent)
}

@Composable
fun ProfileScreen(state: ProfileUiState, onEvent: (ProfileEvent) -> Unit) {
    // Stateless composition of ProfileHeader, ProfileStats, ProfileMenu.
}
```

### Semua state di composable

```kotlin
// BAD: business state hilang dan logic sulit diuji.
@Composable
fun Checkout() {
    var order by remember { mutableStateOf<Order?>(null) }
    // network request dan payment logic...
}

// GOOD: composable hanya membaca state dan mengirim intent.
@Composable
fun CheckoutScreen(state: CheckoutUiState, onEvent: (CheckoutEvent) -> Unit) { /* ... */ }
```

State lokal seperti menu expanded atau animation state tetap pantas berada dalam composition. Jangan memindahkan semuanya ke ViewModel tanpa alasan.

### `remember` untuk business data/persistence

```kotlin
// BAD
val customer = remember { repository.loadCustomerBlocking() }

// GOOD
val state by viewModel.uiState.collectAsStateWithLifecycle()
CustomerContent(state.customer)
```

`remember` adalah cache composition, bukan database, bukan lifecycle owner, dan bukan mekanisme background work.

### Reusable component mengabaikan Modifier

```kotlin
// BAD
@Composable
fun StatusCard() = Card(Modifier.fillMaxWidth().padding(16.dp)) { /* ... */ }

// GOOD: caller menentukan placement; component menentukan internal padding.
@Composable
fun StatusCard(modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(16.dp)) { /* ... */ } }
}
```

### Modifier order salah

```kotlin
// Padding tidak clickable.
Modifier.padding(16.dp).clickable(onClick = onClick)

// Seluruh bounds termasuk padding internal clickable.
Modifier.clickable(onClick = onClick).padding(16.dp)
```

Tidak ada satu urutan universal. Tentukan area yang ingin diukur, digambar, di-clip, dan menerima input.

### Clickable bertumpuk

```kotlin
// RISKY: card click dan child click dapat membingungkan semantics/gesture.
Card(onClick = onOpen) {
    Row {
        Text("Produk")
        Icon(Modifier.clickable(onClick = onDelete), ...)
    }
}

// BETTER: gunakan action component yang jelas dan review semantics.
Card(onClick = onOpen) {
    Row {
        Text("Produk", Modifier.weight(1f))
        IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Hapus produk") }
    }
}
```

Jika nested action masih membingungkan, jangan jadikan seluruh card clickable; sediakan action eksplisit.

### Fixed height untuk text content

```kotlin
// BAD: rawan terpotong saat localization/font scale.
Text(title, Modifier.height(24.dp))

// GOOD
Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis)
```

Gunakan fixed height hanya jika contract design memang mengharuskan viewport tertentu dan overflow sudah ditentukan.

### Komponen terlalu generic

```kotlin
// BAD
AppThing(isRed = true, isLarge = false, hasBorder = true, leftIcon = true)

// GOOD
AppBanner(
    tone = BannerTone.Error,
    leadingIcon = { Icon(Icons.Outlined.Error, null) },
)
```

Gunakan model variant yang bermakna dan slot untuk content, bukan kombinasi Boolean yang menghasilkan state ilegal.

## 2. Performance praktis

### Mental model

- Recomposition bukan redraw seluruh screen; Compose menjadwalkan scope yang membaca state berubah.
- Composable dapat dijalankan sering, dilewati, atau dibatalkan. Jangan melakukan side effect langsung dalam body.
- Optimasi harus dimulai dari trace/measurement, bukan menambahkan annotation secara acak.

### Checklist

| Area | Praktik |
|---|---|
| State reads | Baca state sedekat mungkin dengan node yang membutuhkannya. |
| Expensive calculation | Pindahkan ke state holder atau cache dengan `remember(keys)` jika murni UI calculation. |
| Lazy list | Berikan key stabil; `contentType` untuk item heterogen. |
| Collection | Gunakan model immutable/stabil; hindari mutasi list yang tidak observable. |
| Scroll-derived UI | Gunakan `derivedStateOf` hanya jika output berubah lebih jarang dari input. |
| Analytics dari state | Gunakan `snapshotFlow` dalam `LaunchedEffect`. |
| Animation | Hindari state read pada composition bila dapat ditunda ke draw/layout lambda. |
| Images | Load sesuai constraints; default ke Coil `AsyncImage`. |
| Verification | Layout Inspector, compiler reports, Macrobenchmark/release build. |

### `remember`, `rememberSaveable`, dan `derivedStateOf`

```kotlin
// Cache hasil kalkulasi selama products/filter tidak berubah.
val visibleProducts = remember(products, filter) {
    products.filter { it.matches(filter) }
}

// State UI kecil yang perlu dipulihkan.
var selectedTab by rememberSaveable { mutableIntStateOf(0) }

// Tepat: scroll offset/index berubah sering, Boolean threshold jarang berubah.
val listState = rememberLazyListState()
val showScrollToTop by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 0 }
}
```

Jangan memakai `derivedStateOf` untuk `"$firstName $lastName"`; hasil itu berubah sesering input sehingga hanya menambah overhead.

### Stable parameters

Utamakan data immutable. `@Immutable`/`@Stable` adalah kontrak kepada compiler, bukan solusi otomatis. Annotation yang salah dapat menyebabkan UI tidak update sebagaimana diharapkan.

```kotlin
@Immutable
data class ProductUi(
    val id: String,
    val name: String,
    val formattedPrice: String,
)
```

Strong skipping aktif secara default pada Kotlin modern tertentu, tetapi identitas object dan stability tetap penting untuk reasoning. Lihat [Stability](https://developer.android.com/develop/ui/compose/performance/stability) dan [Performance best practices](https://developer.android.com/develop/ui/compose/performance/bestpractices).

## 3. Accessibility

### Checklist minimum

- Interactive touch target minimal **48dp × 48dp** dan area target tidak saling overlap.
- Icon/gambar informatif memiliki localized `contentDescription`; dekoratif memakai `null`.
- Gunakan `Button`, `Checkbox`, `RadioButton`, `Switch`, dan high-level APIs agar semantics default benar.
- Label harus menjelaskan tujuan: “Bagikan produk”, bukan “Ikon share”.
- Selected/checked/expanded/error/loading state dapat dipahami tanpa mengandalkan warna.
- Contrast memenuhi design standard yang digunakan.
- Dynamic type/font scale tidak memotong informasi/aksi.
- Traversal order logis untuk TalkBack, keyboard, dan switch access.
- Custom component memberikan `role`, state, dan action melalui semantics.

Referensi: [Accessibility API defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults) dan [Semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics).

### Informative vs decorative image

```kotlin
// Informative
Image(painterResource(R.drawable.branch_map), contentDescription = "Peta lokasi cabang Sudirman")

// Decorative: text di sebelahnya sudah membawa informasi yang sama.
Icon(Icons.Outlined.CheckCircle, contentDescription = null)
Text("Pengajuan berhasil")
```

### Custom semantics

```kotlin
@Composable
fun ExpandableSummary(expanded: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                stateDescription = if (expanded) "Diperluas" else "Diciutkan"
                onClick(label = if (expanded) "Ciutkan" else "Perluas") {
                    onToggle()
                    true
                }
            }
            .clickable(onClick = onToggle)
            .padding(16.dp),
    ) {
        Text("Detail biaya", Modifier.weight(1f))
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
        )
    }
}
```

Hindari menggandakan action semantics dari `clickable` dan manual `onClick` tanpa mengecek hasil semantics tree. Pada custom control, pilih satu strategi yang menghasilkan announcement dan action yang benar.

### Touch target

```kotlin
Icon(
    imageVector = Icons.Default.Close,
    contentDescription = "Tutup",
    modifier = Modifier
        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
        .clickable(onClick = onClose)
        .padding(12.dp),
)

// Lebih baik untuk aksi icon standar:
IconButton(onClick = onClose) {
    Icon(Icons.Default.Close, contentDescription = "Tutup")
}
```

## 4. Preview handbook

Preview penting saat slicing karena memperpendek feedback loop dan memaksa component dapat dirender dari parameter, tanpa Activity/ViewModel nyata.

### Preview state dasar

```kotlin
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProductCardPreview() {
    AppTheme(darkTheme = isSystemInDarkTheme()) {
        ProductCard(
            product = ProductUi.preview(),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
```

### Device dan font scale

```kotlin
@Preview(name = "Compact", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Expanded", widthDp = 1000, heightDp = 700, showBackground = true)
@Preview(name = "Font 200%", widthDp = 360, fontScale = 2f, showBackground = true)
@Composable
private fun LoanScreenPreview() {
    AppTheme(darkTheme = false) {
        LoanProductDetailScreen(LoanDetailUiState.preview(), onEvent = {})
    }
}
```

### Preview parameter

```kotlin
class ProductStateProvider : PreviewParameterProvider<ContentState<List<ProductUi>>> {
    override val values = sequenceOf(
        ContentState.Loading,
        ContentState.Empty,
        ContentState.Error("Koneksi terputus"),
        ContentState.Success(listOf(ProductUi.preview())),
    )
}

@Preview(showBackground = true)
@Composable
private fun ProductStatePreview(
    @PreviewParameter(ProductStateProvider::class) state: ContentState<List<ProductUi>>,
) {
    AppTheme(false) { ProductRouteContent(state, onRetry = {}) }
}
```

Preview matrix minimum untuk screen penting:

| Dimension | Cases |
|---|---|
| Theme | Light, dark |
| Width | Compact, medium, expanded |
| Font | 1.0, 1.3, 2.0 |
| Content | Empty, normal, long/localized |
| Network | Loading, success, error/offline |
| Interaction | Enabled, disabled, selected, focused/error |

## 5. Compose UI test

Test berinteraksi dengan semantics tree. Prefer matcher berdasarkan visible text/role/content description; gunakan `testTag` jika tidak ada semantic identity alami.

```kotlin
class LoanDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun applyButton_sendsApplyEvent() {
        var applied = false
        composeRule.setContent {
            AppTheme(false) {
                LoanProductDetailScreen(
                    state = LoanDetailUiState.preview(),
                    onEvent = { if (it == LoanDetailEvent.Apply) applied = true },
                )
            }
        }

        composeRule.onNodeWithText("Ajukan sekarang")
            .assertIsEnabled()
            .performClick()

        assertThat(applied).isTrue()
    }
}
```

Uji behavior/state penting, bukan seluruh detail implementasi. Referensi: [Compose testing](https://developer.android.com/develop/ui/compose/testing).

## 6. Recommended project structure

```text
ui/
├── components/
│   ├── action/
│   │   ├── AppPrimaryButton.kt
│   │   └── AppIconButton.kt
│   ├── input/
│   │   ├── AppTextField.kt
│   │   └── AppSecureTextField.kt
│   ├── card/
│   │   └── LoanCard.kt
│   ├── feedback/
│   │   ├── EmptyState.kt
│   │   └── ErrorState.kt
│   └── dialog/
│       └── ConfirmationDialog.kt
├── theme/
│   ├── Color.kt
│   ├── Type.kt
│   ├── Shape.kt
│   └── Theme.kt
├── design/
│   ├── Spacing.kt
│   ├── Dimension.kt
│   └── ExtendedColors.kt
├── navigation/
│   ├── AppNavKey.kt
│   └── AppNavigation.kt
└── screens/
    ├── home/
    │   ├── HomeRoute.kt
    │   ├── HomeScreen.kt
    │   ├── HomeUiState.kt
    │   └── HomeViewModel.kt
    ├── profile/
    └── login/
```

### Tanggung jawab

| Folder/file | Tanggung jawab |
|---|---|
| `components/` | Reusable cross-feature components; jangan menjadi tempat semua private screen component. |
| `theme/` | Material theme, color scheme, typography, shapes. |
| `design/` | Token tambahan yang tidak ditampung langsung oleh Material theme. |
| `navigation/` | Top-level navigation state, keys, entry provider, app shell. |
| `screens/<feature>/Route` | Integrasi ViewModel/navigation/platform contracts. |
| `screens/<feature>/Screen` | Stateless screen content. |
| `UiState` | Immutable data yang benar-benar dibutuhkan render. |
| `ViewModel` | Business event handling dan production of screen state. |

Untuk proyek modular, komponen feature-specific tetap di feature module. Hanya promosikan ke shared `components` setelah contract reuse terbukti.

## 7. Definition of done slicing

- [ ] Semua state Figma dan edge cases terimplementasi.
- [ ] Preview matrix lulus visual review.
- [ ] Screen tidak bergantung langsung pada repository/service.
- [ ] User input, scroll, dan navigation state pulih sesuai requirement.
- [ ] UI test mencakup primary flow dan critical errors.
- [ ] Semantics tree dan TalkBack flow masuk akal.
- [ ] Touch target, contrast, dark theme, font scale, RTL, dan keyboard diperiksa.
- [ ] Lazy items memiliki stable key.
- [ ] Tidak ada hardcoded token berulang.
- [ ] Performance diukur bila screen kompleks; tidak ada optimasi spekulatif yang membingungkan.

