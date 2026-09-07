package com.masesas.exercise.bcaf_test_1.data.loan.repository

import com.masesas.exercise.bcaf_test_1.core.network.requirePayload
import com.masesas.exercise.bcaf_test_1.core.network.runApiCatching
import com.masesas.exercise.bcaf_test_1.data.loan.local.LoanProductDao
import com.masesas.exercise.bcaf_test_1.data.loan.mapper.toDomain
import com.masesas.exercise.bcaf_test_1.data.loan.mapper.toEntity
import com.masesas.exercise.bcaf_test_1.data.loan.remote.LoanProductApi
import com.masesas.exercise.bcaf_test_1.domain.common.AppResult
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanProduct
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanProductPage
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanProductQuery
import com.masesas.exercise.bcaf_test_1.domain.loan.repository.LoanProductRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class LoanProductRepositoryImpl internal constructor(
    private val dao: LoanProductDao,
    private val api: LoanProductApi,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LoanProductRepository {

    override fun observeLoanProducts(limit: Int): Flow<List<LoanProduct>> =
        dao.observePaged(limit).map { entities -> entities.map { it.toDomain() } }

    override suspend fun refresh(query: LoanProductQuery): AppResult<LoanProductPage> =
        withContext(ioDispatcher) {
            runApiCatching(json) {
                val envelope = api.getLoanProducts(
                    page = query.page,
                    size = query.size,
                    sort = query.sort,
                )

                val payload = envelope.requirePayload()
                if (payload is AppResult.Failure) return@runApiCatching payload

                val items = (payload as AppResult.Success).data
                val offset = query.page * query.size
                val entities = items.mapIndexed { index, dto -> dto.toEntity(offset + index) }

                if (query.page == FIRST_PAGE) dao.replaceAll(entities) else dao.upsertAll(entities)

                val meta = envelope.meta
                AppResult.success(
                    LoanProductPage(
                        page = meta?.page ?: query.page,
                        size = meta?.size ?: query.size,
                        totalElements = meta?.totalElements ?: entities.size.toLong(),
                        totalPages = meta?.totalPages ?: 1,
                        items = entities.map { it.toDomain() },
                    )
                )
            }
        }

    override suspend fun cachedCount(): Int = withContext(ioDispatcher) { dao.count() }

    override suspend fun clearCache() = withContext(ioDispatcher) { dao.clear() }

    private companion object {
        const val FIRST_PAGE = 0
    }
}
