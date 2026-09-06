package com.masesas.exercise.bcaf_test_1.domain.auth.model

/** Identitas user yang sedang login. Tidak pernah memuat password atau token. */
data class AuthUser(
    val id: String,
    val name: String,
    val email: String,
    val tipe: String? = null,
    val roles: List<String> = emptyList(),
)
