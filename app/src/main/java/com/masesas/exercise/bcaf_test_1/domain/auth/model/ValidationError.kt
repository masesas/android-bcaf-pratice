package com.masesas.exercise.bcaf_test_1.domain.auth.model

/**
 * Alasan sebuah field ditolak validator. Sengaja berupa tipe, bukan String, supaya domain
 * layer tidak tahu-menahu soal resource string / bahasa yang dipakai UI.
 */
enum class ValidationError {
    REQUIRED,
    INVALID_EMAIL_FORMAT,
    NAME_TOO_SHORT,
    PASSWORD_TOO_SHORT,
}
