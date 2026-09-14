package com.masesas.exercise.bcaf_test_1.presentation.compose.profile.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.component.fullScreenComposable
import com.masesas.exercise.bcaf_test_1.presentation.compose.profile.DokumenProfileScreen
import com.masesas.exercise.bcaf_test_1.presentation.compose.profile.FormProfileScreen
import com.masesas.exercise.bcaf_test_1.presentation.compose.profile.ProfileScreen

fun NavGraphBuilder.profileTab(
    onOpenFormProfile: () -> Unit,
    onOpenDokumenProfile: () -> Unit,
) {
    composable<ProfileRoute> {
        ProfileScreen(
            onOpenFormProfile = onOpenFormProfile,
            onOpenDokumenProfile = onOpenDokumenProfile,
        )
    }
}

fun NavGraphBuilder.profileDetailDestinations(navController: NavHostController) {
    fullScreenComposable<FormProfileRoute>(navController = navController) { _, innerPadding ->
        FormProfileScreen(modifier = Modifier
            .padding(innerPadding)
            .safeDrawingPadding())
    }
    composable<DokumenProfileRoute> {
        DokumenProfileScreen(modifier = Modifier.safeDrawingPadding())
    }
}
