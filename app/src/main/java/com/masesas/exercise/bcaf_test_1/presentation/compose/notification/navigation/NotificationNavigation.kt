package com.masesas.exercise.bcaf_test_1.presentation.compose.notification.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.masesas.exercise.bcaf_test_1.presentation.compose.notification.NotificationScreen

fun NavGraphBuilder.notificationGraph() {
    navigation<NotificationGraph>(startDestination = NotificationRoute) {
        composable<NotificationRoute> { NotificationScreen() }
    }
}
