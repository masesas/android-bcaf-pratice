package com.masesas.exercise.bcaf_test_1.data.loan.repository

import com.masesas.exercise.bcaf_test_1.core.network.requirePayload
import com.masesas.exercise.bcaf_test_1.core.network.runApiCatching
import com.masesas.exercise.bcaf_test_1.data.loan.local.LoanApplicationDao
import com.masesas.exercise.bcaf_test_1.data.loan.mapper.toDomain
import com.masesas.exercise.bcaf_test_1.data.loan.mapper.toEntity
import com.masesas.exercise.bcaf_test_1.data.loan.remote.LoanApplicationApi
import com.masesas.exercise.bcaf_test_1.domain.common.AppResult
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanApplication
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanApplicationPage
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanApplicationQuery
import com.masesas.exercise.bcaf_test_1.domain.loan.repository.LoanApplicationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class LoanApplicationRepositoryImpl internal constructor(
    private val dao: LoanApplicationDao,
    private val api: LoanApplicationApi,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LoanApplicationRepository {

    override fun observeLoanApplications(): Flow<List<LoanApplication>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeLoanApplication(id: Long): Flow<LoanApplication?> =
        dao.observeById(id).map { entity -> entity?.toDomain() }

    override suspend fun refresh(query: LoanApplicationQuery): AppResult<LoanApplicationPage> =
        withContext(ioDispatcher) {
            runApiCatching(json) {
                val envelope = api.getLoanApplications(
                    page = query.page,
                    size = query.size,
                    sort = query.sort,
                )

                val payload = envelope.requirePayload()
                if (payload is AppResult.Failure) return@runApiCatching payload

                val items = (payload as AppResult.Success).data
                val entities = items.map { it.toEntity() }

                if (query.page == FIRST_PAGE) dao.replaceAll(entities) else dao.upsertAll(entities)

                val meta = envelope.meta
                AppResult.success(
                    LoanApplicationPage(
                        page = meta?.page ?: query.page,
                        size = meta?.size ?: query.size,
                        totalElements = meta?.totalElements ?: entities.size.toLong(),
                        totalPages = meta?.totalPages ?: 1,
                        items = entities.map { it.toDomain() },
                    )
                )
            }
        }

    override suspend fun refreshLoanApplication(id: Long): AppResult<LoanApplication> =
        withContext(ioDispatcher) {
            runApiCatching(json) {
                val envelope = api.getLoanApplication(id)

                when (val payload = envelope.requirePayload()) {
                    is AppResult.Failure -> payload
                    is AppResult.Success -> {
                        val entity = payload.data.toEntity()
                        dao.upsert(entity)
                        AppResult.success(entity.toDomain())
                    }
                }
            }
        }

    override suspend fun clearCache() = withContext(ioDispatcher) { dao.clear() }

    private companion object {
        const val FIRST_PAGE = 0
    }
}
