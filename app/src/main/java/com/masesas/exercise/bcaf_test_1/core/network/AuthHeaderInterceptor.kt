package com.masesas.exercise.bcaf_test_1.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

private const val HEADER_AUTHORIZATION = "Authorization"

/** Menempelkan `Authorization: Bearer <token>` bila ada session tersimpan. */
class AuthHeaderInterceptor(
    private val tokenProvider: AuthTokenProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (request.header(HEADER_AUTHORIZATION) != null) return chain.proceed(request)

        val token = runBlocking { tokenProvider.currentToken() }
            ?: return chain.proceed(request)

        return chain.proceed(
            request.newBuilder().header(HEADER_AUTHORIZATION, "Bearer $token").build()
        )
    }
}
