package com.masesas.exercise.bcaf_test_1.domain.loan.repository

import com.masesas.exercise.bcaf_test_1.domain.common.AppResult
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanApplication
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanApplicationPage
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanApplicationQuery
import kotlinx.coroutines.flow.Flow

/**
 * Offline-first: fungsi `observe*` membaca cache lokal sebagai sumber kebenaran,
 * fungsi `refresh*` mengambil dari jaringan lalu menulis cache tersebut.
 */
interface LoanApplicationRepository {

    fun observeLoanApplications(): Flow<List<LoanApplication>>

    fun observeLoanApplication(id: Long): Flow<LoanApplication?>

    suspend fun refresh(query: LoanApplicationQuery = LoanApplicationQuery()): AppResult<LoanApplicationPage>

    /** Mengambil satu pengajuan dari server dan memperbarui barisnya di cache. */
    suspend fun refreshLoanApplication(id: Long): AppResult<LoanApplication>

    suspend fun clearCache()
}
