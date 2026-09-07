package com.masesas.exercise.bcaf_test_1.presentation.viewmodel.loan

import com.masesas.exercise.bcaf_test_1.domain.common.AppFailure
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanProduct

data class LoanProductUiState(
    val items: List<LoanProduct> = emptyList(),
    val InitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNextPage: Boolean = false,
    val failure: AppFailure? = null,
) {
    val isEmpty: Boolean get() = items.isEmpty() && !isRefreshing

    /** Kegagalan hanya ditonjolkan saat tidak ada data cache yang bisa ditampilkan. */
    val blockingFailure: AppFailure? get() = failure.takeIf { items.isEmpty() }
}
