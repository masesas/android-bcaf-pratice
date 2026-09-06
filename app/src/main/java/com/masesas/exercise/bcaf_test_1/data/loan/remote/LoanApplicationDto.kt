package com.masesas.exercise.bcaf_test_1.data.loan.remote

import com.masesas.exercise.bcaf_test_1.core.network.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class LoanApplicationDto(
    val id: Long,
    val idCustomer: Long = 0,
    val namaCustomer: String = "",
    val idLoanProduct: Long = 0,
    val kodeLoanProduct: String = "",
    val idBranch: Long = 0,
    val kodeBranch: String = "",
    @Serializable(with = BigDecimalSerializer::class)
    val jumlahPengajuan: BigDecimal = BigDecimal.ZERO,
    val tenorBulan: Int = 0,
    val status: String? = null,
    val catatan: String? = null,
    val version: Long = 0,
    val createdDate: String? = null,
    val updatedDate: String? = null,
)
