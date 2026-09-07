package com.masesas.exercise.bcaf_test_1.auth

import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthFailure
import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthField
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthStatus
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthViewModel
import com.masesas.exercise.bcaf_test_1.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val VALID_EMAIL = "budi@example.com"
private const val VALID_PASSWORD = "rahasia123"

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()

    private fun viewModel() = AuthViewModel(repository)

    @Test
    fun `tanpa session tersimpan status jadi UNAUTHENTICATED`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AuthStatus.UNAUTHENTICATED, state.status)
        assertEquals(false, state.isLoggedIn)
        assertEquals(false, state.isRestoringSession)
    }

    @Test
    fun `session tersimpan langsung menandai user sudah login`() = runTest {
        repository.givenStoredSession()

        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isLoggedIn)
        assertEquals(FakeAuthRepository.SESSION.user, state.user)
    }

    @Test
    fun `login berhasil menandai status AUTHENTICATED`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.login(email = VALID_EMAIL, password = VALID_PASSWORD)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isLoggedIn)
        assertEquals(false, state.isSubmitting)
        assertNull(state.failure)
        assertEquals(1, repository.loginCallCount)
    }

    @Test
    fun `email tidak valid ditolak sebelum menyentuh data layer`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.login(email = "bukan-email", password = VALID_PASSWORD)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, repository.loginCallCount)
        assertTrue(state.fieldErrors.containsKey(AuthField.EMAIL))
        assertEquals(false, state.isLoggedIn)
    }

    @Test
    fun `password terlalu pendek ditolak sebelum menyentuh data layer`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.login(email = VALID_EMAIL, password = "123")
        advanceUntilIdle()

        assertEquals(0, repository.loginCallCount)
        assertTrue(viewModel.uiState.value.fieldErrors.containsKey(AuthField.PASSWORD))
    }

    @Test
    fun `kredensial salah memunculkan failure tanpa mengubah status login`() = runTest {
        repository.loginFailure = AuthFailure.InvalidCredentials

        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.login(email = VALID_EMAIL, password = VALID_PASSWORD)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, repository.loginCallCount)
        assertEquals(AuthFailure.InvalidCredentials, state.generalFailure)
        assertEquals(false, state.isLoggedIn)
    }

    @Test
    fun `logout mengembalikan status ke UNAUTHENTICATED`() = runTest {
        repository.givenStoredSession()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.logout()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoggedIn)
        assertTrue(state.isLoggedOut)
    }

    /**
     * Penjaga regresi untuk pemicu navigasi logout di kedua Home. Kalau `isLoggedOut`
     * ditulis sebagai `!isLoggedIn`, status UNKNOWN ikut bernilai true dan user akan
     * dilempar ke LoginActivity setiap kali Home dibuka.
     */
    @Test
    fun `selama session belum selesai dibaca user belum dianggap logout`() = runTest {
        repository.givenStoredSession()

        val state = viewModel().uiState.value

        assertTrue(state.isRestoringSession)
        assertEquals(false, state.isLoggedOut)
        assertEquals(false, state.isLoggedIn)
    }
}
