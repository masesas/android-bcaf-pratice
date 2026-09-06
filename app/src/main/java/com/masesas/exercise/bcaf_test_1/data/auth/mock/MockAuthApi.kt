package com.masesas.exercise.bcaf_test_1.data.auth.mock

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Pengganti backend auth. Menyimpan daftar user di memori dan menirukan latensi jaringan.
 *
 * Daftar user disimpan di [MutableStateFlow] dan diperbarui lewat `update { it + record }`
 * (salin, bukan ubah di tempat), sehingga aman diakses dari beberapa coroutine sekaligus.
 *
 * Diganti implementasi HTTP asli nanti tanpa menyentuh domain maupun ViewModel.
 */
class MockAuthApi(
    private val latencyMillis: Long = DEFAULT_LATENCY_MILLIS,
) {

    private val users = MutableStateFlow(SEED_USERS)

    /** @return record user bila cocok, `null` bila email tidak dikenal atau password salah. */
    suspend fun authenticate(email: String, password: String): MockUserRecord? {
        delay(latencyMillis)
        return users.value.firstOrNull { it.email == email && it.password == password }
    }

    /** @return record user baru, atau `null` bila email sudah terpakai. */
    suspend fun createUser(name: String, email: String, password: String): MockUserRecord? {
        delay(latencyMillis)

        if (users.value.any { it.email == email }) return null

        val created = MockUserRecord(
            id = UUID.randomUUID().toString(),
            name = name,
            email = email,
            password = password,
        )
        users.update { existing -> existing + created }
        return created
    }

    /** Token palsu; bentuknya sengaja dibuat mirip token asli agar UI tidak perlu diubah nanti. */
    fun issueAccessToken(userId: String): String = "mock-token-$userId-${UUID.randomUUID()}"

    companion object {
        private const val DEFAULT_LATENCY_MILLIS = 800L

        /**
         * Akun bawaan untuk mencoba flow login tanpa registrasi lebih dulu.
         * Kredensial: budi@bcaf.co.id / password123
         */
        private val SEED_USERS = listOf(
            MockUserRecord(
                id = "user-001",
                name = "Budi Santoso",
                email = "budi@bcaf.co.id",
                password = "password123",
            ),
            MockUserRecord(
                id = "user-002",
                name = "Siti Rahayu",
                email = "siti@bcaf.co.id",
                password = "password123",
            ),
        )
    }
}
