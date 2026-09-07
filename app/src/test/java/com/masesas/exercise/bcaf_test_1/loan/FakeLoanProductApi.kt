package com.masesas.exercise.bcaf_test_1.loan

import com.masesas.exercise.bcaf_test_1.core.network.ApiEnvelope
import com.masesas.exercise.bcaf_test_1.core.network.ApiMetaDto
import com.masesas.exercise.bcaf_test_1.data.loan.remote.LoanProductApi
import com.masesas.exercise.bcaf_test_1.data.loan.remote.LoanProductDto
import java.io.IOException
import java.math.BigDecimal

/** Server palsu berisi [totalElements] produk, dipotong sesuai page/size yang diminta. */
class FakeLoanProductApi(private val totalElements: Int) : LoanProductApi {

    var failWith: Throwable? = null

    val requestedPages = mutableListOf<Int>()
    val lastPage: Int? get() = requestedPages.lastOrNull()

    override suspend fun getLoanProducts(
        page: Int,
        size: Int,
        sort: List<String>,
    ): ApiEnvelope<List<LoanProductDto>> {
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

    private fun dtoAt(index: Int) = LoanProductDto(
        id = index.toLong(),
        kode = "P$index",
        nama = "Produk $index",
        bungaPersen = 12.5,
        tenorMin = 6,
        tenorMax = 36,
        plafondMin = BigDecimal("5000000"),
        plafondMax = BigDecimal("50000000"),
        aktif = true,
    )

    companion object {
        fun offline() = IOException("offline")
    }
}
