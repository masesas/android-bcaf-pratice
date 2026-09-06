package com.masesas.exercise.bcaf_test_1.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.masesas.exercise.bcaf_test_1.BuildConfig
import com.masesas.exercise.bcaf_test_1.core.network.AuthHeaderInterceptor
import com.masesas.exercise.bcaf_test_1.core.network.AuthTokenProvider
import com.masesas.exercise.bcaf_test_1.data.auth.local.AuthSessionLocalDataSource
import com.masesas.exercise.bcaf_test_1.data.auth.local.SessionAuthTokenProvider
import com.masesas.exercise.bcaf_test_1.data.auth.remote.AuthApi
import com.masesas.exercise.bcaf_test_1.data.loan.remote.LoanApplicationApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val TIMEOUT_SECONDS = 30L

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideAuthTokenProvider(
        localDataSource: AuthSessionLocalDataSource,
    ): AuthTokenProvider = SessionAuthTokenProvider(localDataSource)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        tokenProvider: AuthTokenProvider,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(AuthHeaderInterceptor(tokenProvider))
        // Di build release, artifact chucker-no-op membuat interceptor ini tidak melakukan apa pun.
        .addInterceptor(ChuckerInterceptor.Builder(context).build())
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideLoanApplicationApi(retrofit: Retrofit): LoanApplicationApi =
        retrofit.create(LoanApplicationApi::class.java)
}
