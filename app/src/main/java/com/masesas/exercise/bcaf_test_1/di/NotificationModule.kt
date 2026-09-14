package com.masesas.exercise.bcaf_test_1.di

import android.content.Context
import com.masesas.exercise.bcaf_test_1.core.network.AuthTokenProvider
import com.masesas.exercise.bcaf_test_1.core.notification.AndroidAppNotifier
import com.masesas.exercise.bcaf_test_1.core.notification.AppNotifier
import com.masesas.exercise.bcaf_test_1.data.notification.local.DeviceRegistrationLocalDataSource
import com.masesas.exercise.bcaf_test_1.data.notification.remote.DeviceRegistrationApi
import com.masesas.exercise.bcaf_test_1.data.notification.remote.FirebaseMessagingClient
import com.masesas.exercise.bcaf_test_1.data.notification.repository.DeviceRegistrationRepositoryImpl
import com.masesas.exercise.bcaf_test_1.domain.notification.repository.DeviceRegistrationRepository
import com.masesas.exercise.bcaf_test_1.presentation.compose.HomeActivityCompose
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {

    @Provides
    @Singleton
    fun provideAppNotifier(
        @ApplicationContext context: Context,
    ): AppNotifier = AndroidAppNotifier(
        context = context,
        target = HomeActivityCompose::class.java,
    )

    @Provides
    @Singleton
    fun provideDeviceRegistrationLocalDataSource(
        @ApplicationContext context: Context,
    ): DeviceRegistrationLocalDataSource = DeviceRegistrationLocalDataSource(context)

    @Provides
    @Singleton
    fun provideFirebaseMessagingClient(): FirebaseMessagingClient = FirebaseMessagingClient()

    @Provides
    @Singleton
    fun provideDeviceRegistrationRepository(
        api: DeviceRegistrationApi,
        localDataSource: DeviceRegistrationLocalDataSource,
        messagingClient: FirebaseMessagingClient,
        authTokenProvider: AuthTokenProvider,
        json: Json,
    ): DeviceRegistrationRepository = DeviceRegistrationRepositoryImpl(
        api = api,
        localDataSource = localDataSource,
        messagingClient = messagingClient,
        authTokenProvider = authTokenProvider,
        json = json,
    )
}
