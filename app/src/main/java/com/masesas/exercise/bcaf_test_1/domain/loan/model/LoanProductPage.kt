package com.masesas.exercise.bcaf_test_1.domain.loan.model

/** Metadata paging yang dikembalikan server bersama daftar loan product. */
data class LoanProductPage(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val items: List<LoanProduct>,
) {
    val hasNextPage: Boolean get() = page + 1 < totalPages
}
