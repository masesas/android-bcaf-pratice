package com.masesas.exercise.bcaf_test_1.presentation.compose.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            EnterTransition.None
        },

        exitTransition = {
            ExitTransition.None
        },

        popEnterTransition = {
            EnterTransition.None
        },

        popExitTransition = {
            ExitTransition.None
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
        transactionDetailDestinations(
            navController = navController
        )
        profileDetailDestinations(
            navController = navController
        )
    }
}