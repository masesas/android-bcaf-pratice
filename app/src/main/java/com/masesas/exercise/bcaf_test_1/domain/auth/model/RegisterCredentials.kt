package com.masesas.exercise.bcaf_test_1.domain.auth.model

/**
 * Input registrasi yang sudah dinormalisasi sebelum masuk ke repository.
 */
data class RegisterCredentials(
    val name: String,
    val email: String,
    val password: String,
) {
    companion object {
        fun of(name: String, email: String, password: String): RegisterCredentials =
            RegisterCredentials(
                name = name.trim(),
                email = email.trim().lowercase(),
                password = password,
            )
    }
}
