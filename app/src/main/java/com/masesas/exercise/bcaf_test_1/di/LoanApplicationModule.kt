package com.masesas.exercise.bcaf_test_1.di

import com.masesas.exercise.bcaf_test_1.data.loan.local.LoanApplicationDao
import com.masesas.exercise.bcaf_test_1.data.loan.remote.LoanApplicationApi
import com.masesas.exercise.bcaf_test_1.data.loan.repository.LoanApplicationRepositoryImpl
import com.masesas.exercise.bcaf_test_1.domain.loan.repository.LoanApplicationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LoanApplicationModule {

    @Provides
    @Singleton
    fun provideLoanApplicationRepository(
        dao: LoanApplicationDao,
        api: LoanApplicationApi,
        json: Json,
    ): LoanApplicationRepository = LoanApplicationRepositoryImpl(
        dao = dao,
        api = api,
        json = json,
    )
}
