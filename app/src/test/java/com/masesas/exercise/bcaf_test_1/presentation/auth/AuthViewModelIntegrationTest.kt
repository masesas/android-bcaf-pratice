package com.masesas.exercise.bcaf_test_1.presentation.auth

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.masesas.exercise.bcaf_test_1.data.auth.local.AuthSessionLocalDataSource
import com.masesas.exercise.bcaf_test_1.data.auth.mock.MockAuthApi
import com.masesas.exercise.bcaf_test_1.data.auth.remote.AuthApi
import com.masesas.exercise.bcaf_test_1.data.auth.remote.Base64JwtDecoder
import com.masesas.exercise.bcaf_test_1.data.auth.remote.LoginRequestDto
import com.masesas.exercise.bcaf_test_1.data.auth.repository.AuthRepositoryImpl
import com.masesas.exercise.bcaf_test_1.di.NetworkModule
import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthField
import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthSession
import com.masesas.exercise.bcaf_test_1.domain.auth.model.ValidationError
import com.masesas.exercise.bcaf_test_1.domain.common.CommonFailure
import com.masesas.exercise.bcaf_test_1.notification.FakeDeviceRegistrationRepository
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthEvent
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthStatus
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthViewModel
import com.masesas.exercise.bcaf_test_1.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

private const val NOW = 1_700_000_000_000L
private const val EMAIL = "budi@example.com"
private const val PASSWORD = "secret123"
private const val SERVER_TOKEN = "server-token"
private val SESSION_LIFETIME = TimeUnit.HOURS.toMillis(1)

private val SUCCESS_BODY = """
    {"statusCode":200,"message":"OK","data":{"token":"$SERVER_TOKEN","tipe":"CUSTOMER","roles":["ROLE_CUSTOMER"]}}
""".trimIndent()

class AuthIntegrationRule(private val dispatcher: TestDispatcher) : ExternalResource() {

    val server = MockWebServer()

    lateinit var localDataSource: AuthSessionLocalDataSource
        private set

    override fun before() {
        server.start()
        localDataSource = AuthSessionLocalDataSource(ApplicationProvider.getApplicationContext())
        runBlocking { localDataSource.clear() }
    }

    override fun after() {
        runBlocking { localDataSource.clear() }
        server.close()
    }

    fun enqueue(code: Int, body: String = "") {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    fun createViewModel(): AuthViewModel {
        val json = NetworkModule.provideJson()
        val authApi = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApi::class.java)

        val repository = AuthRepositoryImpl(
            localDataSource = localDataSource,
            remoteDataSource = authApi,
            mockDataSource = MockAuthApi(latencyMillis = 0),
            jwtDecoder = Base64JwtDecoder(json),
            json = json,
            ioDispatcher = dispatcher,
            clock = { NOW },
        )
        return AuthViewModel(repository, FakeDeviceRegistrationRepository())
    }
}

@RunWith(Enclosed::class)
class AuthViewModelIntegrationTest {

    @RunWith(RobolectricTestRunner::class)
    class Login {

        @get:Rule
        val mainDispatcherRule = MainDispatcherRule()

        @get:Rule
        val env = AuthIntegrationRule(mainDispatcherRule.dispatcher)

        @Test
        fun `authenticates user when server accepts credentials`() = runTest {
            env.enqueue(200, SUCCESS_BODY)
            val viewModel = env.createViewModel()

            viewModel.login(EMAIL, PASSWORD)

            val state = viewModel.uiState.first { it.isLoggedIn && !it.isSubmitting }
            assertNull(state.failure)
        }

        @Test
        fun `stores access token returned by server`() = runTest {
            env.enqueue(200, SUCCESS_BODY)
            val viewModel = env.createViewModel()

            viewModel.login(EMAIL, PASSWORD)

            viewModel.uiState.first { it.isLoggedIn && !it.isSubmitting }
            assertEquals(SERVER_TOKEN, env.localDataSource.observe().first()?.accessToken)
        }

        @Test
        fun `sends normalized email as username to login endpoint`() = runTest {
            env.enqueue(200, SUCCESS_BODY)
            val viewModel = env.createViewModel()

            viewModel.login("  Budi@Example.COM ", PASSWORD)

            viewModel.uiState.first { it.isLoggedIn && !it.isSubmitting }
            val request = env.server.takeRequest()
            val body = NetworkModule.provideJson().decodeFromString<LoginRequestDto>(requireNotNull(request.body).utf8())
            assertEquals("POST", request.method)
            assertEquals("/api/auth/customer/login", request.target)
            assertEquals(LoginRequestDto(username = EMAIL, password = PASSWORD), body)
        }

        @Test
        fun `reports validation failure without calling server when password is empty`() = runTest {
            val viewModel = env.createViewModel()

            viewModel.login(EMAIL, "")

            assertEquals(ValidationError.REQUIRED, viewModel.uiState.value.errorOf(AuthField.PASSWORD))
            assertEquals(0, env.server.requestCount)
        }

        @Test
        fun `reports server message when server rejects credentials`() = runTest {
            env.enqueue(400, """{"statusCode":400,"message":"Invalid username or password"}""")
            val viewModel = env.createViewModel()

            viewModel.login(EMAIL, PASSWORD)

            val state = viewModel.uiState.first { it.failure != null && !it.isSubmitting }
            assertEquals(CommonFailure.ApiError(message = "Invalid username or password"), state.failure)
            assertEquals(AuthStatus.UNAUTHENTICATED, state.status)
        }

        @Test
        fun `reports error details when response envelope contains error`() = runTest {
            env.enqueue(200, """{"error":{"code":"AUTH_001","details":["Account is locked","Contact support"]}}""")
            val viewModel = env.createViewModel()

            viewModel.login(EMAIL, PASSWORD)

            val state = viewModel.uiState.first { it.failure != null && !it.isSubmitting }
            assertEquals(
                CommonFailure.ApiError(code = "AUTH_001", details = listOf("Account is locked", "Contact support")),
                state.failure,
            )
        }

        @Test
        fun `reports unauthorized when server responds 401 without body`() = runTest {
            env.enqueue(401)
            val viewModel = env.createViewModel()

            viewModel.login(EMAIL, PASSWORD)

            val state = viewModel.uiState.first { it.failure != null && !it.isSubmitting }
            assertEquals(CommonFailure.Unauthorized, state.failure)
        }

        @Test
        fun `reports network failure when server is unreachable`() = runTest {
            val viewModel = env.createViewModel()
            env.server.close()

            viewModel.login(EMAIL, PASSWORD)

            val state = viewModel.uiState.first { it.failure != null && !it.isSubmitting }
            assertTrue(state.failure is CommonFailure.Network)
        }

        @Test
        fun `clears previous failure after successful retry`() = runTest {
            env.enqueue(401)
            env.enqueue(200, SUCCESS_BODY)
            val viewModel = env.createViewModel()
            viewModel.login(EMAIL, PASSWORD)
            viewModel.uiState.first { it.failure != null && !it.isSubmitting }

            viewModel.login(EMAIL, PASSWORD)

            val state = viewModel.uiState.first { it.isLoggedIn && !it.isSubmitting }
            assertNull(state.failure)
        }
    }

    @RunWith(RobolectricTestRunner::class)
    class RestoreSession {

        @get:Rule
        val mainDispatcherRule = MainDispatcherRule()

        @get:Rule
        val env = AuthIntegrationRule(mainDispatcherRule.dispatcher)

        @Test
        fun `restores valid stored session as authenticated`() = runTest {
            env.localDataSource.save(AuthSession(accessToken = "stored-token", expiresAtMillis = NOW + SESSION_LIFETIME))

            val viewModel = env.createViewModel()

            val state = viewModel.uiState.first { it.status != AuthStatus.UNKNOWN }
            assertEquals(AuthStatus.AUTHENTICATED, state.status)
            assertNull(state.failure)
        }

        @Test
        fun `emits session expired event when stored session has expired`() = runTest {
            env.localDataSource.save(AuthSession(accessToken = "stored-token", expiresAtMillis = NOW - 1))

            val viewModel = env.createViewModel()

            viewModel.events.test {
                assertEquals(AuthEvent.SessionExpired, awaitItem())
            }
        }

        @Test
        fun `removes expired session from storage`() = runTest {
            env.localDataSource.save(AuthSession(accessToken = "stored-token", expiresAtMillis = NOW - 1))

            val viewModel = env.createViewModel()

            viewModel.events.test { awaitItem() }
            assertNull(env.localDataSource.observe().first())
        }

        @Test
        fun `keeps user logged out when stored session has expired`() = runTest {
            env.localDataSource.save(AuthSession(accessToken = "stored-token", expiresAtMillis = NOW - 1))

            val viewModel = env.createViewModel()

            viewModel.events.test { awaitItem() }
            val state = viewModel.uiState.first { it.status != AuthStatus.UNKNOWN }
            assertFalse(state.isLoggedIn)
        }
    }

    @RunWith(RobolectricTestRunner::class)
    class ObserveSession {

        @get:Rule
        val mainDispatcherRule = MainDispatcherRule()

        @get:Rule
        val env = AuthIntegrationRule(mainDispatcherRule.dispatcher)

        private val validSession = AuthSession(accessToken = "stored-token", expiresAtMillis = NOW + SESSION_LIFETIME)

        @Test
        fun `starts with unknown status before storage is read`() = runTest {
            val viewModel = env.createViewModel()

            val state = viewModel.uiState.value

            assertEquals(AuthStatus.UNKNOWN, state.status)
        }

        @Test
        fun `becomes unauthenticated when storage has no session`() = runTest {
            val viewModel = env.createViewModel()

            val state = viewModel.uiState.first { it.status != AuthStatus.UNKNOWN }

            assertEquals(AuthStatus.UNAUTHENTICATED, state.status)
        }

        @Test
        fun `becomes authenticated when session is saved to storage`() = runTest {
            val viewModel = env.createViewModel()
            viewModel.uiState.first { it.isLoggedOut }

            env.localDataSource.save(validSession)

            val state = viewModel.uiState.first { it.status != AuthStatus.UNAUTHENTICATED }
            assertEquals(AuthStatus.AUTHENTICATED, state.status)
        }

        @Test
        fun `becomes unauthenticated when stored session is cleared`() = runTest {
            env.localDataSource.save(validSession)
            val viewModel = env.createViewModel()
            viewModel.uiState.first { it.isLoggedIn }

            env.localDataSource.clear()

            val state = viewModel.uiState.first { it.status != AuthStatus.AUTHENTICATED }
            assertEquals(AuthStatus.UNAUTHENTICATED, state.status)
        }
    }
}
