package com.masesas.exercise.bcaf_test_1.core.network

/** Sumber bearer token untuk [AuthHeaderInterceptor]. */
fun interface AuthTokenProvider {
    suspend fun currentToken(): String?
}
