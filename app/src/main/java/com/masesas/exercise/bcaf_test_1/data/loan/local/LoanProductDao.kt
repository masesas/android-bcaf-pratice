package com.masesas.exercise.bcaf_test_1.data.loan.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanProductDao {

    /** Paginasi di sisi Room: UI hanya membaca sebanyak halaman yang sudah dimuat. */
    @Query("SELECT * FROM loan_product ORDER BY position ASC LIMIT :limit")
    fun observePaged(limit: Int): Flow<List<LoanProductEntity>>

    @Query("SELECT COUNT(*) FROM loan_product")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertAll(items: List<LoanProductEntity>)

    @Query("DELETE FROM loan_product")
    suspend fun clear()

    /** Dipakai saat memuat halaman pertama agar entri yang sudah dihapus server ikut hilang. */
    @Transaction
    suspend fun replaceAll(items: List<LoanProductEntity>) {
        clear()
        upsertAll(items)
    }
}
