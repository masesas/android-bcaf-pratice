package com.masesas.exercise.bcaf_test_1.auth

import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthSession
import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthUser
import com.masesas.exercise.bcaf_test_1.domain.auth.model.LoginCredentials
import com.masesas.exercise.bcaf_test_1.domain.auth.model.RegisterCredentials
import com.masesas.exercise.bcaf_test_1.domain.auth.repository.AuthRepository
import com.masesas.exercise.bcaf_test_1.domain.common.AppFailure
import com.masesas.exercise.bcaf_test_1.domain.common.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Pengganti AuthRepository di unit test; menirukan DataStore lewat StateFlow. */
class FakeAuthRepository : AuthRepository {

    private val session = MutableStateFlow<AuthSession?>(null)

    var loginFailure: AppFailure? = null
    var restoreResult: AppResult<AuthSession?> = AppResult.success(null)

    /** Dipakai untuk membuktikan validasi menahan request sebelum menyentuh data layer. */
    var loginCallCount = 0
        private set

    /** Menaruh session seolah sudah tersimpan dari sesi sebelumnya. */
    fun givenStoredSession(stored: AuthSession = SESSION) {
        restoreResult = AppResult.success(stored)
    }

    override fun observeSession(): Flow<AuthSession?> = session.asStateFlow()

    override suspend fun restoreSession(): AppResult<AuthSession?> = restoreResult.also { result ->
        if (result is AppResult.Success) session.value = result.data
    }

    override suspend fun login(credentials: LoginCredentials): AppResult<AuthSession> {
        loginCallCount++

        loginFailure?.let { return AppResult.failure(it) }

        session.value = SESSION
        return AppResult.success(SESSION)
    }

    override suspend fun register(credentials: RegisterCredentials): AppResult<AuthSession> {
        session.value = SESSION
        return AppResult.success(SESSION)
    }

    override suspend fun logout(): AppResult<Unit> {
        session.value = null
        return AppResult.success(Unit)
    }

    companion object {
        val SESSION = AuthSession(
            user = AuthUser(id = "1", name = "Budi", email = "budi@example.com"),
            accessToken = "token",
            expiresAtMillis = Long.MAX_VALUE,
        )
    }
}
