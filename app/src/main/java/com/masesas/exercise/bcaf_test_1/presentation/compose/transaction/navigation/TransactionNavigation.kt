package com.masesas.exercise.bcaf_test_1.presentation.compose.transaction.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.component.fullScreenComposable
import com.masesas.exercise.bcaf_test_1.presentation.compose.transaction.TransactionDetailScreen
import com.masesas.exercise.bcaf_test_1.presentation.compose.transaction.TransactionScreen

fun NavGraphBuilder.transactionTab(onOpenTransactionDetail: (transactionId: String) -> Unit) {
    composable<TransactionRoute> {
        TransactionScreen(onOpenTransactionDetail = onOpenTransactionDetail)
    }
}

fun NavGraphBuilder.transactionDetailDestinations(navController: NavHostController) {
    fullScreenComposable<TransactionDetailRoute>(
        navController = navController,
        deepLinks = listOf(navDeepLink<TransactionDetailRoute>(basePath = TRANSACTION_DETAIL_DEEP_LINK)),
    ) { entry, innerPadding ->
        val route = entry.toRoute<TransactionDetailRoute>()

        TransactionDetailScreen(
            transactionId = route.transactionId,
            modifier = Modifier
                .padding(innerPadding)
                .safeDrawingPadding(),
        )
    }
}
