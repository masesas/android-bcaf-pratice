package com.masesas.exercise.bcaf_test_1.loan

import com.masesas.exercise.bcaf_test_1.data.loan.local.LoanApplicationDao
import com.masesas.exercise.bcaf_test_1.data.loan.local.LoanApplicationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Pengganti Room di unit test; menirukan ORDER BY id ASC tanpa limit. */
class FakeLoanApplicationDao : LoanApplicationDao {

    private val rows = MutableStateFlow<List<LoanApplicationEntity>>(emptyList())

    override fun observeAll(): Flow<List<LoanApplicationEntity>> =
        rows.map { list -> list.sortedBy { it.id } }

    override fun observeById(id: Long): Flow<LoanApplicationEntity?> =
        rows.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun upsert(item: LoanApplicationEntity) = upsertAll(listOf(item))

    override suspend fun upsertAll(items: List<LoanApplicationEntity>) {
        val byId = rows.value.associateBy { it.id } + items.associateBy { it.id }
        rows.value = byId.values.sortedBy { it.id }
    }

    override suspend fun clear() {
        rows.value = emptyList()
    }

    override suspend fun replaceAll(items: List<LoanApplicationEntity>) {
        clear()
        upsertAll(items)
    }
}
