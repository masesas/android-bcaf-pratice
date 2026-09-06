package com.masesas.exercise.bcaf_test_1.data.auth.remote

import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class JwtClaims(
    val sub: String? = null,
    val exp: Long? = null,
    val name: String? = null,
    val email: String? = null,
)

/** Membaca payload JWT tanpa memverifikasi signature — hanya untuk mengisi identitas & masa berlaku. */
class JwtDecoder(private val json: Json) {

    fun decode(token: String): JwtClaims? {
        val payload = token.split(".").getOrNull(1) ?: return null

        val decoded = runCatching {
            String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
        }.getOrNull() ?: return null

        return runCatching { json.decodeFromString<JwtClaims>(decoded) }.getOrNull()
    }
}
