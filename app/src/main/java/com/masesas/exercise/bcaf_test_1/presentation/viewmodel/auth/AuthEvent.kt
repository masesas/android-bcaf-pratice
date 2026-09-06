package com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth

import com.masesas.exercise.bcaf_test_1.domain.common.AppFailure
import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthUser

sealed interface AuthEvent {

    data class LoggedIn(val user: AuthUser) : AuthEvent

    data class Registered(val user: AuthUser) : AuthEvent

    data object LoggedOut : AuthEvent

    data object SessionExpired : AuthEvent

    data class Failed(val failure: AppFailure) : AuthEvent
}
