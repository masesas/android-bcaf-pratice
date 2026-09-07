package com.masesas.exercise.bcaf_test_1.presentation.compose.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component.AppSectionHeader
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.theme.Spacing
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.component.ListStatusOverlay
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.component.LoadMoreEffect
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.component.LoadingRow
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.sharedActivityViewModel
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.loan.LoanProductUiState
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.loan.LoanProductViewModel

@Composable
fun HomeScreen(
    onOpenMenu: (HomeMenu) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoanProductViewModel = sharedActivityViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onOpenMenu = onOpenMenu,
        onLoadMore = viewModel::loadMore,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
private fun HomeScreen(
    uiState: LoanProductUiState,
    onOpenMenu: (HomeMenu) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LoadMoreEffect(listState = listState, enabled = uiState.hasNextPage, onLoadMore = onLoadMore)

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            item(key = "header") { HomeHeader() }
            item(key = "menu") { HomeMenuGrid(onOpenMenu = onOpenMenu) }
            item(key = "products_title") {
                AppSectionHeader(title = stringResource(R.string.loan_product_section_title))
                Text(
                    text = stringResource(R.string.home_products_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
            items(items = uiState.items, key = { it.id }) { product ->
                LoanProductItem(product = product)
            }
            if (uiState.InitialLoading || uiState.isLoadingMore) {
                item(key = "loading") { LoadingRow() }
            } else if (uiState.blockingFailure != null || uiState.isEmpty) {
                item(key = "status") {
                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp)) {
                        ListStatusOverlay(
                            failure = uiState.blockingFailure,
                            isEmpty = uiState.isEmpty,
                            emptyMessage = stringResource(R.string.loan_product_empty),
                            onRetry = onRefresh,
                        )
                    }
                }
            }
        }
    }
}
