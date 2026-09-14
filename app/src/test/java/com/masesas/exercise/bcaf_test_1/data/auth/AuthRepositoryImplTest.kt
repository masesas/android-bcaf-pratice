@file:OptIn(ExperimentalCoroutinesApi::class)

package com.masesas.exercise.bcaf_test_1.data.auth

import app.cash.turbine.test
import com.masesas.exercise.bcaf_test_1.core.network.ApiEnvelope
import com.masesas.exercise.bcaf_test_1.core.network.ApiErrorDto
import com.masesas.exercise.bcaf_test_1.data.auth.local.AuthSessionLocalDataSource
import com.masesas.exercise.bcaf_test_1.data.auth.remote.AuthApi
import com.masesas.exercise.bcaf_test_1.data.auth.remote.JwtClaims
import com.masesas.exercise.bcaf_test_1.data.auth.remote.JwtDecoder
import com.masesas.exercise.bcaf_test_1.data.auth.remote.LoginRequestDto
import com.masesas.exercise.bcaf_test_1.data.auth.remote.LoginResponseDto
import com.masesas.exercise.bcaf_test_1.data.auth.repository.AuthRepositoryImpl
import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthSession
import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthUser
import com.masesas.exercise.bcaf_test_1.domain.auth.model.LoginCredentials
import com.masesas.exercise.bcaf_test_1.domain.common.AppResult
import com.masesas.exercise.bcaf_test_1.domain.common.CommonFailure
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val NOW = 1_700_000_000_000L
private const val TOKEN = "header.payload.signature"
private val SESSION_LIFETIME = TimeUnit.HOURS.toMillis(1)
private val CREDENTIALS = LoginCredentials(email = "budi@example.com", password = "secret123")

private fun authRepository(
    localDataSource: AuthSessionLocalDataSource,
    remoteDataSource: AuthApi = mockk(),
    jwtDecoder: JwtDecoder = mockk(),
    clock: () -> Long = { NOW },
) = AuthRepositoryImpl(
    localDataSource = localDataSource,
    remoteDataSource = remoteDataSource,
    mockDataSource = mockk(),
    jwtDecoder = jwtDecoder,
    json = Json,
    ioDispatcher = UnconfinedTestDispatcher(),
    clock = clock,
)

private fun storedSession(expiresAtMillis: Long) =
    AuthSession(accessToken = TOKEN, expiresAtMillis = expiresAtMillis)

@RunWith(Enclosed::class)
class AuthRepositoryImplTest {

    class Login {
        private val localDataSource = mockk<AuthSessionLocalDataSource>(relaxUnitFun = true)
        private val remoteDataSource = mockk<AuthApi>()
        private val jwtDecoder = mockk<JwtDecoder>()
        private val repository = authRepository(localDataSource, remoteDataSource, jwtDecoder)

        private val loginResponse = LoginResponseDto(
            token = TOKEN,
            tipe = "CUSTOMER",
            roles = listOf("ROLE_CUSTOMER"),
        )

        @Test
        fun `returns session mapped from jwt claims and login response`() = runTest {
            coEvery { remoteDataSource.login(any()) } returns ApiEnvelope(data = loginResponse)
            every { jwtDecoder.decode(TOKEN) } returns JwtClaims(
                sub = "user-001",
                name = "Budi Santoso",
                email = "budi@bcaf.co.id",
            )

            val result = repository.login(CREDENTIALS)

            val expected = AuthSession(
                user = AuthUser(
                    id = "user-001",
                    name = "Budi Santoso",
                    email = "budi@bcaf.co.id",
                    tipe = "CUSTOMER",
                    roles = listOf("ROLE_CUSTOMER"),
                ),
                accessToken = TOKEN,
                expiresAtMillis = NOW + SESSION_LIFETIME,
            )
            assertEquals(AppResult.success(expected), result)
        }

        @Test
        fun `falls back to credential email when jwt claims are missing`() = runTest {
            coEvery { remoteDataSource.login(any()) } returns ApiEnvelope(data = loginResponse)
            every { jwtDecoder.decode(TOKEN) } returns null

            val result = repository.login(CREDENTIALS)

            val expected = AuthUser(
                id = "",
                name = CREDENTIALS.email,
                email = CREDENTIALS.email,
                tipe = "CUSTOMER",
                roles = listOf("ROLE_CUSTOMER"),
            )
            assertEquals(expected, (result as AppResult.Success).data.user)
        }

        @Test
        fun `sends email as username together with password`() = runTest {
            coEvery { remoteDataSource.login(any()) } returns ApiEnvelope(data = loginResponse)
            every { jwtDecoder.decode(any()) } returns null

            repository.login(CREDENTIALS)

            val expected = LoginRequestDto(username = "budi@example.com", password = "secret123")
            coVerify(exactly = 1) { remoteDataSource.login(expected) }
        }

        @Test
        fun `saves session to local data source when login succeeds`() = runTest {
            coEvery { remoteDataSource.login(any()) } returns ApiEnvelope(data = loginResponse)
            every { jwtDecoder.decode(any()) } returns null

            val result = repository.login(CREDENTIALS)

            coVerify(exactly = 1) { localDataSource.save((result as AppResult.Success).data) }
        }

        @Test
        fun `returns api error when envelope contains error`() = runTest {
            val error = ApiErrorDto(code = "AUTH_001", details = listOf("Account is locked"))
            coEvery { remoteDataSource.login(any()) } returns ApiEnvelope(error = error)

            val result = repository.login(CREDENTIALS)

            assertEquals(
                AppResult.failure(CommonFailure.ApiError(code = "AUTH_001", details = listOf("Account is locked"))),
                result,
            )
        }

        @Test
        fun `returns api error with message when envelope has no data`() = runTest {
            coEvery { remoteDataSource.login(any()) } returns ApiEnvelope(message = "Login failed")

            val result = repository.login(CREDENTIALS)

            assertEquals(AppResult.failure(CommonFailure.ApiError(details = listOf("Login failed"))), result)
        }

        @Test
        fun `returns network failure when request throws IOException`() = runTest {
            val offline = IOException("offline")
            coEvery { remoteDataSource.login(any()) } throws offline

            val result = repository.login(CREDENTIALS)

            assertEquals(AppResult.failure(CommonFailure.Network(offline)), result)
        }

        @Test
        fun `returns unauthorized when server responds 401 without body`() = runTest {
            val unauthorized = HttpException(Response.error<Any>(401, "".toResponseBody()))
            coEvery { remoteDataSource.login(any()) } throws unauthorized

            val result = repository.login(CREDENTIALS)

            assertEquals(AppResult.failure(CommonFailure.Unauthorized), result)
        }

        @Test
        fun `returns api error with server message when error body is an envelope`() = runTest {
            val body = """{"statusCode":400,"message":"Invalid username or password"}"""
            val badRequest = HttpException(Response.error<Any>(400, body.toResponseBody()))
            coEvery { remoteDataSource.login(any()) } throws badRequest

            val result = repository.login(CREDENTIALS)

            assertEquals(
                AppResult.failure(CommonFailure.ApiError(message = "Invalid username or password")),
                result,
            )
        }

        @Test
        fun `does not save session when login fails`() = runTest {
            coEvery { remoteDataSource.login(any()) } throws IOException("offline")

            repository.login(CREDENTIALS)

            coVerify(exactly = 0) { localDataSource.save(any()) }
        }
    }

    class ObserveSession {

        private val localDataSource = mockk<AuthSessionLocalDataSource>()
        private var now = NOW
        private val repository = authRepository(localDataSource, clock = { now })

        @Test
        fun `emits null when no session is stored`() = runTest {
            every { localDataSource.observe() } returns flowOf(null)

            val session = repository.observeSession().first()

            assertNull(session)
        }

        @Test
        fun `emits stored session when it has not expired`() = runTest {
            val stored = storedSession(expiresAtMillis = NOW + 1)
            every { localDataSource.observe() } returns flowOf(stored)

            val session = repository.observeSession().first()

            assertEquals(stored, session)
        }

        @Test
        fun `emits null when stored session has expired`() = runTest {
            every { localDataSource.observe() } returns flowOf(storedSession(expiresAtMillis = NOW - 1))

            val session = repository.observeSession().first()

            assertNull(session)
        }

        @Test
        fun `treats session expiring exactly now as expired`() = runTest {
            every { localDataSource.observe() } returns flowOf(storedSession(expiresAtMillis = NOW))

            val session = repository.observeSession().first()

            assertNull(session)
        }

        @Test
        fun `emits null after stored session is cleared`() = runTest {
            val stored = storedSession(expiresAtMillis = NOW + SESSION_LIFETIME)
            val storedSessions = MutableSharedFlow<AuthSession?>()
            every { localDataSource.observe() } returns storedSessions

            repository.observeSession().test {
                storedSessions.emit(stored)
                assertEquals(stored, awaitItem())

                storedSessions.emit(null)
                assertNull(awaitItem())
            }
        }

        @Test
        fun `checks expiry against current time on every emission`() = runTest {
            val stored = storedSession(expiresAtMillis = NOW + SESSION_LIFETIME)
            val storedSessions = MutableSharedFlow<AuthSession?>()
            every { localDataSource.observe() } returns storedSessions

            repository.observeSession().test {
                storedSessions.emit(stored)
                assertEquals(stored, awaitItem())

                now = NOW + SESSION_LIFETIME
                storedSessions.emit(stored)
                assertNull(awaitItem())
            }
        }
    }
}
