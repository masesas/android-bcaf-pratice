package com.masesas.exercise.bcaf_test_1.data.notification.repository

import android.util.Log
import com.masesas.exercise.bcaf_test_1.core.network.AuthTokenProvider
import com.masesas.exercise.bcaf_test_1.core.network.runApiCatching
import com.masesas.exercise.bcaf_test_1.data.notification.local.DeviceRegistrationLocalDataSource
import com.masesas.exercise.bcaf_test_1.data.notification.remote.DeviceRegistrationApi
import com.masesas.exercise.bcaf_test_1.data.notification.remote.DeviceRegistrationRequestDto
import com.masesas.exercise.bcaf_test_1.data.notification.remote.FirebaseMessagingClient
import com.masesas.exercise.bcaf_test_1.domain.common.AppResult
import com.masesas.exercise.bcaf_test_1.domain.common.onFailure
import com.masesas.exercise.bcaf_test_1.domain.common.onSuccess
import com.masesas.exercise.bcaf_test_1.domain.notification.model.NotificationTopic
import com.masesas.exercise.bcaf_test_1.domain.notification.repository.DeviceRegistrationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

private const val TAG = "DeviceRegistration"

/** Endpoint perangkat tidak mengembalikan payload, jadi keberhasilan dinilai dari status HTTP. */
class DeviceRegistrationRepositoryImpl(
    private val api: DeviceRegistrationApi,
    private val localDataSource: DeviceRegistrationLocalDataSource,
    private val messagingClient: FirebaseMessagingClient,
    private val authTokenProvider: AuthTokenProvider,
    private val json: Json,
) : DeviceRegistrationRepository {

    override suspend fun ensureRegistered() {
        logOnFailure("Registrasi FCM gagal") { messagingClient.register() }
    }

    override suspend fun syncInstallationId(installationId: String) {
        if (authTokenProvider.currentToken() == null) return
        if (localDataSource.syncedInstallationId() == installationId) return

        sendRegistration(installationId)
            .onSuccess { localDataSource.save(installationId) }
            .onFailure { Log.w(TAG, "Pengiriman installation id ke backend gagal: $it") }
    }

    override fun subscribe(topics: Set<NotificationTopic>) {
        //
        topics.forEach { messagingClient.subscribe(it.id) }
    }

    override fun unsubscribe(topics: Set<NotificationTopic>) {
        topics.forEach { messagingClient.unsubscribe(it.id) }
    }

    override suspend fun unregister() {
        unsubscribe(NotificationTopic.entries.toSet())

        localDataSource.syncedInstallationId()?.let { installationId ->
            sendUnregistration(installationId)
                .onFailure { Log.w(TAG, "Pelepasan perangkat di backend gagal: $it") }
        }

        logOnFailure("Unregister FCM gagal") { messagingClient.unregister() }
        localDataSource.clear()
    }

    private suspend fun sendRegistration(installationId: String): AppResult<Unit> =
        runApiCatching(json) {
            api.register(DeviceRegistrationRequestDto(installationId))
            AppResult.success(Unit)
        }

    private suspend fun sendUnregistration(installationId: String): AppResult<Unit> =
        runApiCatching(json) {
            api.unregister(DeviceRegistrationRequestDto(installationId))
            AppResult.success(Unit)
        }

    private suspend fun logOnFailure(message: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Log.w(TAG, message, throwable)
        }
    }
}
