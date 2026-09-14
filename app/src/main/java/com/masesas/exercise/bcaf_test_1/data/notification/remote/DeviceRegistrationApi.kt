package com.masesas.exercise.bcaf_test_1.data.notification.remote

import com.masesas.exercise.bcaf_test_1.core.network.ApiEnvelope
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.POST

interface DeviceRegistrationApi {

    @POST("api/notification/device")
    suspend fun register(@Body body: DeviceRegistrationRequestDto): ApiEnvelope<Unit>

    @HTTP(method = "DELETE", path = "api/notification/device", hasBody = true)
    suspend fun unregister(@Body body: DeviceRegistrationRequestDto): ApiEnvelope<Unit>
}
