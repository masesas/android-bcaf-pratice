package com.masesas.exercise.bcaf_test_1.data.loan.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanApplicationDao {

    @Query("SELECT * FROM loan_application ORDER BY id ASC")
    fun observeAll(): Flow<List<LoanApplicationEntity>>

    @Query("SELECT * FROM loan_application WHERE id = :id")
    fun observeById(id: Long): Flow<LoanApplicationEntity?>

    @Upsert
    suspend fun upsert(item: LoanApplicationEntity)

    @Upsert
    suspend fun upsertAll(items: List<LoanApplicationEntity>)

    @Query("DELETE FROM loan_application")
    suspend fun clear()

    /** Dipakai saat memuat halaman pertama agar entri yang sudah dihapus server ikut hilang. */
    @Transaction
    suspend fun replaceAll(items: List<LoanApplicationEntity>) {
        clear()
        upsertAll(items)
    }
}
