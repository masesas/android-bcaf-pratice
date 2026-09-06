package com.masesas.exercise.bcaf_test_1.domain.loan.model

enum class LoanApplicationStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED,
    DISBURSED,
    CANCELLED,

    /** Status yang belum dikenal klien; nilai mentahnya tetap tersimpan di entity. */
    UNKNOWN;

    companion object {
        fun fromApi(raw: String?): LoanApplicationStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: UNKNOWN
    }
}
