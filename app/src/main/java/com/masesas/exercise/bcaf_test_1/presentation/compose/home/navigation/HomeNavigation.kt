package com.masesas.exercise.bcaf_test_1.presentation.compose.home.navigation

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.masesas.exercise.bcaf_test_1.presentation.compose.home.HomeMenu
import com.masesas.exercise.bcaf_test_1.presentation.compose.home.HomeMenuScreen
import com.masesas.exercise.bcaf_test_1.presentation.compose.home.HomeScreen

fun NavGraphBuilder.homeTab(onOpenMenu: (HomeMenu) -> Unit) {
    composable<HomeRoute> { HomeScreen(onOpenMenu = onOpenMenu) }
}

fun NavGraphBuilder.homeDetailDestinations(onBack: () -> Unit) {
    composable<HomeMenuRoute> { entry ->
        HomeMenuScreen(
            menu = entry.toRoute<HomeMenuRoute>().menu,
            onBack = onBack,
            modifier = Modifier.safeDrawingPadding(),
        )
    }
}
