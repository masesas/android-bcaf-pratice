package com.masesas.exercise.bcaf_test_1.di

import com.masesas.exercise.bcaf_test_1.data.loan.local.LoanProductDao
import com.masesas.exercise.bcaf_test_1.data.loan.remote.LoanProductApi
import com.masesas.exercise.bcaf_test_1.data.loan.repository.LoanProductRepositoryImpl
import com.masesas.exercise.bcaf_test_1.domain.loan.repository.LoanProductRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LoanProductModule {

    @Provides
    @Singleton
    fun provideLoanProductRepository(
        dao: LoanProductDao,
        api: LoanProductApi,
        json: Json,
    ): LoanProductRepository = LoanProductRepositoryImpl(
        dao = dao,
        api = api,
        json = json,
    )
}
