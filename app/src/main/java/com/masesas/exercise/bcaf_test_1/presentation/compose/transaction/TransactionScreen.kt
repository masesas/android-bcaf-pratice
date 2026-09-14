package com.masesas.exercise.bcaf_test_1.presentation.compose.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanApplication
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanApplicationStatus
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.component.ListStatusOverlay
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.component.LoadMoreEffect
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.component.LoadingRow
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.theme.MyBcafTest1Theme
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.loan.LoanApplicationUiState
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.loan.LoanApplicationViewModel
import kotlinx.coroutines.delay
import java.math.BigDecimal


@Composable
fun TransactionScreen(
    modifier: Modifier = Modifier,
    onOpenTransactionDetail: (transactionId: String) -> Unit = {},
    viewModel: LoanApplicationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TransactionScreen(
        uiState = uiState,
        onOpenTransactionDetail = onOpenTransactionDetail,
        onLoadMore = viewModel::loadMore,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
private fun TransactionScreen(
    uiState: LoanApplicationUiState,
    onOpenTransactionDetail: (transactionId: String) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    var mockLoading by remember {
        mutableStateOf(false)
    }

    var selectedTransactionId by remember {
        mutableStateOf<String?>(null)
    }


    LoadMoreEffect(listState = listState, enabled = uiState.hasNextPage, onLoadMore = onLoadMore)

    LaunchedEffect(mockLoading, selectedTransactionId) {
        if (!mockLoading) return@LaunchedEffect

        val transactionId = selectedTransactionId
            ?: return@LaunchedEffect

        delay(2000)

        mockLoading = false
        selectedTransactionId = null

        onOpenTransactionDetail(transactionId)
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    text = stringResource(R.string.loan_application_section_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp),
                )
            }

            items(items = uiState.items, key = { it.id }) { application ->
                LoanApplicationItem(
                    application = application,
                    onClick = {
                        selectedTransactionId = application.id.toString()
                        mockLoading = true
                    },
                )
            }

            if (uiState.isLoadingMore) {
                item { LoadingRow() }
            }
        }

        ListStatusOverlay(
            failure = uiState.blockingFailure,
            isEmpty = uiState.isEmpty,
            emptyMessage = stringResource(R.string.loan_application_empty),
            onRetry = onRefresh,
        )

        if (mockLoading) {
            FullScreenLoading()
        }
    }
}

@Preview
@Composable
private fun TransactionScreenPreview() {
    MyBcafTest1Theme {
        TransactionScreen(
            uiState = LoanApplicationUiState(items = previewApplications()),
            onOpenTransactionDetail = {},
            onLoadMore = {},
            onRefresh = {},
        )
    }
}

@Composable
private fun FullScreenLoading(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)
            ),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

private fun previewApplications() = listOf(
    previewApplication(1, LoanApplicationStatus.SUBMITTED, "Menunggu verifikasi dokumen"),
    previewApplication(2, LoanApplicationStatus.APPROVED, null),
    previewApplication(3, LoanApplicationStatus.REJECTED, "Skor kredit tidak memenuhi"),
)

private fun previewApplication(id: Long, status: LoanApplicationStatus, note: String?) =
    LoanApplication(
        id = id,
        customerId = 1,
        customerName = "Budi",
        loanProductId = 1,
        loanProductCode = "KTA-00$id",
        branchId = 1,
        branchCode = "JKT",
        submittedAmount = BigDecimal("10000000"),
        tenorMonths = 12,
        status = status,
        note = note,
        version = 0,
        createdAt = null,
        updatedAt = null,
    )
