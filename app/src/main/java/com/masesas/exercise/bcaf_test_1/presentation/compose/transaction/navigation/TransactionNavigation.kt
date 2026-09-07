package com.masesas.exercise.bcaf_test_1.presentation.compose.transaction.navigation

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.masesas.exercise.bcaf_test_1.presentation.compose.transaction.TransactionDetailScreen
import com.masesas.exercise.bcaf_test_1.presentation.compose.transaction.TransactionScreen

fun NavGraphBuilder.transactionTab(onOpenTransactionDetail: (transactionId: String) -> Unit) {
    composable<TransactionRoute> {
        TransactionScreen(onOpenTransactionDetail = onOpenTransactionDetail)
    }
}

fun NavGraphBuilder.transactionDetailDestinations() {
    composable<TransactionDetailRoute> { entry ->
        TransactionDetailScreen(
            transactionId = entry.toRoute<TransactionDetailRoute>().transactionId,
            modifier = Modifier.safeDrawingPadding(),
        )
    }
}
