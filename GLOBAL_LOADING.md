```kotlin
class GlobalUiViewModel : ViewModel() {

    private val _loadingCount = MutableStateFlow(0)

    val isLoading = _loadingCount
        .map { it > 0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    fun showLoading() {
        _loadingCount.update { it + 1 }
    }

    fun hideLoading() {
        _loadingCount.update {
            (it - 1).coerceAtLeast(0)
        }
    }
}

// COMPOSITION
val LocalGlobalLoading = staticCompositionLocalOf<GlobalLoadingController> {
    error("GlobalLoadingController not provided")
}

// USAGE ROOT
@Composable
fun App(
    globalUiViewModel: GlobalUiViewModel = viewModel(),
) {
    val uiState by globalUiViewModel.uiState
        .collectAsStateWithLifecycle()

    val loadingController = remember(globalUiViewModel) {
        DefaultGlobalLoadingController(globalUiViewModel)
    }

    CompositionLocalProvider(
        LocalGlobalLoading provides loadingController,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            AppNavHost()

            if (uiState.isLoading) {
                GlobalLoadingOverlay()
            }
        }
    }
}

// USAGE IMPL
val globalLoading = LocalGlobalLoading.current

LaunchedEffect(pendingTransactionId) {
    val transactionId = pendingTransactionId
        ?: return@LaunchedEffect

    globalLoading.show()

    try {
        delay(2_000)

        onOpenTransactionDetail(transactionId)
    } finally {
        globalLoading.hide()
    }
}
```