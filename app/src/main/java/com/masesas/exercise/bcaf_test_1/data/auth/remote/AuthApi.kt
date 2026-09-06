package com.masesas.exercise.bcaf_test_1.data.auth.remote

import com.masesas.exercise.bcaf_test_1.core.network.ApiEnvelope
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/auth/customer/login")
    suspend fun login(@Body body: LoginRequestDto): ApiEnvelope<LoginResponseDto>
}
