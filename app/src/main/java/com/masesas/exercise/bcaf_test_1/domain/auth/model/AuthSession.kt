package com.masesas.exercise.bcaf_test_1.domain.auth.model

data class AuthSession(
    val user: AuthUser? = null,
    val accessToken: String,
    val expiresAtMillis: Long,
) {
    fun isExpiredAt(nowMillis: Long): Boolean = nowMillis >= expiresAtMillis
}
