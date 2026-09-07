package com.masesas.exercise.bcaf_test_1.loan

import com.masesas.exercise.bcaf_test_1.core.network.ApiEnvelope
import com.masesas.exercise.bcaf_test_1.core.network.ApiMetaDto
import com.masesas.exercise.bcaf_test_1.data.loan.remote.LoanApplicationApi
import com.masesas.exercise.bcaf_test_1.data.loan.remote.LoanApplicationDto
import java.io.IOException
import java.math.BigDecimal

/** Server palsu berisi [totalElements] pengajuan, dipotong sesuai page/size yang diminta. */
class FakeLoanApplicationApi(private val totalElements: Int) : LoanApplicationApi {

    var failWith: Throwable? = null

    val requestedPages = mutableListOf<Int>()
    val lastPage: Int? get() = requestedPages.lastOrNull()

    override suspend fun getLoanApplications(
        page: Int,
        size: Int,
        sort: String,
    ): ApiEnvelope<List<LoanApplicationDto>> {
        failWith?.let { throw it }
        requestedPages += page

        val from = page * size
        val until = minOf(from + size, totalElements)
        val items = (from until until).map { index -> dtoAt(index) }

        return ApiEnvelope(
            statusCode = 200,
            data = items,
            meta = ApiMetaDto(
                page = page,
                size = size,
                totalElements = totalElements.toLong(),
                totalPages = (totalElements + size - 1) / size,
            ),
        )
    }

    override suspend fun getLoanApplication(id: Long): ApiEnvelope<LoanApplicationDto> {
        failWith?.let { throw it }
        return ApiEnvelope(statusCode = 200, data = dtoAt(id.toInt()), meta = null)
    }

    private fun dtoAt(index: Int) = LoanApplicationDto(
        id = index.toLong(),
        idCustomer = 1,
        namaCustomer = "Customer $index",
        idLoanProduct = 1,
        kodeLoanProduct = "P1",
        idBranch = 1,
        kodeBranch = "B1",
        jumlahPengajuan = BigDecimal("10000000"),
        tenorBulan = 12,
        status = "SUBMITTED",
        version = 0,
    )

    companion object {
        fun offline() = IOException("offline")
    }
}
