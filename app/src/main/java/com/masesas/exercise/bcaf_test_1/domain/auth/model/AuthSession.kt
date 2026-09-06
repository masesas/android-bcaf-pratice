package com.masesas.exercise.bcaf_test_1.domain.auth.model

/**
 * Session hasil autentikasi: siapa user-nya + kredensial akses yang berlaku sampai [expiresAtMillis].
 */
data class AuthSession(
    val user: AuthUser,
    val accessToken: String,
    val expiresAtMillis: Long,
) {
    fun isExpiredAt(nowMillis: Long): Boolean = nowMillis >= expiresAtMillis
}
