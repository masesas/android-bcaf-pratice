# 07 — Common Slicing Patterns

[← Workflow Slicing](06-slicing-workflow.md) · [Index](README.md) · [Quality, Accessibility, dan Preview →](08-quality-accessibility-preview.md)

Contoh berikut adalah pola struktur, bukan design system final. Ganti spacing literal dengan token aplikasi dan pindahkan user-facing strings ke resources.

## 1. Header + content

```kotlin
@Composable
fun HeaderContentScreen(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer) {
            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}
```

Jika content dapat panjang, ganti outer `Column` dengan `LazyColumn` atau gunakan `Column.verticalScroll()` untuk content pendek yang seluruh node-nya perlu composed.

## 2. List screen

```kotlin
@Composable
fun ListScreen(
    title: String,
    items: List<ProductUi>,
    onBack: () -> Unit,
    onItemClick: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, "Kembali")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = padding.calculateTopPadding() + 16.dp,
                end = 16.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.id }) { item ->
                ProductCard(item, onClick = { onItemClick(item.id) })
            }
        }
    }
}
```

## 3. Grid screen

```kotlin
@Composable
fun GridScreen(products: List<ProductUi>, onClick: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(products, key = { it.id }) { product ->
            ProductTile(product, onClick = { onClick(product.id) })
        }
    }
}
```

## 4. Search + filter

```kotlin
@Composable
fun SearchFilterHeader(
    queryState: TextFieldState,
    activeFilterCount: Int,
    onFilterClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchField(
            state = queryState,
            onClear = { queryState.clearText() },
            modifier = Modifier.weight(1f),
        )
        BadgedBox(
            badge = { if (activeFilterCount > 0) Badge { Text(activeFilterCount.toString()) } },
        ) {
            OutlinedIconButton(onClick = onFilterClick) {
                Icon(Icons.Outlined.FilterList, "Buka filter")
            }
        }
    }
}
```

`weight(1f)` membuat search field memakai ruang tersisa tanpa mendorong tombol filter keluar layar.

## 5. Card list

```kotlin
@Composable
fun CardList(sections: List<SectionUi>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(sections, key = { it.id }) { section ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(section.title, style = MaterialTheme.typography.titleMedium)
                    Text(section.body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
```

## 6. Detail screen

```kotlin
@Composable
fun DetailScreen(
    product: ProductUi,
    onBack: () -> Unit,
    onPrimaryAction: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Default.ArrowBack, "Kembali") }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(onPrimaryAction, Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Pilih produk")
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProductSummary(product.name, product.description, product.price)
            HorizontalDivider()
            Text(product.longDescription)
        }
    }
}
```

## 7. Profile screen

```kotlin
data class ProfileUiState(
    val name: String,
    val email: String,
    val avatarUrl: String,
    val stats: List<ProfileStatUi>,
    val menu: List<ProfileMenuUi>,
)

@Composable
fun ProfileScreen(state: ProfileUiState, onEvent: (ProfileEvent) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ProfileHeader(
                name = state.name,
                subtitle = state.email,
                avatarUrl = state.avatarUrl,
                onEdit = { onEvent(ProfileEvent.Edit) },
            )
        }
        item { ProfileStats(state.stats) }
        items(state.menu, key = { it.id }) { item ->
            ProfileMenuItem(item) { onEvent(ProfileEvent.MenuSelected(item.id)) }
        }
        item {
            OutlinedButton(
                onClick = { onEvent(ProfileEvent.Logout) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Keluar") }
        }
    }
}
```

## 8. Form screen

```kotlin
@Composable
fun RegistrationForm(
    nameState: TextFieldState,
    emailState: TextFieldState,
    agreed: Boolean,
    isSubmitting: Boolean,
    errors: RegistrationErrors,
    onAgreementChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Buat akun", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            state = nameState,
            label = { Text("Nama lengkap") },
            isError = errors.name != null,
            supportingText = errors.name?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            state = emailState,
            label = { Text("Email") },
            isError = errors.email != null,
            supportingText = errors.email?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        AgreementRow(agreed, onAgreementChange)
        SubmitButton(isSubmitting, onSubmit)
    }
}
```

`imePadding()` membantu menghindari keyboard, tetapi tetap uji adjust-resize, edge-to-edge, dan scroll-to-focused-field pada konfigurasi aplikasi.

## 9. Dashboard

```kotlin
@Composable
fun DashboardScreen(state: DashboardUiState) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("Ringkasan", style = MaterialTheme.typography.headlineMedium) }
        item {
            FlowRow(
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.metrics.forEach { metric ->
                    StatisticCard(metric.label, metric.value, Modifier.weight(1f))
                }
            }
        }
        item { AppSectionTitle("Transaksi terakhir") }
        items(state.transactions, key = { it.id }) { TransactionRow(it) }
    }
}
```

Untuk expanded window, pertimbangkan `ListDetailPaneScaffold`, `SupportingPaneScaffold`, atau layout dua kolom berdasarkan window size class.

## 10. Empty state

```kotlin
@Composable
fun EmptyState(
    title: String,
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Outlined.Inbox,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
        )
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        if (actionLabel != null) Button(onClick = onAction) { Text(actionLabel) }
    }
}
```

Empty state harus menjelaskan mengapa kosong dan aksi berikutnya jika ada.

## 11. Loading state

```kotlin
@Composable
fun LoadingState(label: String = "Memuat", modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.semantics { stateDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator()
        Text(label)
    }
}
```

Untuk layout dengan bentuk stabil, skeleton dapat mengurangi layout shift. Jangan membuat animasi skeleton yang mengganggu atau mengabaikan reduced-motion policy produk.

## 12. Error state

```kotlin
@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Text("Terjadi kesalahan", style = MaterialTheme.typography.titleLarge)
        Text(message, textAlign = TextAlign.Center)
        Button(onClick = onRetry) { Text("Coba lagi") }
    }
}
```

Jangan tampilkan exception teknis kepada pengguna. Log detail secara aman dan berikan pesan yang dapat ditindaklanjuti.

## 13. Bottom navigation

```kotlin
@Composable
fun MainBottomBar(items: List<AppDestination>, selected: AppDestination, onSelect: (AppDestination) -> Unit) {
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = item == selected,
                onClick = { onSelect(item) },
                icon = {
                    BadgedBox(badge = { if (item.badgeCount > 0) Badge { Text(item.badgeCount.toString()) } }) {
                        Icon(item.icon, contentDescription = null)
                    }
                },
                label = { Text(item.label) },
            )
        }
    }
}
```

Top-level destination harus mempertahankan state/back stack sesuai architecture navigation aplikasi.

## 14. Toolbar

```kotlin
@Composable
fun SelectionToolbar(selectedCount: Int, onClose: () -> Unit, onDelete: () -> Unit) {
    TopAppBar(
        title = { Text("$selectedCount dipilih") },
        navigationIcon = {
            IconButton(onClose) { Icon(Icons.Default.Close, "Batalkan pilihan") }
        },
        actions = {
            IconButton(onDelete) { Icon(Icons.Outlined.Delete, "Hapus yang dipilih") }
        },
    )
}
```

Gunakan contextual toolbar ketika seluruh screen masuk selection mode, bukan menambahkan action kecil pada setiap item.

## 15. Floating action button

```kotlin
@Composable
fun NotesScreen(notes: List<NoteUi>, onCreate: () -> Unit) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreate,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Catatan baru") },
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(notes, key = { it.id }) { NoteRow(it) }
        }
    }
}
```

## Screen state switch yang konsisten

```kotlin
sealed interface ContentState<out T> {
    data object Loading : ContentState<Nothing>
    data class Success<T>(val data: T) : ContentState<T>
    data object Empty : ContentState<Nothing>
    data class Error(val message: String) : ContentState<Nothing>
}

@Composable
fun ProductRouteContent(state: ContentState<List<ProductUi>>, onRetry: () -> Unit) {
    when (state) {
        ContentState.Loading -> LoadingState(modifier = Modifier.fillMaxSize())
        ContentState.Empty -> EmptyState(
            title = "Belum ada produk",
            message = "Produk yang tersedia akan muncul di sini.",
            actionLabel = null,
            onAction = {},
            modifier = Modifier.fillMaxSize(),
        )
        is ContentState.Error -> ErrorState(state.message, onRetry, Modifier.fillMaxSize())
        is ContentState.Success -> ProductList(state.data, onProductClick = {})
    }
}
```

Model sealed membantu mencegah kombinasi ilegal seperti `isLoading = true`, `error != null`, dan data sukses tampil sekaligus.
