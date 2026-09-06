# 01 — Compose UI Component API Overview

[← Index](README.md) · [Berikutnya: Layout, Text, dan Image →](02-layout-text-image.md)

## Frequency

- ⭐⭐⭐⭐⭐ **Sangat sering:** hampir selalu muncul pada aplikasi.
- ⭐⭐⭐⭐ **Sering:** umum pada banyak screen atau design system.
- ⭐⭐⭐ **Sedang:** tergantung pola produk.
- ⭐⭐ **Jarang:** requirement tertentu.
- ⭐ **Khusus:** advanced atau custom design.

Frequency menggambarkan frekuensi saat slicing aplikasi bisnis/konsumen umum, bukan tingkat kepentingan API.

## Layout dan collection

| Component / API | Category | Frequency | Kapan digunakan | Alasan menggunakan | Hindari jika | Alternatif |
|---|---|---:|---|---|---|---|
| `Row` | Layout | ⭐⭐⭐⭐⭐ | Elemen tersusun horizontal | Sederhana, scope-safe, mendukung `weight` | Item perlu wrap atau lazy | `FlowRow`, `LazyRow` |
| `Column` | Layout | ⭐⭐⭐⭐⭐ | Elemen tersusun vertikal | Struktur screen paling umum | Data panjang/unknown | `LazyColumn` |
| `Box` | Layout | ⭐⭐⭐⭐⭐ | Overlay, badge, alignment bebas dalam satu area | Ringan dan mudah dipahami | Hanya butuh urutan linear | `Row`, `Column` |
| `Spacer` | Layout | ⭐⭐⭐⭐ | Jarak eksplisit atau flexible space | Menunjukkan ruang sebagai elemen layout | Semua sibling hanya perlu gap seragam | `Arrangement.spacedBy` |
| `FlowRow` | Layout | ⭐⭐⭐ | Chip/tag perlu wrap ke baris berikutnya | Responsive tanpa menghitung baris | Collection sangat besar | `LazyVerticalGrid` |
| `FlowColumn` | Layout | ⭐⭐ | Item mengalir ke kolom baru | Cocok untuk flow vertikal terbatas | Pola baca utama vertikal biasa | `Column`, grid |
| `BoxWithConstraints` | Responsive | ⭐⭐ | Child perlu keputusan lokal berdasarkan constraints | Menyediakan `min/maxWidth/Height` | Keputusan layout level aplikasi | Window size class |
| `ConstraintLayout` | Layout | ⭐⭐ | Relasi sibling/guideline kompleks atau transisi ConstraintSet | Relasi posisi eksplisit | Layout dapat dibaca sebagai Row/Column/Box | Layout primitives, custom `Layout` |
| `LazyColumn` | Lazy list | ⭐⭐⭐⭐⭐ | List vertikal panjang/unknown | Hanya compose item yang diperlukan | Semua item sedikit dan tak perlu scroll | `Column` |
| `LazyRow` | Lazy list | ⭐⭐⭐⭐ | Carousel/menu horizontal panjang | Lazy dan scroll state bawaan | Semua item harus wrap | `FlowRow` |
| `LazyVerticalGrid` | Lazy grid | ⭐⭐⭐⭐ | Katalog/dashboard grid | Fixed/adaptive columns dan item spans | Urutan linear lebih mudah dibaca | `LazyColumn`, `FlowRow` |
| `LazyHorizontalGrid` | Lazy grid | ⭐⭐ | Grid yang digeser horizontal | Mendukung row count dan lazy loading | UX horizontal tidak jelas | `LazyVerticalGrid` |
| `LazyVerticalStaggeredGrid` | Staggered grid | ⭐⭐ | Masonry photo/feed dengan tinggi berbeda | Mendukung item tidak seragam | Card seharusnya sejajar | `LazyVerticalGrid` |
| `HorizontalPager` | Pager | ⭐⭐⭐ | Onboarding, gallery, tab content swipe | Paging lazy dan snap per halaman | Konten seharusnya continuous scroll | `LazyRow` |
| `VerticalPager` | Pager | ⭐⭐ | Full-page vertical feed | Snap per halaman | Dokumen/list biasa | `LazyColumn` |
| `Layout` | Custom layout | ⭐ | Algoritma measure/place khusus | Kontrol penuh atas child | Primitive sudah cukup | `Row`, `Column`, `Box` |
| `Canvas` | Custom drawing | ⭐⭐ | Chart, custom decoration, shape kompleks | API drawing langsung | UI bisa dibangun dari komponen | `drawBehind`, vector asset |

## Text dan image

| Component / API | Category | Frequency | Kapan digunakan | Alasan menggunakan | Hindari jika | Alternatif |
|---|---|---:|---|---|---|---|
| `Text` | Text | ⭐⭐⭐⭐⭐ | Semua teks Material | Theme, semantics, dan style nyaman | Membangun design system non-Material tingkat rendah | `BasicText` |
| `BasicText` | Text | ⭐⭐ | Primitive design system custom | Kontrol dasar tanpa opinion Material | Material styling dibutuhkan | `Text` |
| `AnnotatedString` | Rich text | ⭐⭐⭐ | Sebagian teks berbeda style/link | Multi-style dalam satu node text | Beberapa blok punya layout berbeda | Beberapa `Text` |
| `LinkAnnotation` | Text link | ⭐⭐⭐ | Link pada sebagian text | Pengganti modern `ClickableText` | Seluruh elemen adalah aksi | `TextButton`, `Modifier.clickable` |
| `TextStyle` | Typography | ⭐⭐⭐⭐⭐ | Style satu blok text | Menggabungkan font, size, line-height, color | Token theme sudah cukup langsung | `MaterialTheme.typography.*` |
| `SpanStyle` | Rich text | ⭐⭐⭐ | Styling karakter/range | Granular tanpa memecah layout | Style satu paragraf penuh | `TextStyle` |
| `ParagraphStyle` | Rich text | ⭐⭐ | Alignment, indent, line height per paragraf | Styling level paragraf | Hanya style inline | `SpanStyle` |
| `Image` | Image | ⭐⭐⭐⭐⭐ | Bitmap/vector/painter lokal | Primitive gambar dengan content scale | Simbol tematik | `Icon` |
| `Icon` | Icon | ⭐⭐⭐⭐⭐ | Simbol aksi/status | Otomatis memakai `LocalContentColor` dan ukuran ikon | Foto/ilustrasi | `Image` |
| `AsyncImage` (Coil) | Remote image | ⭐⭐⭐⭐ | URL, cache, placeholder/error | Resolusi request mengikuti constraints | Butuh `Painter` atau pipeline non-Coil | `rememberAsyncImagePainter`, loader lain |
| `rememberAsyncImagePainter` (Coil) | Remote painter | ⭐⭐ | API memerlukan `Painter` atau observasi state khusus | Fleksibel | Hanya ingin menampilkan URL | `AsyncImage` |
| `painterResource` | Resource | ⭐⭐⭐⭐⭐ | Drawable/vector lokal | Satu API untuk raster dan vector drawable | Remote URL | Coil `AsyncImage` |
| `ContentScale` | Image sizing | ⭐⭐⭐⭐ | Mengatur fit/crop/fill dalam bounds | Menjaga aturan aspect ratio eksplisit | Ukuran intrinsic selalu tepat | Default `Fit` |

## Actions, input, dan selection

| Component / API | Category | Frequency | Kapan digunakan | Alasan menggunakan | Hindari jika | Alternatif |
|---|---|---:|---|---|---|---|
| `Button` | Primary action | ⭐⭐⭐⭐⭐ | CTA utama seperti Simpan/Lanjutkan | Emphasis tertinggi | Banyak primary button berdekatan | Tonal/Outlined/Text |
| `FilledTonalButton` | Action | ⭐⭐⭐⭐ | Aksi penting tetapi lebih lembut | Menjaga hierarchy tanpa outline | CTA paling dominan membutuhkan contrast | `Button` |
| `ElevatedButton` | Action | ⭐⭐⭐ | Action di atas surface berpola/berwarna | Separation lewat elevation | Elevation tidak punya makna | Filled tonal |
| `OutlinedButton` | Secondary action | ⭐⭐⭐⭐⭐ | Alternatif dari primary action | Medium emphasis | Aksi sangat rendah | `TextButton` |
| `TextButton` | Tertiary action | ⭐⭐⭐⭐⭐ | Dialog action/link/action ringan | Tidak mendominasi layout | Primary CTA | `Button` |
| `IconButton` | Icon action | ⭐⭐⭐⭐⭐ | Toolbar atau aksi ringkas | Semantik button dan touch target | Makna ikon ambigu tanpa label/tooltip | Button berlabel |
| Filled/tonal/outlined icon button | Icon action | ⭐⭐⭐ | Icon action perlu emphasis/state | Visual hierarchy lebih jelas | Toolbar sudah ramai | `IconButton` |
| FAB variants | Floating action | ⭐⭐⭐ | Satu aksi utama kontekstual screen | Aksi mudah dijangkau dan menonjol | Lebih dari satu aksi utama | `Button`, app-bar action |
| `TextField` | Filled input | ⭐⭐⭐⭐ | Form Material dengan emphasis filled | Label/supporting/error slots | Figma memakai outlined field | `OutlinedTextField` |
| `OutlinedTextField` | Outlined input | ⭐⭐⭐⭐⭐ | Form umum di atas background netral | Bounds dan state jelas | Banyak field padat dengan filled pattern | `TextField` |
| `BasicTextField` | Custom input | ⭐⭐ | Search/input custom yang jauh dari Material | Kendali decoration penuh | Standard Material field sudah cocok | `TextField` |
| `SecureTextField` / `OutlinedSecureTextField` | Password | ⭐⭐⭐ | Input password dengan API state-based | Obfuscation dan security defaults | Dependency belum mendukung | TextField + transformation |
| `Checkbox` | Multi selection | ⭐⭐⭐⭐ | Pilihan independen/multiple agreement | Status checked eksplisit | Tepat satu pilihan wajib | `RadioButton` |
| `RadioButton` | Single selection | ⭐⭐⭐ | Tepat satu dari beberapa opsi | Semantik mutual exclusion | Hanya dua state on/off | `Switch` |
| `Switch` | Setting | ⭐⭐⭐⭐ | Efek on/off langsung | Mencerminkan state setting | Aksi butuh submit terpisah | `Checkbox` |
| `Slider` | Range input | ⭐⭐⭐ | Nilai dalam rentang kontinu/diskrit | Manipulasi langsung | Nilai presisi perlu diketik | Number text field |
| `RangeSlider` | Range input | ⭐⭐ | Minimum dan maksimum | Dua thumb dalam satu konteks | Hanya satu nilai | `Slider` |
| `AssistChip` | Chip | ⭐⭐⭐ | Aksi kontekstual ringkas | Compact action dengan label/icon | Primary CTA | Button |
| `FilterChip` | Filter | ⭐⭐⭐⭐ | Toggle filter satu atau banyak | Selected state jelas | Banyak opsi kompleks | Filter sheet |
| `InputChip` | Entity input | ⭐⭐⭐ | Tag/penerima terpilih yang bisa dihapus | Mewakili entity input | Filter biasa | `FilterChip` |
| `SuggestionChip` | Suggestion | ⭐⭐⭐ | Saran dinamis | Ringkas dan mudah dipilih | Aksi wajib | Button |
| `SegmentedButton` | Selection | ⭐⭐⭐ | 2–5 opsi berdampingan | Perbandingan dan selected state langsung | Opsi banyak/label panjang | Chips, radio |

## Container, navigation, overlay, dan feedback

| Component / API | Category | Frequency | Kapan digunakan | Alasan menggunakan | Hindari jika | Alternatif |
|---|---|---:|---|---|---|---|
| `Surface` | Container | ⭐⭐⭐⭐⭐ | Container mengikuti color/elevation/shape Material | Propagasi content color dan tonal elevation | Hanya butuh positioning | `Box` |
| `Card` | Container | ⭐⭐⭐⭐⭐ | Group konten satu subjek | Semantik visual card dan action overload | Group tidak perlu boundary | `Surface`, `Column` |
| `ElevatedCard` | Card | ⭐⭐⭐ | Card perlu separation lewat elevation | Menonjol di atas surface | Banyak elevation membuat noise | `Card`, `OutlinedCard` |
| `OutlinedCard` | Card | ⭐⭐⭐⭐ | Group perlu batas tanpa elevation | Cocok pada surface datar | Divider/spacing sudah cukup | `Card` |
| `Scaffold` | Screen structure | ⭐⭐⭐⭐⭐ | Root screen dengan app bar/FAB/snackbar/bottom bar | Slot dan insets terkoordinasi | Komponen kecil | `Box`/`Column` |
| `TopAppBar` | App bar | ⭐⭐⭐⭐⭐ | Title ringkas, navigation, actions | Pola standard screen | Landing page tanpa chrome | Custom header |
| `CenterAlignedTopAppBar` | App bar | ⭐⭐⭐ | Title singkat berpusat | Visual formal/simetris | Banyak actions atau title panjang | `TopAppBar` |
| `MediumTopAppBar` | App bar | ⭐⭐ | Hierarchy title lebih kuat dan collapsible | Transisi scroll bawaan | Screen padat | `TopAppBar` |
| `LargeTopAppBar` | App bar | ⭐⭐⭐ | Destination utama dengan title besar | Hierarchy kuat | Subscreen sederhana | Medium/Small |
| `NavigationBar` | Visual navigation | ⭐⭐⭐⭐⭐ | 3–5 top-level destination pada compact window | Reachability dan selected state | Banyak destination | Rail/drawer |
| `NavigationRail` | Visual navigation | ⭐⭐⭐ | Top-level destination pada medium/expanded window | Memakai ruang horizontal | Compact portrait | Navigation bar |
| `ModalNavigationDrawer` | Visual navigation | ⭐⭐⭐ | Banyak destination; drawer overlay | Hemat ruang saat ditutup | 3–5 destination utama | Bar/rail |
| `PermanentNavigationDrawer` | Visual navigation | ⭐⭐ | Expanded window dengan drawer selalu tampil | Navigasi selalu terlihat | Ruang sempit | Rail/modal drawer |
| `NavigationSuiteScaffold` | Adaptive nav | ⭐⭐⭐ | Bar/rail/drawer perlu beradaptasi | Menyatukan keputusan adaptive | Layout selalu satu ukuran | Komponen nav langsung |
| `NavDisplay` (Navigation 3) | Navigation engine UI | ⭐⭐⭐⭐ | Render back stack Compose-first | App memiliki back stack sebagai state | App masih tergantung Fragment/Nav2 | `NavHost` |
| `AlertDialog` | Dialog | ⭐⭐⭐⭐ | Konfirmasi/prompt Material standar | Slots title/text/buttons siap | Layout modal sangat custom | `BasicAlertDialog`, `Dialog` |
| `BasicAlertDialog` | Dialog | ⭐⭐ | Custom dialog tetap memakai primitive Material | Lebih bebas dari `AlertDialog` | Hanya perlu prompt biasa | `AlertDialog` |
| `Dialog` | Dialog | ⭐⭐⭐ | Full custom modal | Kontrol ukuran/container penuh | Standard alert sudah cukup | `AlertDialog` |
| `ModalBottomSheet` | Bottom sheet | ⭐⭐⭐⭐ | Secondary task/options pada mobile | Mudah di-dismiss dan dekat jempol | Keputusan kritis/harus dibaca | Dialog/full screen |
| `BottomSheetScaffold` | Persistent sheet | ⭐⭐ | Sheet menjadi bagian persisten screen | Integrasi sheet + content | Sheet sesekali | `ModalBottomSheet` |
| `SnackbarHost` / `Snackbar` | Feedback | ⭐⭐⭐⭐⭐ | Pesan transient dengan optional action | Tidak memblokir flow | Error butuh resolusi kompleks | Inline error/dialog |
| `CircularProgressIndicator` | Progress | ⭐⭐⭐⭐ | Loading indeterminate atau area kecil | Compact | Progress panjang/known | Linear indicator |
| `LinearProgressIndicator` | Progress | ⭐⭐⭐ | Progress screen/operation horizontal | Cocok untuk determinate progress | Button kecil | Circular indicator |
| `DropdownMenu` | Menu | ⭐⭐⭐⭐ | Overflow/action menu sementara | Anchored popup standar | Pilihan adalah value form | Exposed dropdown |
| `ExposedDropdownMenuBox` | Form/menu | ⭐⭐⭐ | Select field dengan exposed options | Relasi field-menu jelas | Banyak opsi/search kompleks | Dialog/sheet/autocomplete |
| `Badge` / `BadgedBox` | Status | ⭐⭐⭐ | Count/unread/status pada icon | Overlay dan semantics mudah | Informasi utama | Text/label |
| `HorizontalDivider` / `VerticalDivider` | Separation | ⭐⭐⭐⭐ | Group benar-benar perlu separator | Boundary ringan | Spacing cukup membedakan group | Spacing/surface color |
| `TooltipBox` | Guidance | ⭐⭐⭐ | Icon/fitur perlu penjelasan hover/long press | Mendukung mouse dan touch | Informasi wajib diketahui | Visible label/supporting text |
| `PullToRefreshBox` | Refresh | ⭐⭐⭐ | User mengharapkan gesture refresh pada feed | Container + indicator Material | Data realtime/refresh tidak relevan | Explicit refresh button |

## Modifier, drawing, dan accessibility

| Component / API | Category | Frequency | Kapan digunakan | Alasan menggunakan | Hindari jika | Alternatif |
|---|---|---:|---|---|---|---|
| `padding`, `size`, `fillMax*` | Layout modifier | ⭐⭐⭐⭐⭐ | Hampir semua sizing/spacing | Constraint-aware dan chainable | Token tersedia tetapi diabaikan | Token + modifier yang sama |
| `weight`, `align` | Parent data | ⭐⭐⭐⭐ | Child memberi informasi ke parent scope | Type-safe terhadap Row/Column/Box | Child bukan direct child scope | Parent arrangement |
| `offset` | Position | ⭐⭐⭐ | Geser visual tanpa mengubah measurement sibling | Cocok untuk badge/animation | Ingin ruang layout ikut berubah | `padding`, arrangement |
| `background`, `border`, `clip` | Appearance | ⭐⭐⭐⭐⭐ | Surface visual custom | Composable dan order-aware | Material surface sudah cocok | `Surface` |
| `shadow` | Appearance | ⭐⭐⭐ | Shadow custom | Kontrol shape/elevation | Material tonal elevation cukup | `Surface` elevation |
| `clickable` | Interaction | ⭐⭐⭐⭐⭐ | Seluruh node menjadi aksi | Ripple, input, semantics dasar | Komponen Button sudah tersedia | `Button`, `IconButton` |
| `combinedClickable` | Interaction | ⭐⭐ | Click + double/long click | Gesture tingkat tinggi terpadu | Long-click tidak discoverable | Visible menu/action |
| `toggleable`, `selectable` | Semantics/input | ⭐⭐⭐ | Membuat custom toggle/choice | Semantics tepat | Standard control cocok | Checkbox/radio/switch |
| `verticalScroll`, `horizontalScroll` | Scroll | ⭐⭐⭐⭐ | Konten kecil yang semuanya harus composed | Mudah untuk layout non-lazy | Banyak/unknown item | Lazy layout |
| `drawBehind` | Drawing | ⭐⭐⭐ | Decoration di belakang content | Tidak perlu Canvas sibling | Objek drawing mahal tiap frame | `drawWithCache` |
| `drawWithContent` | Drawing | ⭐⭐ | Draw sebelum/sesudah content | Kontrol draw order | Hanya background sederhana | `background`, `drawBehind` |
| `graphicsLayer` | Graphics | ⭐⭐ | Transform/alpha/clip dan animation cepat | Layer properties tanpa relayout | Layout position harus berubah | layout modifier |
| `semantics` | Accessibility/test | ⭐⭐⭐⭐ | Custom component butuh role/state/action/label | Mengisi semantics tree | Material component sudah benar | Built-in semantics |
| `clearAndSetSemantics` | Accessibility | ⭐⭐ | Descendant semantics harus diganti sebagai satu konsep | Kontrol announcement | Hanya ingin menambahkan properti | `semantics` |

## Ringkasan tier

| Tier | Fokus | API representatif |
|---|---|---|
| **Tier 1 — wajib** | Membuat hampir semua screen | `Text`, `Row`, `Column`, `Box`, `Modifier`, `Image`, `Button`, `OutlinedTextField`, `LazyColumn`, `Surface`, `Card`, `Scaffold`, state hoisting |
| **Tier 2 — sering** | Aplikasi production umum | Grid, app bar, navigation bar, sheet, snackbar, menu, selection control, chips, pager, Coil, preview |
| **Tier 3 — situasional** | Pola produk tertentu | Rail/drawer, staggered grid, tooltip, pull refresh, segmented button, rich text, custom dialog, adaptive pane |
| **Tier 4 — advanced** | Design/interaction sangat khusus | Custom `Layout`, Canvas/draw modifiers, `ConstraintLayout`, custom semantics, custom state holder |

Penjelasan dan checklist lengkap tier tersedia di [09 — Cheat Sheet dan Priority](09-cheat-sheet-priority.md).

## Rujukan

- [Material components](https://developer.android.com/develop/ui/compose/components)
- [Lazy lists and grids](https://developer.android.com/develop/ui/compose/lists)
- [Flow layouts](https://developer.android.com/develop/ui/compose/layouts/flow)
- [Compose modifiers](https://developer.android.com/develop/ui/compose/modifiers)
- [Graphics modifiers](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers)

