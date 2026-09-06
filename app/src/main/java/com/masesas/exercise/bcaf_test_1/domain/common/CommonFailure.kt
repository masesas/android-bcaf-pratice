package com.masesas.exercise.bcaf_test_1.domain.common

/** Kegagalan yang berlaku untuk semua fitur, bukan milik satu domain tertentu. */
sealed interface CommonFailure : AppFailure {

    data class Network(val cause: Throwable? = null) : CommonFailure

    data object Unauthorized : CommonFailure

    data class ApiError(
        val code: String? = null,
        val details: List<String> = emptyList(),
    ) : CommonFailure

    data class Unexpected(val cause: Throwable? = null) : CommonFailure
}
