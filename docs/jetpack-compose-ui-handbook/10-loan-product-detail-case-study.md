# 10 — Complete Screen Slicing: Loan Product Detail

[← Cheat Sheet dan Priority](09-cheat-sheet-priority.md) · [Index](README.md)

## 1. Design brief

Screen memiliki:

- top app bar dengan back dan share;
- product title dan description;
- interest rate;
- maximum plafond;
- tenor;
- benefit card;
- simulation summary;
- secondary CTA “Simulasikan”;
- primary CTA “Ajukan sekarang”.

Target tambahan yang tidak selalu terlihat pada satu frame Figma:

- loading saat submit;
- unavailable/disabled application;
- benefit list kosong;
- content panjang dan font scale besar;
- safe area/navigation bar;
- accessibility labels;
- preview dan state hoisting.

## 2. Layout analysis

```text
Scaffold
├── TopAppBar
│   ├── Back IconButton
│   ├── Title
│   └── Share IconButton
├── LazyColumn / scrollable content
│   ├── ProductHero
│   ├── LoanMetrics (FlowRow)
│   │   ├── LoanMetric: bunga
│   │   ├── LoanMetric: plafond
│   │   └── LoanMetric: tenor
│   ├── BenefitCard
│   │   └── BenefitRow × N
│   └── SimulationSummaryCard
└── BottomActionBar
    ├── OutlinedButton
    └── Button
```

Keputusan:

- `LazyColumn` dipilih agar content panjang dan font scale besar tetap aman.
- `FlowRow` pada metrics dapat berpindah baris di width sempit.
- `Surface` membentuk bottom action bar dengan tonal/shadow hierarchy.
- `OutlinedCard` mengelompokkan benefit dan summary tanpa terlalu banyak elevation.
- State screen immutable dan semua event keluar melalui satu event sink.

## 3. Component mapping

| Design element | Compose API | Alasan |
|---|---|---|
| Screen shell | `Scaffold` | Mengatur app bar, bottom action, dan insets. |
| Toolbar | `TopAppBar` | Title, back, dan actions standar. |
| Scroll content | `LazyColumn` | Aman untuk content dinamis/panjang. |
| Metrics responsive | `FlowRow` | Dapat wrap tanpa breakpoint manual. |
| Metric container | `Surface` | Semantic color/shape dari theme. |
| Benefit grouping | `OutlinedCard` | Boundary ringan. |
| Benefit icon | `Icon` | Mengikuti content color. |
| Summary separation | `HorizontalDivider` | Memisahkan estimasi final. |
| Secondary CTA | `OutlinedButton` | Medium emphasis. |
| Primary CTA | `Button` | Emphasis tertinggi. |
| Loading submit | `CircularProgressIndicator` | Compact di dalam button. |

## 4. State dan events

```kotlin
@Immutable
data class LoanProductDetailUiState(
    val productName: String,
    val description: String,
    val interestRate: String,
    val maximumPlafond: String,
    val tenor: String,
    val benefits: List<String>,
    val simulation: LoanSimulationUi,
    val canApply: Boolean = true,
    val isApplying: Boolean = false,
)

@Immutable
data class LoanSimulationUi(
    val requestedAmount: String,
    val selectedTenor: String,
    val estimatedInstallment: String,
)

sealed interface LoanProductDetailEvent {
    data object Back : LoanProductDetailEvent
    data object Share : LoanProductDetailEvent
    data object OpenSimulation : LoanProductDetailEvent
    data object Apply : LoanProductDetailEvent
}
```

## 5. Design tokens

Dalam proyek nyata, gabungkan tokens dengan theme/design system pusat. Tokens lokal berikut hanya membuat studi kasus mandiri dan menunjukkan intent.

```kotlin
private object LoanSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

private object LoanDimensions {
    val contentMaxWidth = 840.dp
    val metricMinWidth = 144.dp
    val iconContainer = 40.dp
}
```

## 6. Reusable components

### Product hero

```kotlin
@Composable
private fun LoanProductHero(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LoanSpacing.sm),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                text = "PINJAMAN MULTIGUNA",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

### Metric

```kotlin
@Composable
private fun LoanMetric(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.widthIn(min = LoanDimensions.metricMinWidth),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            Modifier.padding(LoanSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LoanSpacing.sm),
        ) {
            Surface(
                modifier = Modifier.size(LoanDimensions.iconContainer),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null)
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun LoanMetrics(
    interestRate: String,
    maximumPlafond: String,
    tenor: String,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LoanSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(LoanSpacing.sm),
        maxItemsInEachRow = 3,
    ) {
        LoanMetric("Bunga mulai", interestRate, Icons.Outlined.Percent, Modifier.weight(1f))
        LoanMetric("Plafond hingga", maximumPlafond, Icons.Outlined.AccountBalanceWallet, Modifier.weight(1f))
        LoanMetric("Tenor hingga", tenor, Icons.Outlined.CalendarMonth, Modifier.weight(1f))
    }
}
```

`weight` pada `FlowRow` dihitung per baris. Uji baris terakhir ketika jumlah item berubah.

### Benefits

```kotlin
@Composable
private fun BenefitCard(benefits: List<String>, modifier: Modifier = Modifier) {
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(LoanSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LoanSpacing.md),
        ) {
            Text("Keuntungan", style = MaterialTheme.typography.titleLarge)
            benefits.forEach { benefit ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(LoanSpacing.sm),
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(benefit, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
```

### Summary

```kotlin
@Composable
private fun SummaryRow(
    label: String,
    value: String,
    emphasize: Boolean = false,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LoanSpacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun SimulationSummaryCard(
    simulation: LoanSimulationUi,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(LoanSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LoanSpacing.md),
        ) {
            Text("Ringkasan simulasi", style = MaterialTheme.typography.titleLarge)
            SummaryRow("Jumlah pinjaman", simulation.requestedAmount)
            SummaryRow("Tenor", simulation.selectedTenor)
            HorizontalDivider()
            SummaryRow("Estimasi cicilan/bulan", simulation.estimatedInstallment, emphasize = true)
        }
    }
}
```

### Bottom actions

```kotlin
@Composable
private fun LoanBottomActions(
    canApply: Boolean,
    isApplying: Boolean,
    onSimulate: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        tonalElevation = 2.dp,
    ) {
        Row(
            Modifier.navigationBarsPadding().padding(LoanSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(LoanSpacing.sm),
        ) {
            OutlinedButton(
                onClick = onSimulate,
                enabled = !isApplying,
                modifier = Modifier.weight(1f),
            ) {
                Text("Simulasikan")
            }
            Button(
                onClick = onApply,
                enabled = canApply && !isApplying,
                modifier = Modifier.weight(1f),
            ) {
                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                    Spacer(Modifier.width(LoanSpacing.sm))
                }
                Text(if (isApplying) "Memproses" else "Ajukan sekarang")
            }
        }
    }
}
```

## 7. Final screen implementation

```kotlin
@Composable
fun LoanProductDetailScreen(
    state: LoanProductDetailUiState,
    onEvent: (LoanProductDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detail produk",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(LoanProductDetailEvent.Back) }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(LoanProductDetailEvent.Share) }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Bagikan produk")
                    }
                },
            )
        },
        bottomBar = {
            LoanBottomActions(
                canApply = state.canApply,
                isApplying = state.isApplying,
                onSimulate = { onEvent(LoanProductDetailEvent.OpenSimulation) },
                onApply = { onEvent(LoanProductDetailEvent.Apply) },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().widthIn(max = LoanDimensions.contentMaxWidth),
                contentPadding = PaddingValues(LoanSpacing.md),
                verticalArrangement = Arrangement.spacedBy(LoanSpacing.lg),
            ) {
                item {
                    LoanProductHero(
                        title = state.productName,
                        description = state.description,
                    )
                }
                item {
                    LoanMetrics(
                        interestRate = state.interestRate,
                        maximumPlafond = state.maximumPlafond,
                        tenor = state.tenor,
                    )
                }
                if (state.benefits.isNotEmpty()) {
                    item { BenefitCard(state.benefits) }
                }
                item { SimulationSummaryCard(state.simulation) }
            }
        }
    }
}
```

## 8. Route dan ViewModel boundary

```kotlin
@Composable
fun LoanProductDetailRoute(
    viewModel: LoanProductDetailViewModel,
    onBack: () -> Unit,
    onShare: (String) -> Unit,
    onOpenSimulation: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LoanProductDetailScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                LoanProductDetailEvent.Back -> onBack()
                LoanProductDetailEvent.Share -> onShare(state.productName)
                LoanProductDetailEvent.OpenSimulation -> onOpenSimulation()
                LoanProductDetailEvent.Apply -> viewModel.apply()
            }
        },
    )
}
```

Screen tidak mengetahui navigation controller, repository, atau share intent. Route menghubungkan UI intent ke platform/navigation/business behavior.

## 9. Preview matrix

```kotlin
private fun previewLoanState() = LoanProductDetailUiState(
    productName = "Kredit Kendaraan Multiguna",
    description = "Solusi pembiayaan fleksibel dengan jaminan BPKB kendaraan untuk berbagai kebutuhan Anda.",
    interestRate = "8,5% p.a.",
    maximumPlafond = "Rp500 juta",
    tenor = "48 bulan",
    benefits = listOf(
        "Proses pengajuan mudah dan transparan",
        "Pilihan tenor sesuai kemampuan pembayaran",
        "Jaringan layanan yang luas",
    ),
    simulation = LoanSimulationUi(
        requestedAmount = "Rp100.000.000",
        selectedTenor = "36 bulan",
        estimatedInstallment = "Rp3.450.000",
    ),
)

@Preview(name = "Compact light", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Compact dark", widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Expanded", widthDp = 1000, heightDp = 700, showBackground = true)
@Preview(name = "Font 200%", widthDp = 360, heightDp = 800, fontScale = 2f, showBackground = true)
@Composable
private fun LoanProductDetailPreview() {
    AppTheme(darkTheme = isSystemInDarkTheme()) {
        LoanProductDetailScreen(state = previewLoanState(), onEvent = {})
    }
}
```

## 10. Review checklist studi kasus

- [ ] Metrics wrap pada width sempit dan tetap terbaca pada font scale 2.0.
- [ ] Bottom actions tidak tertutup navigation bar/IME.
- [ ] `isApplying` menonaktifkan kedua action dan mencegah double submit.
- [ ] Disabled `canApply` memiliki penjelasan di content, bukan hanya button abu-abu.
- [ ] Back dan share icon memiliki content description.
- [ ] Benefit check icon dekoratif karena teks membawa makna.
- [ ] Long product title tidak mendorong action keluar app bar.
- [ ] CTA tetap terlihat tetapi tidak menutupi last content item.
- [ ] Dark theme, contrast, RTL, localization, dan TalkBack diperiksa.
- [ ] UI test memverifikasi event `Apply`, `OpenSimulation`, `Share`, dan `Back`.

## 11. Alur akhir

```text
Design
  ↓
Audit state dan adaptive behavior
  ↓
Layout analysis (Scaffold + LazyColumn + FlowRow)
  ↓
Component mapping (Material 3 + app tokens)
  ↓
Reusable components (Hero, Metric, Benefits, Summary, Actions)
  ↓
Stateless LoanProductDetailScreen
  ↓
Route menghubungkan ViewModel/navigation/platform
  ↓
Preview matrix + semantics + UI test + visual QA
```

