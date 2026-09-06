package com.masesas.exercise.bcaf_test_1.domain.auth.validation

import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthField
import com.masesas.exercise.bcaf_test_1.domain.auth.model.LoginCredentials
import com.masesas.exercise.bcaf_test_1.domain.auth.model.RegisterCredentials
import com.masesas.exercise.bcaf_test_1.domain.auth.model.ValidationError

/**
 * Aturan validasi input auth. Fungsi murni tanpa dependency — dipanggil ViewModel sebelum
 * memanggil repository, dan dipanggil ulang di repository sebagai penjaga boundary.
 *
 * Mengembalikan map kosong bila valid.
 */
object AuthCredentialsValidator {

    const val MIN_PASSWORD_LENGTH = 8
    const val MIN_NAME_LENGTH = 2

    private val EMAIL_PATTERN = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validateLogin(credentials: LoginCredentials): Map<AuthField, ValidationError> =
        buildMap {
            validateEmail(credentials.email)?.let { put(AuthField.EMAIL, it) }
            validatePassword(credentials.password)?.let { put(AuthField.PASSWORD, it) }
        }

    fun validateRegister(credentials: RegisterCredentials): Map<AuthField, ValidationError> =
        buildMap {
            validateName(credentials.name)?.let { put(AuthField.NAME, it) }
            validateEmail(credentials.email)?.let { put(AuthField.EMAIL, it) }
            validatePassword(credentials.password)?.let { put(AuthField.PASSWORD, it) }
        }

    private fun validateName(name: String): ValidationError? = when {
        name.isBlank() -> ValidationError.REQUIRED
        name.length < MIN_NAME_LENGTH -> ValidationError.NAME_TOO_SHORT
        else -> null
    }

    private fun validateEmail(email: String): ValidationError? = when {
        email.isBlank() -> ValidationError.REQUIRED
        !EMAIL_PATTERN.matches(email) -> ValidationError.INVALID_EMAIL_FORMAT
        else -> null
    }

    private fun validatePassword(password: String): ValidationError? = when {
        password.isBlank() -> ValidationError.REQUIRED
        password.length < MIN_PASSWORD_LENGTH -> ValidationError.PASSWORD_TOO_SHORT
        else -> null
    }
}
