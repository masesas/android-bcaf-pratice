package com.masesas.exercise.bcaf_test_1.domain.loan.model

import java.math.BigDecimal
import java.time.Instant

/** Satu produk pinjaman. Nama field mengikuti domain, bukan penamaan API. */
data class LoanProduct(
    val id: Long,
    val code: String,
    val name: String,
    val interestPercent: Double,
    val tenorMinMonths: Int,
    val tenorMaxMonths: Int,
    val plafondMin: BigDecimal,
    val plafondMax: BigDecimal,
    val isActive: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)
