package com.masesas.exercise.bcaf_test_1.data.loan.remote

import com.masesas.exercise.bcaf_test_1.core.network.ApiEnvelope
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface LoanApplicationApi {

    @GET("api/customer/loan-application")
    suspend fun getLoanApplications(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("sort") sort: String,
    ): ApiEnvelope<List<LoanApplicationDto>>

    @GET("api/customer/loan-application/{id}")
    suspend fun getLoanApplication(
        @Path("id") id: Long,
    ): ApiEnvelope<LoanApplicationDto>
}
