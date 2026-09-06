package com.masesas.exercise.bcaf_test_1.core.ui

import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthFailure
import com.masesas.exercise.bcaf_test_1.domain.auth.model.ValidationError
import com.masesas.exercise.bcaf_test_1.domain.auth.validation.AuthCredentialsValidator
import com.masesas.exercise.bcaf_test_1.domain.common.AppFailure
import com.masesas.exercise.bcaf_test_1.domain.common.CommonFailure

fun AppFailure.toUiMessage(): UiMessage = when (this) {
    is AuthFailure -> authMessage()
    is CommonFailure -> commonMessage()
    else -> UiMessage(R.string.auth_error_unexpected)
}

private fun AuthFailure.authMessage(): UiMessage = when (this) {
    is AuthFailure.Validation -> UiMessage(R.string.auth_error_validation)
    AuthFailure.InvalidCredentials -> UiMessage(R.string.auth_error_invalid_credentials)
    AuthFailure.EmailAlreadyRegistered -> UiMessage(R.string.auth_error_email_already_registered)
    AuthFailure.SessionExpired -> UiMessage(R.string.auth_error_session_expired)
}

private fun CommonFailure.commonMessage(): UiMessage = when (this) {
    is CommonFailure.Network -> UiMessage(R.string.auth_error_network)
    CommonFailure.Unauthorized -> UiMessage(R.string.error_unauthorized)
    is CommonFailure.ApiError -> details.firstOrNull()
        ?.let { UiMessage(R.string.error_api_detail, listOf(it)) }
        ?: UiMessage(R.string.auth_error_unexpected)

    is CommonFailure.Unexpected -> UiMessage(R.string.auth_error_unexpected)
}

fun ValidationError.toUiMessage(): UiMessage = when (this) {
    ValidationError.REQUIRED -> UiMessage(R.string.auth_validation_required)

    ValidationError.INVALID_EMAIL_FORMAT -> UiMessage(R.string.auth_validation_invalid_email)

    ValidationError.NAME_TOO_SHORT -> UiMessage(
        resId = R.string.auth_validation_name_too_short,
        formatArgs = listOf(AuthCredentialsValidator.MIN_NAME_LENGTH),
    )

    ValidationError.PASSWORD_TOO_SHORT -> UiMessage(
        resId = R.string.auth_validation_password_too_short,
        formatArgs = listOf(AuthCredentialsValidator.MIN_PASSWORD_LENGTH),
    )
}
