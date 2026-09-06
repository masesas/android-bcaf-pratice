package com.masesas.exercise.bcaf_test_1.di

import android.content.Context
import com.masesas.exercise.bcaf_test_1.data.auth.local.AuthSessionLocalDataSource
import com.masesas.exercise.bcaf_test_1.data.auth.mock.MockAuthApi
import com.masesas.exercise.bcaf_test_1.data.auth.remote.AuthApi
import com.masesas.exercise.bcaf_test_1.data.auth.remote.JwtDecoder
import com.masesas.exercise.bcaf_test_1.data.auth.repository.AuthRepositoryImpl
import com.masesas.exercise.bcaf_test_1.domain.auth.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthSessionLocalDataSource(
        @ApplicationContext context: Context,
    ): AuthSessionLocalDataSource = AuthSessionLocalDataSource(context)

    @Provides
    @Singleton
    fun provideJwtDecoder(json: Json): JwtDecoder = JwtDecoder(json)

    @Provides
    @Singleton
    fun provideAuthRepository(
        localDataSource: AuthSessionLocalDataSource,
        authApi: AuthApi,
        jwtDecoder: JwtDecoder,
        json: Json,
    ): AuthRepository = AuthRepositoryImpl(
        localDataSource = localDataSource,
        remoteDataSource = authApi,
        mockDataSource = MockAuthApi(),
        jwtDecoder = jwtDecoder,
        json = json,
    )
}
