package com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth

import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthField
import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthFailure
import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthUser
import com.masesas.exercise.bcaf_test_1.domain.auth.model.ValidationError
import com.masesas.exercise.bcaf_test_1.domain.common.AppFailure

data class AuthUiState(
    val status: AuthStatus = AuthStatus.UNKNOWN,
    val user: AuthUser? = null,
    val isSubmitting: Boolean = false,
    val failure: AppFailure? = null,
    val fieldErrors: Map<AuthField, ValidationError> = emptyMap(),
) {
    val isLoggedIn: Boolean get() = status == AuthStatus.AUTHENTICATED

    val isRestoringSession: Boolean get() = status == AuthStatus.UNKNOWN

    val generalFailure: AppFailure?
        get() = failure.takeUnless { it is AuthFailure.Validation }

    fun errorOf(field: AuthField): ValidationError? = fieldErrors[field]
}
