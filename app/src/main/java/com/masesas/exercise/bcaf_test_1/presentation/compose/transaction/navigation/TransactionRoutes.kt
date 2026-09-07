package com.masesas.exercise.bcaf_test_1.presentation.compose.transaction.navigation

import com.masesas.exercise.bcaf_test_1.presentation.compose.navigation.AppRoute
import kotlinx.serialization.Serializable

@Serializable
data object TransactionRoute : AppRoute

@Serializable
data class TransactionDetailRoute(val transactionId: String) : AppRoute
