# 02 — Layout, Text, dan Image

[← API Overview](01-api-overview.md) · [Index](README.md) · [Actions, Form, dan Selection →](03-actions-form-selection.md)

## 1. Layout components

### Decision table

| Kebutuhan | Gunakan | Hindari/alternatif |
|---|---|---|
| Susunan horizontal pendek | `Row` | Gunakan `FlowRow` jika dapat wrap; `LazyRow` jika panjang. |
| Susunan vertikal pendek | `Column` | Gunakan `LazyColumn` jika panjang/unknown. |
| Overlay atau alignment dalam satu area | `Box` | Jangan membuat `Box` jika tidak ada kebutuhan stack/alignment. |
| Gap seragam antarsibling | `Arrangement.spacedBy()` | `Spacer` cocok jika jarak hanya muncul pada titik tertentu. |
| Chip/tag wrap | `FlowRow` | Untuk ratusan item gunakan lazy grid/list. |
| Relasi sibling kompleks | Primitive dahulu; `ConstraintLayout` bila lebih jelas | Flat hierarchy bukan alasan performance di Compose. |
| Keputusan child berdasarkan lebar lokal | `BoxWithConstraints` | Keputusan aplikasi memakai window size class/adaptive scaffold. |
| Daftar panjang | `LazyColumn` / `LazyRow` | Jangan memakai `Column.verticalScroll()` untuk ratusan item. |
| Grid seragam | `LazyVerticalGrid` | Staggered grid untuk tinggi berbeda. |
| Konten per halaman dengan snap | Pager | Lazy list untuk continuous scrolling. |

### `Row`

**Fungsi:** mengukur dan menempatkan child secara horizontal.

**Gunakan untuk:** toolbar content, price + badge, avatar + identity, tombol berdampingan. `horizontalArrangement` mengatur sumbu utama; `verticalAlignment` mengatur sumbu silang.

**Hindari ketika:** item perlu wrap (`FlowRow`) atau daftar horizontal panjang (`LazyRow`).

```kotlin
@Composable
fun ProfileIdentity(
    name: String,
    role: String,
    avatarUrl: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(CircleShape),
        )
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(role, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = { /* open edit */ }) {
            Icon(Icons.Outlined.Edit, contentDescription = "Edit profil")
        }
    }
}
```

### `Column`

**Fungsi:** menempatkan child secara vertikal.

**Gunakan untuk:** form pendek, detail card, empty state, section content.

**Hindari ketika:** jumlah item besar/unknown; gunakan `LazyColumn`.

```kotlin
@Composable
fun ProductSummary(
    title: String,
    description: String,
    price: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(description, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = price,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
```

### `Box`

**Fungsi:** menumpuk child dan mengatur alignment relatif terhadap area yang sama.

**Gunakan untuk:** badge di atas icon, gradient di atas gambar, loading overlay, empty overlay.

**Hindari ketika:** yang dibutuhkan hanya urutan horizontal/vertikal.

```kotlin
@Composable
fun ProductThumbnail(url: String, discount: String, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(4f / 3f).clip(MaterialTheme.shapes.medium)) {
        AsyncImage(
            model = url,
            contentDescription = "Foto produk",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) {
            Text(discount, Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}
```

### `Spacer`

**Fungsi:** membuat ruang eksplisit atau menyerap ruang dengan `weight`.

**Gunakan untuk:** mendorong action ke ujung atau jarak yang hanya muncul pada satu titik.

```kotlin
Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text("Total", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.weight(1f))
    Text("Rp12.500.000", style = MaterialTheme.typography.titleMedium)
}
```

Untuk gap seragam, lebih ringkas menggunakan `Arrangement.spacedBy(AppSpacing.sm)`.

### `FlowRow` dan `FlowColumn`

**Fungsi:** seperti `Row`/`Column`, tetapi pindah ke baris/kolom baru saat ruang habis.

**Gunakan untuk:** chip filters, tags, responsive action group. **Jangan gunakan** sebagai pengganti lazy grid untuk dataset besar.

```kotlin
@Composable
fun CategoryFilters(
    categories: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            FilterChip(
                selected = category in selected,
                onClick = { onToggle(category) },
                label = { Text(category) },
            )
        }
    }
}

@Composable
fun VerticalTagFlow(tags: List<String>) {
    FlowColumn(
        modifier = Modifier.height(160.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
    }
}
```

### `ConstraintLayout`

**Fungsi:** menempatkan child berdasarkan relasi sibling, guideline, barrier, atau chain.

**Gunakan jika:** layout relasional benar-benar lebih mudah dibaca sebagai constraints, atau perlu mengganti `ConstraintSet`. **Jangan memilihnya hanya agar hierarchy flat**; keuntungan itu berasal dari View system dan tidak relevan dengan cara yang sama di Compose.

```kotlin
@Composable
fun PriceWithCornerBadge(price: String, badge: String) {
    ConstraintLayout(Modifier.fillMaxWidth()) {
        val (priceRef, badgeRef) = createRefs()

        Text(
            text = price,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.constrainAs(priceRef) {
                start.linkTo(parent.start)
                top.linkTo(parent.top)
            },
        )
        Badge(
            modifier = Modifier.constrainAs(badgeRef) {
                start.linkTo(priceRef.end, 8.dp)
                top.linkTo(priceRef.top)
            },
        ) { Text(badge) }
    }
}
```

Alternatif yang sering lebih jelas adalah `Row(verticalAlignment = Alignment.Top)`.

### `BoxWithConstraints`

**Fungsi:** menyediakan constraints parent saat composition child.

**Gunakan untuk:** sebuah card reusable harus mengganti susunan internal berdasarkan lebar yang benar-benar diterimanya. Untuk layout aplikasi, gunakan window size class agar keputusan responsive terpusat.

```kotlin
@Composable
fun ResponsiveMetricCard(label: String, value: String) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 320.dp) {
            Column { MetricContent(label, value) }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) { MetricContent(label, value) }
        }
    }
}
```

### Lazy list

`LazyColumn` dan `LazyRow` hanya compose/layout item yang dibutuhkan viewport. Berikan `key` stabil; gunakan `contentType` ketika satu list memiliki beberapa tipe item agar reuse lebih efektif.

```kotlin
@Composable
fun ProductList(
    products: List<ProductUi>,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = products,
            key = { it.id },
            contentType = { "product" },
        ) { product ->
            ProductCard(product, onClick = { onProductClick(product.id) })
        }
    }
}

@Composable
fun HorizontalMenu(items: List<MenuUi>, selectedId: String?) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id }) { item ->
            FilterChip(
                selected = item.id == selectedId,
                onClick = { /* select */ },
                label = { Text(item.label) },
            )
        }
    }
}
```

### Lazy grid

```kotlin
@Composable
fun CatalogGrid(products: List<ProductUi>) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("Produk populer", style = MaterialTheme.typography.headlineSmall)
        }
        items(products, key = { it.id }) { ProductCard(it) }
    }
}

@Composable
fun HorizontalDashboardGrid(metrics: List<MetricUi>) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = Modifier.height(220.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(metrics, key = { it.id }) { MetricCard(it) }
    }
}
```

Gunakan `GridCells.Adaptive` untuk lebar card minimum dan jumlah kolom fleksibel; `GridCells.Fixed` ketika design memang meminta jumlah kolom tetap.

### Staggered grid

```kotlin
@Composable
fun InspirationGallery(images: List<ImageUi>) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
    ) {
        items(images, key = { it.id }) { image ->
            AsyncImage(
                model = image.url,
                contentDescription = image.description,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium),
            )
        }
    }
}
```

Hindari staggered grid jika alignment baris penting untuk membandingkan produk.

### Pager

`HorizontalPager` dan `VerticalPager` bersifat lazy dan menggunakan `PagerState`. Hindari nilai `beyondViewportPageCount` besar karena menghilangkan manfaat lazy composition.

```kotlin
@Composable
fun ProductGallery(images: List<String>) {
    val pagerState = rememberPagerState(pageCount = { images.size })

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 24.dp),
        ) { page ->
            AsyncImage(
                model = images[page],
                contentDescription = "Gambar ${page + 1} dari ${images.size}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(MaterialTheme.shapes.large),
            )
        }
        Text("${pagerState.currentPage + 1}/${images.size}")
    }
}

@Composable
fun FullPageStories(stories: List<StoryUi>) {
    val state = rememberPagerState(pageCount = { stories.size })
    VerticalPager(state = state, modifier = Modifier.fillMaxSize()) { page ->
        StoryPage(stories[page], Modifier.fillMaxSize())
    }
}
```

Referensi layout: [Layout basics](https://developer.android.com/develop/ui/compose/layouts/basics), [Lazy lists and grids](https://developer.android.com/develop/ui/compose/lists), [Pager](https://developer.android.com/develop/ui/compose/layouts/pager), dan [Flow layouts](https://developer.android.com/develop/ui/compose/layouts/flow).

## 2. Text API

### `Text` vs `BasicText`

| Pilihan | Gunakan ketika | Alasan |
|---|---|---|
| `Text` | Aplikasi Material 3 | Mengikuti `LocalTextStyle`, `LocalContentColor`, dan API Material. |
| `BasicText` | Membangun primitive design system non-Material atau butuh overload Foundation | Lebih rendah level dan tidak membawa opinion Material. |

```kotlin
@Composable
fun TextPrimitiveExamples() {
    Text(
        text = "Material title",
        style = MaterialTheme.typography.titleLarge,
    )
    BasicText(
        text = "Foundation primitive",
        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
    )
}
```

### API styling

| API | Scope | Contoh |
|---|---|---|
| `TextStyle` | Seluruh `Text` | Font family, size, weight, line height, letter spacing. |
| `SpanStyle` | Range karakter | Sebagian kata berwarna/bold/underline. |
| `ParagraphStyle` | Range paragraf | Alignment, direction, indent, line height. |
| `AnnotatedString` | Model rich text | Menyimpan text + range style + annotations/link. |
| `TextOverflow.Ellipsis` | Overflow | Judul card maksimal dua baris. |
| `maxLines` | Line constraint | Membatasi tinggi teks dinamis. |
| `softWrap` | Wrapping | `false` untuk ticker/label satu baris; biasanya biarkan `true`. |

```kotlin
@Composable
fun TypographyExamples() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Ajukan Pinjaman", style = MaterialTheme.typography.headlineMedium)
        Text("Dana cepat untuk kebutuhan Anda", style = MaterialTheme.typography.bodyMedium)
        Text("Diperbarui 2 menit lalu", style = MaterialTheme.typography.labelSmall)
        Text(
            text = "Rp25.000.000",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Judul produk yang sangat panjang dan harus dibatasi",
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
```

### Rich text dan link modern

```kotlin
@Composable
fun PriceWithSuffix() {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp)) {
                append("8,5%")
            }
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                append(" per tahun")
            }
        }
    )
}

@Composable
fun TermsText() {
    Text(
        buildAnnotatedString {
            append("Dengan melanjutkan, Anda menyetujui ")
            withLink(
                LinkAnnotation.Url(
                    url = "https://example.com/terms",
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                        )
                    ),
                )
            ) { append("Syarat dan Ketentuan") }
        },
        style = MaterialTheme.typography.bodySmall,
    )
}
```

Jangan memakai `ClickableText` untuk kode baru; gunakan `LinkAnnotation` pada `AnnotatedString` yang diberikan ke `Text`.

Referensi: [Style text](https://developer.android.com/develop/ui/compose/text/style-text) dan [Text interactions](https://developer.android.com/develop/ui/compose/text/user-interactions).

## 3. Image API

### Pemilihan API

| Kebutuhan | API | Catatan |
|---|---|---|
| Foto/ilustrasi drawable lokal | `Image(painterResource(...))` | Berikan description bermakna atau `null` jika dekoratif. |
| Symbol yang mengikuti theme | `Icon` | Default tint berasal dari content color; icon-only action tetap dibungkus `IconButton`. |
| URL/network dengan Coil | `AsyncImage` | Pilihan default; request size mengikuti constraints. |
| Membutuhkan `Painter` dari URL | `rememberAsyncImagePainter` | Atur size resolver jika perlu; jangan pilih hanya untuk kasus biasa. |
| Avatar/banner/thumbnail | `ContentScale.Crop` | Mengisi bounds dengan kemungkinan crop. |
| Logo harus terlihat utuh | `ContentScale.Fit` | Dapat menyisakan ruang kosong. |
| Tint vector | `Icon` atau `ColorFilter.tint` | Hindari tint foto kecuali efek memang diminta. |

```kotlin
@Composable
fun ImageExamples(avatarUrl: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Image(
            painter = painterResource(R.drawable.loan_banner),
            contentDescription = "Promo pinjaman",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp)),
        )

        Icon(
            imageVector = Icons.Outlined.Verified,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatarUrl)
                .crossfade(true)
                .build(),
            placeholder = painterResource(R.drawable.avatar_placeholder),
            error = painterResource(R.drawable.avatar_placeholder),
            contentDescription = "Foto profil",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(64.dp).clip(CircleShape),
        )
    }
}
```

### Background image dengan overlay

```kotlin
@Composable
fun HeroBanner(imageUrl: String, title: String) {
    Box(Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.large)) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .7f)))
            )
        )
        Text(
            title,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
        )
    }
}
```

### Ketika membutuhkan `Painter` dan tint

```kotlin
@Composable
fun PainterAndTintExamples(imageUrl: String) {
    val remotePainter = rememberAsyncImagePainter(model = imageUrl)

    Image(
        painter = remotePainter,
        contentDescription = "Thumbnail dokumen",
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(96.dp).clip(MaterialTheme.shapes.medium),
    )

    Image(
        painter = painterResource(R.drawable.ic_vehicle),
        contentDescription = "Kendaraan",
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
    )
}
```

Gunakan `rememberAsyncImagePainter` saat API benar-benar membutuhkan `Painter` atau Anda perlu mengobservasi `AsyncImagePainter.state`. Untuk display URL biasa, `AsyncImage` lebih tepat karena menghitung request size berdasarkan constraints. `ColorFilter.tint` lazim untuk vector/monochrome asset, bukan foto.

Referensi: [Loading images](https://developer.android.com/develop/ui/compose/graphics/images/loading), [Customize images](https://developer.android.com/develop/ui/compose/graphics/images/customize), dan [Coil Compose](https://coil-kt.github.io/coil/compose/).
