package com.masesas.exercise.bcaf_test_1.data.auth.remote

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String,
)

@Serializable
data class LoginResponseDto(
    val token: String,
    val tipe: String? = null,
    val roles: List<String> = emptyList(),
)
