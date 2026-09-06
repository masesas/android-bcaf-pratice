package com.masesas.exercise.bcaf_test_1.core.network

import kotlinx.serialization.Serializable

/** Bungkus response seragam dari backend: statusCode, message, data, meta, error. */
@Serializable
data class ApiEnvelope<T>(
    val statusCode: Int? = null,
    val message: String? = null,
    val data: T? = null,
    val meta: ApiMetaDto? = null,
    val error: ApiErrorDto? = null,
)

@Serializable
data class ApiMetaDto(
    val page: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
)

@Serializable
data class ApiErrorDto(
    val code: String? = null,
    val details: List<String> = emptyList(),
)
