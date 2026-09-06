package com.masesas.exercise.bcaf_test_1.presentation.compose.transaction.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.masesas.exercise.bcaf_test_1.presentation.compose.transaction.TransactionDetailScreen
import com.masesas.exercise.bcaf_test_1.presentation.compose.transaction.TransactionScreen

fun NavGraphBuilder.transactionGraph(
    onOpenTransactionDetail: (transactionId: String) -> Unit,
) {
    navigation<TransactionGraph>(startDestination = TransactionRoute) {
        composable<TransactionRoute> {
            TransactionScreen(
                onOpenTransactionDetail = { onOpenTransactionDetail(SAMPLE_TRANSACTION_ID) },
            )
        }

        composable<TransactionDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<TransactionDetailRoute>()
            TransactionDetailScreen(transactionId = route.transactionId)
        }
    }
}

/** Sementara sampai daftar transaksi memakai data asli. */
private const val SAMPLE_TRANSACTION_ID = "TRX-001"
