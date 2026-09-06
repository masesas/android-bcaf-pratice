package com.masesas.exercise.bcaf_test_1.presentation.compose.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.masesas.exercise.bcaf_test_1.presentation.compose.home.HomeScreen

fun NavGraphBuilder.homeGraph() {
    navigation<HomeGraph>(startDestination = HomeRoute) {
        composable<HomeRoute> { HomeScreen() }
    }
}
