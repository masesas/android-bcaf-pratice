package com.masesas.exercise.bcaf_test_1.presentation.compose.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.masesas.exercise.bcaf_test_1.presentation.compose.auth.navigation.RegisterRoute
import com.masesas.exercise.bcaf_test_1.presentation.compose.auth.navigation.authGraph
import com.masesas.exercise.bcaf_test_1.presentation.compose.home.navigation.HomeMenuRoute
import com.masesas.exercise.bcaf_test_1.presentation.compose.home.navigation.homeDetailDestinations
import com.masesas.exercise.bcaf_test_1.presentation.compose.profile.navigation.DokumenProfileRoute
import com.masesas.exercise.bcaf_test_1.presentation.compose.profile.navigation.FormProfileRoute
import com.masesas.exercise.bcaf_test_1.presentation.compose.profile.navigation.profileDetailDestinations
import com.masesas.exercise.bcaf_test_1.presentation.compose.transaction.navigation.TransactionDetailRoute
import com.masesas.exercise.bcaf_test_1.presentation.compose.transaction.navigation.transactionDetailDestinations

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: AppRoute,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(180)) + slideInHorizontally(
                animationSpec = tween(220),
                initialOffsetX = { it / 12 },
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(150)) + slideOutHorizontally(
                animationSpec = tween(180),
                targetOffsetX = { -it / 12 },
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(180)) + slideInHorizontally(
                animationSpec = tween(220),
                initialOffsetX = { -it / 12 },
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(150)) + slideOutHorizontally(
                animationSpec = tween(180),
                targetOffsetX = { it / 12 },
            )
        },
    ) {
        authGraph(
            onOpenRegister = { navController.navigate(RegisterRoute) },
            onBackToLogin = { navController.popBackStack() },
        )

        composable<MainRoute> {
            MainScreen(
                onOpenMenu = { navController.navigate(HomeMenuRoute(it)) },
                onOpenTransactionDetail = { navController.navigate(TransactionDetailRoute(it)) },
                onOpenFormProfile = { navController.navigate(FormProfileRoute) },
                onOpenDokumenProfile = { navController.navigate(DokumenProfileRoute) },
                onLogout = onLogout,
            )
        }

        homeDetailDestinations(onBack = { navController.popBackStack() })
        transactionDetailDestinations()
        profileDetailDestinations()
    }
}
