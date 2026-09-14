package com.masesas.exercise.bcaf_test_1.presentation.viewmodel.loan

import com.masesas.exercise.bcaf_test_1.domain.common.AppFailure
import com.masesas.exercise.bcaf_test_1.domain.common.CommonFailure
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanProduct

data class LoanProductUiState(
    val items: List<LoanProduct> = emptyList(),
    val initialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNextPage: Boolean = false,
    val failure: AppFailure? = null,
) {
    val isEmpty: Boolean get() = items.isEmpty() && !isRefreshing && !initialLoading

    /** Kegagalan hanya ditonjolkan saat tidak ada data cache yang bisa ditampilkan. */
    val blockingFailure: AppFailure? get() = failure.takeIf { items.isEmpty() }

    val errorMessage: String get() = when (failure) {
        is CommonFailure.ApiError -> {
            failure.details.joinToString(", ").ifBlank {
                failure.message ?: "Terjadi kesalahan pada API"
            }
        }
        is CommonFailure.Unauthorized -> "Sesi telah berakhir, silakan login kembali."
        is CommonFailure.Network -> "Koneksi internet bermasalah."
        is CommonFailure.Unexpected -> "Terjadi kesalahan tidak terduga."
        else -> "Loan product gagal di muat"
    }
}
