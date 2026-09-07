package com.masesas.exercise.bcaf_test_1.domain.loan.model

/** Parameter paging untuk endpoint loan product; mengikuti skema Pageable server. */
data class LoanProductQuery(
    val page: Int = DEFAULT_PAGE,
    val size: Int = DEFAULT_SIZE,
    val sort: List<String> = emptyList(),
) {
    companion object {
        const val DEFAULT_PAGE = 0
        const val DEFAULT_SIZE = 20
    }
}
