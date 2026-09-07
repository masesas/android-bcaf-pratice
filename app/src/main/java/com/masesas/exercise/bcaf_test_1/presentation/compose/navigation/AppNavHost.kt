package com.masesas.exercise.bcaf_test_1.presentation.compose.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.masesas.exercise.bcaf_test_1.presentation.compose.home.navigation.HomeGraph
import com.masesas.exercise.bcaf_test_1.presentation.compose.home.navigation.homeGraph
import com.masesas.exercise.bcaf_test_1.presentation.compose.notification.navigation.notificationGraph
import com.masesas.exercise.bcaf_test_1.presentation.compose.profile.navigation.DokumenProfileRoute
import com.masesas.exercise.bcaf_test_1.presentation.compose.profile.navigation.FormProfileRoute
import com.masesas.exercise.bcaf_test_1.presentation.compose.profile.navigation.profileGraph
import com.masesas.exercise.bcaf_test_1.presentation.compose.transaction.navigation.TransactionDetailRoute
import com.masesas.exercise.bcaf_test_1.presentation.compose.transaction.navigation.transactionGraph

/** Satu-satunya NavHost aplikasi; tiap fitur menyumbang nested graph-nya sendiri. */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HomeGraph,
        modifier = modifier,
        enterTransition = {
            fadeIn(tween(220))
        },
        exitTransition = {
            fadeOut(tween(180))
        },
    ) {
        homeGraph()
        transactionGraph(
            onOpenTransactionDetail = { transactionId ->
                navController.navigate(TransactionDetailRoute(transactionId))
            },
        )
        notificationGraph()
        profileGraph(
            onOpenFormProfile = { navController.navigate(FormProfileRoute) },
            onOpenDokumenProfile = { navController.navigate(DokumenProfileRoute) },
        )
    }
}
