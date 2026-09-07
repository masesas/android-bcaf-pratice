package com.masesas.exercise.bcaf_test_1.presentation.compose.profile.navigation

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
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

fun NavGraphBuilder.profileDetailDestinations() {
    composable<FormProfileRoute> {
        FormProfileScreen(modifier = Modifier.safeDrawingPadding())
    }
    composable<DokumenProfileRoute> {
        DokumenProfileScreen(modifier = Modifier.safeDrawingPadding())
    }
}
