package com.masesas.exercise.bcaf_test_1.presentation.compose.auth.navigation

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.masesas.exercise.bcaf_test_1.presentation.compose.auth.LoginScreen
import com.masesas.exercise.bcaf_test_1.presentation.compose.auth.RegisterScreen

fun NavGraphBuilder.authGraph(
    onOpenRegister: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    navigation<AuthGraph>(startDestination = LoginRoute) {
        composable<LoginRoute> {
            LoginScreen(onOpenRegister = onOpenRegister, modifier = Modifier.safeDrawingPadding())
        }
        composable<RegisterRoute> {
            RegisterScreen(onBackToLogin = onBackToLogin, modifier = Modifier.safeDrawingPadding())
        }
    }
}
