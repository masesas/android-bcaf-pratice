package com.masesas.exercise.bcaf_test_1.data.notification.remote

import kotlinx.serialization.Serializable

@Serializable
data class DeviceRegistrationRequestDto(
    val installationId: String,
    val platform: String = "android",
)
