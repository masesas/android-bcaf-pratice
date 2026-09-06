package com.masesas.exercise.bcaf_test_1.data.auth.repository

import com.masesas.exercise.bcaf_test_1.core.network.requirePayload
import com.masesas.exercise.bcaf_test_1.core.network.runApiCatching
import com.masesas.exercise.bcaf_test_1.data.auth.local.AuthSessionLocalDataSource
import com.masesas.exercise.bcaf_test_1.data.auth.mock.MockAuthApi
import com.masesas.exercise.bcaf_test_1.data.auth.mock.MockUserRecord
import com.masesas.exercise.bcaf_test_1.data.auth.remote.AuthApi
import com.masesas.exercise.bcaf_test_1.data.auth.remote.JwtDecoder
import com.masesas.exercise.bcaf_test_1.data.auth.remote.LoginRequestDto
import com.masesas.exercise.bcaf_test_1.data.auth.remote.LoginResponseDto
import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthFailure
import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthSession
import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthUser
import com.masesas.exercise.bcaf_test_1.domain.auth.model.LoginCredentials
import com.masesas.exercise.bcaf_test_1.domain.auth.model.RegisterCredentials
import com.masesas.exercise.bcaf_test_1.domain.auth.repository.AuthRepository
import com.masesas.exercise.bcaf_test_1.domain.auth.validation.AuthCredentialsValidator
import com.masesas.exercise.bcaf_test_1.domain.common.AppResult
import com.masesas.exercise.bcaf_test_1.domain.common.CommonFailure
import com.masesas.exercise.bcaf_test_1.domain.common.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Login memakai API asli; register masih memakai [MockAuthApi] karena backend belum menyediakannya. */
class AuthRepositoryImpl internal constructor(
    private val localDataSource: AuthSessionLocalDataSource,
    private val remoteDataSource: AuthApi,
    private val mockDataSource: MockAuthApi,
    private val jwtDecoder: JwtDecoder,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
) : AuthRepository {

    override fun observeSession(): Flow<AuthSession?> = localDataSource.observe()
        .map { session -> session?.takeUnless { it.isExpiredAt(clock()) } }

    override suspend fun restoreSession(): AppResult<AuthSession?> = runAuthCatching {
        val stored = localDataSource.observe().first()
            ?: return@runAuthCatching AppResult.success(null)

        if (stored.isExpiredAt(clock())) {
            localDataSource.clear()
            AppResult.failure(AuthFailure.SessionExpired)
        } else {
            AppResult.success(stored)
        }
    }

    override suspend fun login(credentials: LoginCredentials): AppResult<AuthSession> {
        val fieldErrors = AuthCredentialsValidator.validateLogin(credentials)
        if (fieldErrors.isNotEmpty()) {
            return AppResult.failure(AuthFailure.Validation(fieldErrors))
        }

        return withContext(ioDispatcher) {
            runApiCatching(json) {
                val envelope = remoteDataSource.login(
                    LoginRequestDto(username = credentials.email, password = credentials.password)
                )

                envelope.requirePayload()
                    .map { payload -> payload.toSession(credentials.email) }
                    .also { result ->
                        if (result is AppResult.Success) localDataSource.save(result.data)
                    }
            }
        }
    }

    override suspend fun register(credentials: RegisterCredentials): AppResult<AuthSession> =
        runAuthCatching {
            val fieldErrors = AuthCredentialsValidator.validateRegister(credentials)
            if (fieldErrors.isNotEmpty()) {
                return@runAuthCatching AppResult.failure(AuthFailure.Validation(fieldErrors))
            }

            val record = mockDataSource.createUser(
                name = credentials.name,
                email = credentials.email,
                password = credentials.password,
            ) ?: return@runAuthCatching AppResult.failure(AuthFailure.EmailAlreadyRegistered)

            AppResult.success(persistMockSession(record))
        }

    override suspend fun logout(): AppResult<Unit> = runAuthCatching {
        localDataSource.clear()
        AppResult.success(Unit)
    }

    private fun LoginResponseDto.toSession(username: String): AuthSession {
        val claims = jwtDecoder.decode(token)

        return AuthSession(
            user = AuthUser(
                id = claims?.sub.orEmpty(),
                name = claims?.name ?: username,
                email = claims?.email ?: username,
                tipe = tipe,
                roles = roles,
            ),
            accessToken = token,
            expiresAtMillis = claims?.exp?.let(TimeUnit.SECONDS::toMillis)
                ?: (clock() + FALLBACK_SESSION_LIFETIME_MILLIS),
        )
    }

    private suspend fun persistMockSession(record: MockUserRecord): AuthSession {
        val session = AuthSession(
            user = AuthUser(id = record.id, name = record.name, email = record.email),
            accessToken = mockDataSource.issueAccessToken(record.id),
            expiresAtMillis = clock() + FALLBACK_SESSION_LIFETIME_MILLIS,
        )
        localDataSource.save(session)
        return session
    }

    private suspend fun <T> runAuthCatching(
        block: suspend () -> AppResult<T>,
    ): AppResult<T> = withContext(ioDispatcher) {
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (io: IOException) {
            AppResult.failure(CommonFailure.Network(io))
        } catch (throwable: Throwable) {
            AppResult.failure(CommonFailure.Unexpected(throwable))
        }
    }

    companion object {
        private val FALLBACK_SESSION_LIFETIME_MILLIS = TimeUnit.HOURS.toMillis(1)
    }
}
