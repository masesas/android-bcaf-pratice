package com.masesas.exercise.bcaf_test_1.presentation.compose.transaction

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanApplication
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanApplicationStatus
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.component.ListStatusOverlay
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.component.LoadMoreEffect
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.component.LoadingRow
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.theme.MyBcafTest1Theme
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.loan.LoanApplicationUiState
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.loan.LoanApplicationViewModel
import java.math.BigDecimal

/**
 * Daftar pengajuan pinjaman.
 *
 * [LoanApplicationViewModel] sengaja di-scope ke NavBackStackEntry lewat `hiltViewModel()`,
 * bukan ke Activity: state-nya hanya milik layar ini dan ikut mati bersamanya.
 */
@Composable
fun TransactionScreen(
    onOpenTransactionDetail: (transactionId: String) -> Unit,
    modifier: Modifier = Modifier,
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
    LoadMoreEffect(listState = listState, enabled = uiState.hasNextPage, onLoadMore = onLoadMore)

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
                    onClick = { onOpenTransactionDetail(application.id.toString()) },
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
