package com.masesas.exercise.bcaf_test_1.domain.auth.model

/**
 * Input login yang sudah dinormalisasi (email di-trim & lowercase) sebelum masuk ke repository.
 */
data class LoginCredentials(
    val email: String,
    val password: String,
) {
    companion object {
        fun of(email: String, password: String): LoginCredentials = LoginCredentials(
            email = email.trim().lowercase(),
            password = password,
        )
    }
}
