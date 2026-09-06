package com.masesas.exercise.bcaf_test_1.domain.auth.model

/**
 * Field form auth yang bisa membawa error validasi. Dipakai sebagai key di
 * [com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthUiState.fieldErrors] supaya UI —
 * baik XML maupun Compose — bisa menempelkan pesan error ke input yang tepat.
 */
enum class AuthField {
    NAME,
    EMAIL,
    PASSWORD,
}
