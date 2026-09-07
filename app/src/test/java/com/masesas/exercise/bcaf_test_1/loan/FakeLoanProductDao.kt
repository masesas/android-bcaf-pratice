package com.masesas.exercise.bcaf_test_1.loan

import com.masesas.exercise.bcaf_test_1.data.loan.local.LoanProductDao
import com.masesas.exercise.bcaf_test_1.data.loan.local.LoanProductEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Pengganti Room di unit test; menirukan ORDER BY position + LIMIT. */
class FakeLoanProductDao : LoanProductDao {

    private val rows = MutableStateFlow<List<LoanProductEntity>>(emptyList())

    val current: List<LoanProductEntity> get() = rows.value

    override fun observePaged(limit: Int): Flow<List<LoanProductEntity>> =
        rows.map { list -> list.sortedBy { it.position }.take(limit) }

    override suspend fun count(): Int = rows.value.size

    override suspend fun upsertAll(items: List<LoanProductEntity>) {
        val byId = rows.value.associateBy { it.id } + items.associateBy { it.id }
        rows.value = byId.values.sortedBy { it.position }
    }

    override suspend fun clear() {
        rows.value = emptyList()
    }

    override suspend fun replaceAll(items: List<LoanProductEntity>) {
        clear()
        upsertAll(items)
    }
}
