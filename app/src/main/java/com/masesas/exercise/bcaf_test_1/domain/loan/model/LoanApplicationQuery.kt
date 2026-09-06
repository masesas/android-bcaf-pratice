package com.masesas.exercise.bcaf_test_1.domain.loan.model

/** Parameter paging untuk endpoint loan application. */
data class LoanApplicationQuery(
    val page: Int = DEFAULT_PAGE,
    val size: Int = DEFAULT_SIZE,
    val sort: String = "",
) {
    companion object {
        const val DEFAULT_PAGE = 0
        const val DEFAULT_SIZE = 20
    }
}
