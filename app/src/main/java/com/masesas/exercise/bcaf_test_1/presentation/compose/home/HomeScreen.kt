package com.masesas.exercise.bcaf_test_1.presentation.compose.home

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.component.ListStatusOverlay
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.component.LoadMoreEffect
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.component.LoadingRow
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.sharedActivityViewModel
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.loan.LoanProductUiState
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.loan.LoanProductViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: LoanProductViewModel = sharedActivityViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onLoadMore = viewModel::loadMore,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
private fun HomeScreen(
    uiState: LoanProductUiState,
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
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    text = stringResource(R.string.loan_product_section_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp),
                )
            }

            items(items = uiState.items, key = { it.id }) { product ->
                LoanProductItem(product = product)
            }

            if (uiState.isLoadingMore) {
                item { LoadingRow() }
            }
        }

        ListStatusOverlay(
            failure = uiState.blockingFailure,
            isEmpty = uiState.isEmpty,
            emptyMessage = stringResource(R.string.loan_product_empty),
            onRetry = onRefresh,
        )
    }
}
