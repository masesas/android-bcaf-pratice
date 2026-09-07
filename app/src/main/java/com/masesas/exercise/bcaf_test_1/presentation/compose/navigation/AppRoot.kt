package com.masesas.exercise.bcaf_test_1.presentation.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.masesas.exercise.bcaf_test_1.presentation.compose.auth.navigation.AuthGraph

@Composable
fun AppRoot(
    startDestination: AppRoute,
    isLoggedIn: Boolean,
    isLoggedOut: Boolean,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    LaunchedEffect(isLoggedIn, isLoggedOut) {
        val currentDestination = navController.currentDestination ?: return@LaunchedEffect
        val isOnAuth = currentDestination.isInGraph(AuthGraph)

        when {
            isLoggedIn && isOnAuth -> navController.navigateToMainAfterAuth()
            isLoggedOut && !isOnAuth -> navController.navigateToAuthAfterLogout()
        }
    }

    AppNavHost(
        navController = navController,
        startDestination = startDestination,
        onLogout = onLogout,
        modifier = modifier,
    )
}

private fun NavDestination.isInGraph(graph: AppRoute): Boolean =
    hierarchy.any { it.hasRoute(graph::class) }

private fun NavHostController.navigateToMainAfterAuth() {
    navigate(MainRoute) {
        popUpTo<AuthGraph> { inclusive = true }
        launchSingleTop = true
    }
}

private fun NavHostController.navigateToAuthAfterLogout() {
    navigate(AuthGraph) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
