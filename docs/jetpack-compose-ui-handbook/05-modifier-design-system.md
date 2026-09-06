# 05 — Modifier dan Design System

[← Container, Navigation, dan Feedback](04-container-navigation-feedback.md) · [Index](README.md) · [Workflow Slicing →](06-slicing-workflow.md)

## 1. Mental model Modifier

`Modifier` adalah immutable ordered chain. Setiap elemen modifier membungkus elemen setelahnya, sehingga urutan dapat mengubah constraints, ukuran, area gambar, dan area interaksi.

Reusable composable sebaiknya:

```kotlin
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp), content = content)
    }
}
```

- menerima satu `modifier: Modifier = Modifier`;
- menerapkannya pada root node yang relevan;
- tidak menambahkan `Modifier` milik caller ke arbitrary child;
- tidak memakai parameter `padding`, `backgroundColor`, `clickable` generik jika modifier atau slot lebih tepat;
- tetap dapat menyediakan parameter semantik penting seperti `enabled`, `selected`, dan `onClick`.

Referensi: [Compose modifiers](https://developer.android.com/develop/ui/compose/modifiers).

## 2. Modifier reference

### Layout

| Modifier | Fungsi | Contoh penggunaan | Frequency |
|---|---|---|---:|
| `padding` | Memberi inset dan mengubah constraints child | Padding screen/card | ⭐⭐⭐⭐⭐ |
| `size` | Preferred width + height | Icon/avatar | ⭐⭐⭐⭐⭐ |
| `width`, `height` | Preferred satu dimensi | Sheet handle/banner | ⭐⭐⭐⭐⭐ |
| `sizeIn` | Min/max bounds | Touch target minimal 48dp | ⭐⭐⭐⭐ |
| `requiredSize` | Memaksa ukuran walau berbeda dari constraints | Kasus khusus artwork | ⭐ |
| `fillMaxWidth` | Mengisi max width yang tersedia | Form/button | ⭐⭐⭐⭐⭐ |
| `fillMaxHeight` | Mengisi max height | Divider/pane | ⭐⭐⭐ |
| `fillMaxSize` | Mengisi kedua sumbu | Root/overlay | ⭐⭐⭐⭐⭐ |
| `wrapContentSize` | Membungkus content dan mengatur alignment | Badge/loading overlay | ⭐⭐⭐ |
| `weight` | Membagi ruang parent Row/Column | Dua pane/flexible spacer | ⭐⭐⭐⭐ |
| `aspectRatio` | Menjaga rasio width-height | Thumbnail 16:9 | ⭐⭐⭐⭐ |
| `defaultMinSize` | Default minimum tanpa mengalahkan caller | Primitive design system | ⭐⭐ |
| `paddingFromBaseline` | Spacing berdasarkan baseline text | Typography presisi | ⭐⭐ |

### Position dan parent data

| Modifier | Fungsi | Contoh penggunaan | Frequency |
|---|---|---|---:|
| `offset` | Menggeser hasil placement tanpa mengubah measurement sibling | Badge/transisi | ⭐⭐⭐ |
| `absoluteOffset` | Offset yang mengabaikan layout direction | Grafik/coordinate khusus | ⭐ |
| `align` | Posisi child dalam scope parent | Bottom-end di `Box` | ⭐⭐⭐⭐ |
| `matchParentSize` | Child mengikuti ukuran `Box` tanpa memengaruhi ukuran Box | Scrim/overlay | ⭐⭐⭐ |

`weight`, `align`, dan `matchParentSize` adalah parent-data/scope-specific. Modifier tersebut hanya bekerja pada child yang berada di scope/direct parent yang benar.

### Appearance

| Modifier | Fungsi | Contoh penggunaan | Frequency |
|---|---|---|---:|
| `background` | Menggambar warna/brush di belakang content | Custom tag/container | ⭐⭐⭐⭐⭐ |
| `border` | Menggambar stroke mengikuti shape | Selected card | ⭐⭐⭐⭐ |
| `clip` | Memotong drawing/input sesuai shape | Avatar/banner | ⭐⭐⭐⭐⭐ |
| `alpha` | Mengubah opacity | Disabled decoration/fade | ⭐⭐⭐ |
| `shadow` | Menggambar shadow dengan shape | Custom floating surface | ⭐⭐⭐ |
| `blur` | Blur render output | Efek visual platform-supported | ⭐ |

Untuk container Material, pertimbangkan `Surface` agar color, content color, shape, dan elevation tetap koheren.

### Interaction

| Modifier | Fungsi | Contoh penggunaan | Frequency |
|---|---|---|---:|
| `clickable` | Click, indication, semantics dasar | Clickable card/row | ⭐⭐⭐⭐⭐ |
| `combinedClickable` | Click, long click, double click | Item dengan context action | ⭐⭐ |
| `toggleable` | Custom boolean control + toggle semantics | Row checkbox/switch | ⭐⭐⭐ |
| `triStateToggleable` | On/off/indeterminate | Select-all hierarchy | ⭐⭐ |
| `selectable` | Custom single-choice control | Radio row/tab custom | ⭐⭐⭐ |
| `selectableGroup` | Menandai parent single-selection | Radio group custom | ⭐⭐ |
| `pointerInput` | Gesture detector tingkat rendah | Drag/transform custom | ⭐⭐ |

Pilih API abstraction tertinggi yang cocok. `Button` lebih baik daripada `Box.clickable` untuk button; `clickable` lebih baik daripada `pointerInput` untuk click biasa.

### Scroll

| Modifier/API | Fungsi | Contoh penggunaan | Frequency |
|---|---|---|---:|
| `verticalScroll` | Scroll vertikal non-lazy | Form/detail pendek | ⭐⭐⭐⭐ |
| `horizontalScroll` | Scroll horizontal non-lazy | Short comparison row | ⭐⭐⭐ |
| `scrollable` | Mengonsumsi delta scroll tanpa otomatis memindahkan content | Custom control | ⭐ |
| `nestedScroll` | Menghubungkan child-parent scroll | Collapsing app bar | ⭐⭐⭐ |

### Drawing dan graphics

| Modifier | Fungsi | Contoh penggunaan | Frequency |
|---|---|---|---:|
| `drawBehind` | Draw sebelum content | Underline/custom background | ⭐⭐⭐ |
| `drawWithContent` | Mengontrol draw content + overlay | Scrim/mask | ⭐⭐ |
| `drawWithCache` | Cache object drawing berdasarkan size/state | Gradient/path mahal | ⭐⭐ |
| `graphicsLayer` | Transform, alpha, clip, render layer | Animation/3D transform | ⭐⭐ |
| `paint` | Menggambar `Painter` ke bounds | Background painter | ⭐⭐ |

```kotlin
fun Modifier.bottomAccent(color: Color): Modifier = drawBehind {
    drawRect(
        color = color,
        topLeft = Offset(0f, size.height - 2.dp.toPx()),
        size = Size(size.width, 2.dp.toPx()),
    )
}
```

Jika brush/path dibuat dari size dan mahal, gunakan `drawWithCache`. Lihat [Graphics modifiers](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers).

### Accessibility, focus, dan test

| Modifier | Fungsi | Contoh penggunaan | Frequency |
|---|---|---|---:|
| `semantics` | Menambah/mengubah semantics | `stateDescription`, heading, custom action | ⭐⭐⭐⭐ |
| `clearAndSetSemantics` | Mengganti semantics descendants | Card dibaca sebagai satu konsep | ⭐⭐ |
| `testTag` | Identifier test jika matcher semantic alami tidak cukup | Dynamic canvas node | ⭐⭐⭐ |
| `focusable` | Membuat node dapat fokus | Custom keyboard control | ⭐⭐ |
| `focusRequester` | Menghubungkan `FocusRequester` | Form focus flow | ⭐⭐ |
| `focusProperties` | Mengatur traversal/focus behavior | TV/tablet/keyboard | ⭐⭐ |

`contentDescription` biasanya merupakan parameter `Image`/`Icon` atau property di block `semantics`, bukan modifier function tersendiri.

## 3. Modifier order

### Padding dan background

```kotlin
// Padding berada di luar area merah.
Modifier
    .padding(16.dp)
    .background(Color.Red)

// Background meliputi content dan padding internal.
Modifier
    .background(Color.Red)
    .padding(16.dp)
```

Mental model: baca dari kiri ke kanan sebagai lapisan pembungkus dari luar ke dalam.

### Clickable area

```kotlin
// Seluruh card termasuk padding dapat diklik.
Modifier
    .clickable(onClick = onClick)
    .padding(16.dp)

// Padding luar tidak termasuk target klik.
Modifier
    .padding(16.dp)
    .clickable(onClick = onClick)
```

### Clip, background, dan border

```kotlin
Modifier
    .clip(RoundedCornerShape(16.dp))
    .background(MaterialTheme.colorScheme.surfaceContainer)
    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    .padding(16.dp)
```

`clip` sebelum `background` memotong background. Border membutuhkan shape yang sama agar sudut konsisten. Untuk kasus ini `Surface(shape, color, border)` sering lebih mudah dan semantik.

### Size dan padding

```kotlin
// Total luar mendekati 48dp + padding.
Modifier.size(48.dp).padding(8.dp)

// Content mendapat ukuran dalam constraints yang sudah dikurangi padding.
Modifier.padding(8.dp).size(48.dp)
```

Jangan mengandalkan tebakan. Gunakan Layout Inspector atau preview dengan background debug untuk melihat bounds.

Referensi: [Constraints and modifier order](https://developer.android.com/develop/ui/compose/layouts/constraints-modifiers).

## 4. Arrangement dan Alignment

`Arrangement` mendistribusikan sekumpulan child pada main axis. `Alignment` menentukan posisi content/child pada cross axis atau dalam `Box`.

### Arrangement

| API | Efek ringkas |
|---|---|
| `Start` / `Top` | Menumpuk dari awal sumbu. |
| `End` / `Bottom` | Menumpuk di akhir sumbu. |
| `Center` | Menumpuk di tengah. |
| `SpaceBetween` | Ruang hanya di antara item. |
| `SpaceAround` | Setiap item mendapat ruang di sekeliling; tepi bernilai setengah gap internal. |
| `SpaceEvenly` | Semua gap termasuk tepi sama. |
| `spacedBy(8.dp)` | Gap tetap antaritem, dapat dikombinasikan dengan alignment. |

```text
Start:        [A][B][C]........
End:          ........[A][B][C]
Center:       ....[A][B][C]....
SpaceBetween: [A]....[B]....[C]
SpaceAround:  .[A]..[B]..[C].
SpaceEvenly:  ..[A]..[B]..[C]..
```

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
    verticalAlignment = Alignment.CenterVertically,
) { /* actions */ }
```

### Alignment dalam Box

```text
TopStart        TopCenter        TopEnd

CenterStart     Center           CenterEnd

BottomStart     BottomCenter     BottomEnd
```

`Start` dan `End` mengikuti layout direction sehingga aman untuk RTL. Gunakan `Left`/`Right` hanya jika arah fisik memang dimaksudkan.

```kotlin
Box(Modifier.fillMaxSize()) {
    Text("Atas", Modifier.align(Alignment.TopCenter))
    FloatingActionButton(
        onClick = {},
        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
    ) { Icon(Icons.Default.Add, "Tambah") }
}
```

## 5. Shapes

| Shape | Use case |
|---|---|
| `RectangleShape` | Full-bleed container atau shape default tanpa radius. |
| `CircleShape` | Avatar, circular icon container, circular FAB. |
| `RoundedCornerShape` | Card, sheet, field, banner. |
| `CutCornerShape` | Design system dengan cut corner. |
| Custom `Shape` / `GenericShape` | Brand shape yang tidak dapat dibentuk dari standard corner. |

```kotlin
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

Image(
    painter = painterResource(R.drawable.avatar),
    contentDescription = "Foto profil",
    modifier = Modifier.size(64.dp).clip(CircleShape),
)

Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
    Text("Pill", Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
}

val TicketShape = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

Surface(shape = TicketShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
    Text("Custom brand shape", Modifier.padding(16.dp))
}
```

Jangan menyalin radius dari Figma tanpa memeriksa peran komponen. Radius semestinya menjadi token berdasarkan peran/ukuran, bukan angka unik per layer.

## 6. Color dan theme

### Hindari token hardcoded tersebar

```kotlin
// BAD: tidak adaptif terhadap dark theme dan sulit diubah.
Text("Bunga rendah", color = Color(0xFF123456))

// GOOD: nama token menjelaskan peran.
Text("Bunga rendah", color = MaterialTheme.colorScheme.primary)
```

Gunakan `ColorScheme` Material untuk semantic roles seperti `primary`, `surface`, `error`, `onSurface`, dan container variants. Jika brand membutuhkan role tambahan, sediakan extension token yang tetap semantik.

```kotlin
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

val LocalExtendedColors = staticCompositionLocalOf<ExtendedColors> {
    error("ExtendedColors belum disediakan")
}

@Composable
fun AppTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extended = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
```

Jangan memakai `CompositionLocal` sebagai service locator untuk repository atau business dependencies. Ia cocok untuk data tree-scoped seperti theme/design tokens.

## 7. Typography mapping

Mapping harus mengikuti hierarchy design, bukan nama literal semata. “Heading 2” di Figma bisa menjadi `headlineSmall` atau `titleLarge` tergantung peran dan skala.

| Figma/design role | Material 3 candidate | Contoh |
|---|---|---|
| Display hero | `displayLarge/Medium/Small` | Marketing hero besar |
| Page heading | `headlineLarge/Medium/Small` | Judul landing/destination |
| Section/title | `titleLarge/Medium/Small` | Judul section/card/list item |
| Body | `bodyLarge/Medium/Small` | Paragraf/deskripsi/supporting |
| Label/control | `labelLarge/Medium/Small` | Button, chip, caption metadata |
| Caption custom lama | Biasanya `bodySmall` atau `labelSmall` | Pilih berdasarkan apakah teks berupa konten atau label |

```kotlin
val AppTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)
```

Saat slicing, periksa font scale 1.0, 1.3, dan 2.0. Hindari fixed height pada container text karena teks dapat terpotong.

## 8. Spacing dan dimension tokens

```kotlin
object AppSpacing {
    val none = 0.dp
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

object AppDimensions {
    val minTouchTarget = 48.dp
    val iconSmall = 16.dp
    val iconMedium = 24.dp
    val avatarMedium = 48.dp
    val contentMaxWidth = 840.dp
}
```

Manfaat token:

- perubahan design system tidak membutuhkan pencarian magic number;
- Figma variables dapat dipetakan langsung;
- konsistensi rhythm lebih mudah direview;
- nama semantic/contextual dapat dibangun di atas scale, misalnya `ScreenPadding = AppSpacing.md`;
- preview dan snapshot mudah membandingkan consistency.

Jangan memaksa setiap jarak Figma ke token terdekat jika perbedaan itu memang bermakna. Konfirmasi apakah nilai tersebut intentional atau sekadar drift design.

## 9. Checklist design system saat slicing

- [ ] Color memakai semantic role dan memiliki light/dark pair.
- [ ] Typography memakai style role, memiliki line height, dan diuji dengan font scale.
- [ ] Shape berasal dari token/peran.
- [ ] Spacing mengikuti scale yang disepakati.
- [ ] Icon size dan touch target dipisahkan; icon 24dp dapat memiliki target 48dp.
- [ ] Component states tersedia: enabled, pressed, focused, selected, error, loading, disabled.
- [ ] Komponen menerima `Modifier` dan slot bila variasi content memang diperlukan.
- [ ] Elevation dipakai untuk hierarchy, bukan dekorasi acak.
- [ ] Semua token memiliki nama berdasarkan peran, bukan hanya warna visual seperti `Blue500` di call site.
