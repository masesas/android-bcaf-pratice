package com.masesas.exercise.bcaf_test_1.data.loan.remote

import com.masesas.exercise.bcaf_test_1.core.network.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class LoanProductDto(
    val id: Long,
    val kode: String = "",
    val nama: String = "",
    val bungaPersen: Double = 0.0,
    val tenorMin: Int = 0,
    val tenorMax: Int = 0,
    @Serializable(with = BigDecimalSerializer::class)
    val plafondMin: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val plafondMax: BigDecimal = BigDecimal.ZERO,
    val aktif: Boolean = false,
    val createdDate: String? = null,
    val updatedDate: String? = null,
)
