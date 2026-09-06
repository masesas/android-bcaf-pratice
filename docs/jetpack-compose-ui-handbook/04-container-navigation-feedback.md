# 04 — Container, Navigation, dan Feedback

[← Actions, Form, dan Selection](03-actions-form-selection.md) · [Index](README.md) · [Modifier dan Design System →](05-modifier-design-system.md)

## 1. Card, Surface, dan Box

| Kebutuhan | Gunakan | Alasan |
|---|---|---|
| Positioning/overlay tanpa semantik Material surface | `Box` | Layout primitive paling sederhana. |
| Container mengikuti color, content color, shape, border, tonal/shadow elevation | `Surface` | Membawa konsep surface Material. |
| Group konten dan aksi tentang satu subjek | `Card` | Pola card dan clickable overload siap pakai. |
| Card terpisah dengan shadow | `ElevatedCard` | Elevation memberi hierarchy. |
| Card dengan boundary datar | `OutlinedCard` | Border tanpa shadow. |

Jangan memakai `Card` untuk setiap section. Sering kali spacing, heading, atau perubahan surface color sudah cukup mengelompokkan konten.

### Product card

```kotlin
@Composable
fun ProductCard(
    product: ProductUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(88.dp).clip(MaterialTheme.shapes.medium),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text(product.description, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(product.price, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
```

### Profile, loan summary, dan statistic card

```kotlin
@Composable
fun ProfileCard(name: String, email: String, onEdit: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(email, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Edit profil") }
        }
    }
}

@Composable
fun LoanSummaryCard(amount: String, tenor: String, installment: String) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ringkasan simulasi", style = MaterialTheme.typography.titleMedium)
            SummaryRow("Plafond", amount)
            SummaryRow("Tenor", tenor)
            HorizontalDivider()
            SummaryRow("Estimasi cicilan", installment, emphasize = true)
        }
    }
}

@Composable
fun StatisticCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
```

## 2. Visual navigation vs navigation engine

Komponen berikut hanya menggambar kontrol navigasi:

- `NavigationBar` / `NavigationBarItem`
- `NavigationRail` / `NavigationRailItem`
- `ModalNavigationDrawer`, `PermanentNavigationDrawer`, `NavigationDrawerItem`
- `NavigationSuiteScaffold`

Komponen tersebut tidak menggantikan navigation engine. Navigation engine seperti Navigation 3 menyimpan back stack, memetakan key ke destination, dan menangani back.

### Pemilihan visual navigation

| Window/requirement | Komponen |
|---|---|
| Compact, 3–5 destination utama | `NavigationBar` |
| Medium atau expanded | `NavigationRail` |
| Banyak destination dan compact/medium | `ModalNavigationDrawer` |
| Expanded dan drawer selalu terlihat | `PermanentNavigationDrawer` |
| Harus berubah otomatis sesuai window | `NavigationSuiteScaffold` |

```kotlin
@Composable
fun AppNavigationBar(
    items: List<AppDestination>,
    selected: AppDestination,
    onSelect: (AppDestination) -> Unit,
) {
    NavigationBar {
        items.forEach { destination ->
            NavigationBarItem(
                selected = destination == selected,
                onClick = { onSelect(destination) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.label) },
            )
        }
    }
}

@Composable
fun AppNavigationRail(items: List<AppDestination>, selected: AppDestination, onSelect: (AppDestination) -> Unit) {
    NavigationRail {
        items.forEach { destination ->
            NavigationRailItem(
                selected = destination == selected,
                onClick = { onSelect(destination) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.label) },
            )
        }
    }
}
```

### Drawer

```kotlin
@Composable
fun DrawerLayout(selected: AppDestination, onSelect: (AppDestination) -> Unit, content: @Composable () -> Unit) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("BCAF", Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                AppDestination.entries.forEach { destination ->
                    NavigationDrawerItem(
                        selected = destination == selected,
                        onClick = {
                            onSelect(destination)
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
        content = content,
    )
}
```

Untuk expanded window, ganti `ModalNavigationDrawer` + `ModalDrawerSheet` dengan `PermanentNavigationDrawer` + `PermanentDrawerSheet`.

```kotlin
@Composable
fun PermanentDrawerLayout(
    selected: AppDestination,
    onSelect: (AppDestination) -> Unit,
    content: @Composable () -> Unit,
) {
    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet {
                AppDestination.entries.forEach { destination ->
                    NavigationDrawerItem(
                        selected = destination == selected,
                        onClick = { onSelect(destination) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
        content = content,
    )
}
```

“Navigation drawer” adalah nama pola. API konkret Material 3 adalah `ModalNavigationDrawer` atau `PermanentNavigationDrawer` beserta sheet/item-nya.

### Adaptive navigation

```kotlin
@Composable
fun AdaptiveAppShell(
    destinations: List<AppDestination>,
    selected: AppDestination,
    onSelect: (AppDestination) -> Unit,
    content: @Composable () -> Unit,
) {
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            destinations.forEach { destination ->
                item(
                    selected = destination == selected,
                    onClick = { onSelect(destination) },
                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                    label = { Text(destination.label) },
                )
            }
        },
        content = content,
    )
}
```

`NavigationSuiteScaffold` secara default dapat memilih bar atau rail berdasarkan adaptive info. Untuk keputusan khusus, hitung `layoutType` dari `currentWindowAdaptiveInfo()`/`currentWindowAdaptiveInfoV2()` sesuai versi library.

Referensi: [Adaptive navigation](https://developer.android.com/develop/adaptive-apps/guides/build-adaptive-navigation).

### Navigation 3 secara ringkas

```kotlin
@Serializable data object Home : NavKey
@Serializable data class ProductDetail(val id: String) : NavKey

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(Home)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Home> {
                HomeScreen(onProductClick = { id -> backStack.add(ProductDetail(id)) })
            }
            entry<ProductDetail> { key ->
                ProductDetailRoute(productId = key.id, onBack = { backStack.removeLastOrNull() })
            }
        },
    )
}
```

Pass navigation callbacks ke screen; jangan meneruskan back stack/controller ke komponen presentational kecil. Lihat [Navigation 3 basics](https://developer.android.com/guide/navigation/navigation-3/basics).

## 3. Top app bar

| API | Use case |
|---|---|
| `TopAppBar` | Subscreen/title ringkas. |
| `CenterAlignedTopAppBar` | Title pendek dan simetris. |
| `MediumTopAppBar` | Destination dengan hierarchy sedang dan collapsing title. |
| `LargeTopAppBar` | Destination utama yang memerlukan title besar. |

Semua varian memiliki slot `title`, `navigationIcon`, `actions`, serta dukungan `scrollBehavior`.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsingProductScreen(content: @Composable (PaddingValues) -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Produk pinjaman") },
                navigationIcon = {
                    IconButton(onClick = { /* back */ }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { /* share */ }) {
                        Icon(Icons.Outlined.Share, "Bagikan")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        content = content,
    )
}
```

Pilihan scroll behavior:

- `pinnedScrollBehavior()`: tetap terlihat.
- `enterAlwaysScrollBehavior()`: collapse saat scroll up dan muncul segera saat scroll down.
- `exitUntilCollapsedScrollBehavior()`: expand kembali setelah konten kembali ke awal.

Hubungkan `nestedScrollConnection` pada root yang menerima nested scroll. Referensi: [App bars](https://developer.android.com/develop/ui/compose/components/app-bars).

### Varian app bar minimal

```kotlin
@Composable
fun AppBarVariants() {
    Column {
        TopAppBar(title = { Text("Small") })
        CenterAlignedTopAppBar(title = { Text("Centered") })
        MediumTopAppBar(title = { Text("Medium") })
        LargeTopAppBar(title = { Text("Large") })
    }
}
```

Contoh gallery ini hanya menunjukkan signature. Dalam screen nyata, app bar ditempatkan pada slot `Scaffold.topBar`, bukan ditumpuk dalam satu `Column`.

## 4. Scaffold

`Scaffold` sering menjadi root screen karena mengoordinasikan komponen Material utama dan memberikan `PaddingValues` agar content tidak tertutup bar.

```kotlin
@Composable
fun CompleteScreen(
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar produk") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, "Kembali")
                    }
                },
            )
        },
        bottomBar = { AppBottomActions() },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, "Tambah produk")
            }
        },
    ) { innerPadding ->
        ProductList(
            products = emptyList(),
            onProductClick = {},
            modifier = Modifier.padding(innerPadding),
        )
    }
}
```

Kesalahan umum adalah mengabaikan `innerPadding`, lalu content berada di bawah top/bottom bar. Terapkan padding ke root scroll container; untuk lazy list, `contentPadding` sering lebih tepat agar overscroll dan positioning item tetap natural.

## 5. Dialog dan bottom sheet

### Decision table

| UX requirement | Component |
|---|---|
| Konfirmasi singkat dengan title/text/actions | `AlertDialog` |
| Dialog Material dengan layout custom | `BasicAlertDialog` |
| Modal custom penuh atau ukuran khusus | `Dialog` |
| Task/options sekunder pada mobile | `ModalBottomSheet` |
| Sheet persisten yang menjadi bagian screen | `BottomSheetScaffold` |
| Flow panjang/kompleks | Screen penuh, bukan dialog/sheet |

```kotlin
@Composable
fun DeleteConfirmation(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
        title = { Text("Hapus data?") },
        text = { Text("Tindakan ini tidak dapat dibatalkan.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Hapus", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
fun CustomDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Pilih tenor", style = MaterialTheme.typography.headlineSmall)
                TenorOptions(listOf(12, 24, 36), 12) {}
                Button(onClick = onDismiss, Modifier.fillMaxWidth()) { Text("Terapkan") }
            }
        }
    }
}
```

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicMaterialDialog(onDismiss: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Perhatian", style = MaterialTheme.typography.headlineSmall)
                Text("Konten custom tetap menyediakan container dan action yang jelas.")
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Tutup")
                }
            }
        }
    }
}
```

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (visible) {
        ModalBottomSheet(onDismissRequest = onDismiss, content = content)
    }
}
```

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersistentSheetExample() {
    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 80.dp,
        sheetContent = {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Ringkasan", style = MaterialTheme.typography.titleLarge)
                Text("Sheet ini selalu menjadi bagian dari screen.")
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            Text("Konten utama", Modifier.align(Alignment.Center))
        }
    }
}
```

Ketika `SheetState.hide()` selesai, keluarkan `ModalBottomSheet` dari composition. Referensi: [Bottom sheet](https://developer.android.com/develop/ui/compose/quick-guides/content/create-bottom-sheet) dan [Dialog](https://developer.android.com/develop/ui/compose/components/dialog).

## 6. Snackbar dan progress

### Feedback decision table

| Kondisi | Gunakan |
|---|---|
| Pesan transient non-blocking | Snackbar |
| Undo setelah aksi | Snackbar dengan action |
| Error field | Supporting/inline error |
| Error yang memblokir flow dan perlu keputusan | Dialog atau error screen |
| Loading seluruh area, durasi unknown | Circular indicator atau skeleton sesuai design |
| Progress dapat dihitung | Determinate linear/circular indicator |
| Loading dalam button | Indicator kecil di dalam button + disabled submit |

```kotlin
@Composable
fun SaveWithSnackbar(snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    Button(onClick = {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Perubahan disimpan",
                actionLabel = "Urungkan",
                withDismissAction = true,
            )
            if (result == SnackbarResult.ActionPerformed) {
                // Request undo through state holder.
            }
        }
    }) { Text("Simpan") }
}

@Composable
fun LoadingExamples(progress: Float?) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (progress == null) CircularProgressIndicator()
        else LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth())
    }
}
```

Jangan mengirim event Snackbar berulang hanya karena screen recomposition; event harus dikonsumsi melalui alur event yang terkontrol.

## 7. Menu

```kotlin
@Composable
fun OverflowMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, "Menu lainnya")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Edit") },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                onClick = { expanded = false; onEdit() },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Hapus") },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                onClick = { expanded = false; onDelete() },
            )
        }
    }
}
```

`DropdownMenu` tidak lazily compose item. Untuk ratusan opsi atau pencarian, gunakan dialog/sheet/searchable picker. Referensi: [Menus](https://developer.android.com/develop/ui/compose/components/menu).

### Exposed dropdown

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySelector(cities: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Kota") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            cities.forEach { city ->
                DropdownMenuItem(text = { Text(city) }, onClick = {
                    onSelect(city)
                    expanded = false
                })
            }
        }
    }
}
```

## 8. Badge dan divider

```kotlin
@Composable
fun CartIcon(count: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (count > 0) Badge { Text(if (count > 99) "99+" else count.toString()) }
            }
        ) {
            Icon(Icons.Outlined.ShoppingCart, contentDescription = "Keranjang, $count item")
        }
    }
}

@Composable
fun DividedRow() {
    Row(Modifier.height(IntrinsicSize.Min)) {
        MetricContent("Bunga", "8,5%")
        VerticalDivider(Modifier.padding(horizontal = 16.dp))
        MetricContent("Tenor", "36 bulan")
    }
}
```

Gunakan divider untuk boundary yang bermakna. Jika hierarchy sudah jelas, spacing biasanya lebih tenang dan fleksibel.

## 9. Tooltip

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoTooltip() {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        state = rememberTooltipState(),
        tooltip = { PlainTooltip { Text("Bunga efektif per tahun") } },
    ) {
        IconButton(onClick = {}) {
            Icon(Icons.Outlined.Info, contentDescription = "Info bunga")
        }
    }
}
```

Gunakan `PlainTooltip` untuk label singkat dan `RichTooltip` untuk penjelasan yang lebih lengkap. Informasi wajib tidak boleh hanya berada di tooltip karena tooltip bergantung pada hover/long press. Referensi: [Tooltips](https://developer.android.com/develop/ui/compose/components/tooltip).

## 10. Pull to refresh

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableProductList(
    products: List<ProductUi>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh) {
        LazyColumn(Modifier.fillMaxSize()) {
            items(products, key = { it.id }) { ProductCard(it) }
        }
    }
}
```

`isRefreshing` berasal dari state holder. `onRefresh` meminta refresh; jangan menghentikan indicator hanya berdasarkan timer lokal. Referensi: [Pull to refresh](https://developer.android.com/develop/ui/compose/components/pull-to-refresh).
