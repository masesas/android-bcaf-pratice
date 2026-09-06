package com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.masesas.exercise.bcaf_test_1.domain.common.AppResult
import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthField
import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthFailure
import com.masesas.exercise.bcaf_test_1.domain.common.AppFailure
import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthSession
import com.masesas.exercise.bcaf_test_1.domain.auth.model.LoginCredentials
import com.masesas.exercise.bcaf_test_1.domain.auth.model.RegisterCredentials
import com.masesas.exercise.bcaf_test_1.domain.auth.model.ValidationError
import com.masesas.exercise.bcaf_test_1.domain.auth.repository.AuthRepository
import com.masesas.exercise.bcaf_test_1.domain.auth.validation.AuthCredentialsValidator
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>(
        replay = 0,
        extraBufferCapacity = EVENT_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    init {
        observeSession()
        restoreSession()
    }

    fun login(email: String, password: String) {
        val credentials = LoginCredentials.of(email = email, password = password)

        submit(
            fieldErrors = AuthCredentialsValidator.validateLogin(credentials),
            operation = { authRepository.login(credentials) },
            successEvent = { session -> AuthEvent.LoggedIn(session.user) },
        )
    }

    fun register(name: String, email: String, password: String) {
        val credentials = RegisterCredentials.of(name = name, email = email, password = password)

        submit(
            fieldErrors = AuthCredentialsValidator.validateRegister(credentials),
            operation = { authRepository.register(credentials) },
            successEvent = { session -> AuthEvent.Registered(session.user) },
        )
    }

    fun logout() {
        if (_uiState.value.isSubmitting) return

        viewModelScope.launch {
            markSubmitting()

            when (val result = authRepository.logout()) {
                is AppResult.Success -> _events.tryEmit(AuthEvent.LoggedOut)
                is AppResult.Failure -> publishFailure(result.failure)
            }

            markIdle()
        }
    }

    fun clearFailure() {
        _uiState.update { it.copy(failure = null) }
    }

    fun clearFieldError(field: AuthField) {
        _uiState.update { state ->
            if (state.fieldErrors.containsKey(field)) {
                state.copy(fieldErrors = state.fieldErrors - field)
            } else {
                state
            }
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            authRepository.observeSession().collect { session ->
                _uiState.update { state ->
                    state.copy(
                        status = if (session == null) {
                            AuthStatus.UNAUTHENTICATED
                        } else {
                            AuthStatus.AUTHENTICATED
                        },
                        user = session?.user,
                    )
                }
            }
        }
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val result = authRepository.restoreSession()

            if (result is AppResult.Failure) {
                if (result.failure is AuthFailure.SessionExpired) {
                    _events.tryEmit(AuthEvent.SessionExpired)
                } else {
                    publishFailure(result.failure)
                }
            }
        }
    }

    private fun submit(
        fieldErrors: Map<AuthField, ValidationError>,
        operation: suspend () -> AppResult<AuthSession>,
        successEvent: (AuthSession) -> AuthEvent,
    ) {
        if (_uiState.value.isSubmitting) return

        if (fieldErrors.isNotEmpty()) {
            publishFailure(AuthFailure.Validation(fieldErrors))
            return
        }

        viewModelScope.launch {
            markSubmitting()

            when (val result = operation()) {
                is AppResult.Success -> _events.tryEmit(successEvent(result.data))
                is AppResult.Failure -> publishFailure(result.failure)
            }

            markIdle()
        }
    }

    private fun markSubmitting() {
        _uiState.update { it.copy(isSubmitting = true, failure = null, fieldErrors = emptyMap()) }
    }

    private fun markIdle() {
        _uiState.update { it.copy(isSubmitting = false) }
    }

    private fun publishFailure(failure: AppFailure) {
        _uiState.update { state ->
            state.copy(
                failure = failure,
                fieldErrors = (failure as? AuthFailure.Validation)?.fieldErrors ?: emptyMap(),
            )
        }
        _events.tryEmit(AuthEvent.Failed(failure))
    }

    private companion object {
        const val EVENT_BUFFER_CAPACITY = 8
    }
}
