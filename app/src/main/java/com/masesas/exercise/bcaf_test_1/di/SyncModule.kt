package com.masesas.exercise.bcaf_test_1.di

import com.masesas.exercise.bcaf_test_1.data.sync.repository.PendingUploadRepositoryImpl
import com.masesas.exercise.bcaf_test_1.domain.sync.repository.PendingUploadRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindPendingUploadRepository(
        impl: PendingUploadRepositoryImpl,
    ): PendingUploadRepository
}
