package com.masesas.exercise.bcaf_test_1.data.loan.remote

import com.masesas.exercise.bcaf_test_1.core.network.ApiEnvelope
import retrofit2.http.GET
import retrofit2.http.Query

interface LoanProductApi {

    @GET("api/loan-product")
    suspend fun getLoanProducts(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("sort") sort: List<String>,
    ): ApiEnvelope<List<LoanProductDto>>
}
