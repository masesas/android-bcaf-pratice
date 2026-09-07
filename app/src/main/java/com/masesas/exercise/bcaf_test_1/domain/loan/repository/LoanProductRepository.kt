package com.masesas.exercise.bcaf_test_1.domain.loan.repository

import com.masesas.exercise.bcaf_test_1.domain.common.AppResult
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanProduct
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanProductPage
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanProductQuery
import kotlinx.coroutines.flow.Flow

/**
 * Offline-first: [observeLoanProducts] membaca cache lokal sebagai sumber kebenaran,
 * [refresh] mengambil satu halaman dari jaringan lalu menulis cache tersebut.
 */
interface LoanProductRepository {

    /** Cache dibaca berhalaman: [limit] = jumlah halaman yang sudah dimuat x ukuran halaman. */
    fun observeLoanProducts(limit: Int): Flow<List<LoanProduct>>

    suspend fun refresh(query: LoanProductQuery = LoanProductQuery()): AppResult<LoanProductPage>

    /** Dipakai saat cold start offline untuk memulihkan berapa halaman yang sudah tersimpan. */
    suspend fun cachedCount(): Int

    suspend fun clearCache()
}
