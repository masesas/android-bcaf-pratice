package com.masesas.exercise.bcaf_test_1.domain.loan.model

import java.math.BigDecimal
import java.time.Instant

/** Satu pengajuan pinjaman. Nama field mengikuti domain, bukan penamaan API. */
data class LoanApplication(
    val id: Long,
    val customerId: Long,
    val customerName: String,
    val loanProductId: Long,
    val loanProductCode: String,
    val branchId: Long,
    val branchCode: String,
    val submittedAmount: BigDecimal,
    val tenorMonths: Int,
    val status: LoanApplicationStatus,
    val note: String?,
    val version: Long,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)
