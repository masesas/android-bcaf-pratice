package com.masesas.exercise.bcaf_test_1.domain.auth.model

import com.masesas.exercise.bcaf_test_1.domain.common.AppFailure

/** Kegagalan khusus auth. Kegagalan umum (jaringan, 401, dll) dipakai dari CommonFailure. */
sealed interface AuthFailure : AppFailure {

    /** Input tidak lolos validasi. [fieldErrors] tidak pernah kosong. */
    data class Validation(val fieldErrors: Map<AuthField, ValidationError>) : AuthFailure

    data object InvalidCredentials : AuthFailure

    data object EmailAlreadyRegistered : AuthFailure

    data object SessionExpired : AuthFailure
}
