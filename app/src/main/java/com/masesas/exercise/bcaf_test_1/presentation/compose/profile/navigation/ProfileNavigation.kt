package com.masesas.exercise.bcaf_test_1.presentation.compose.profile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.masesas.exercise.bcaf_test_1.presentation.compose.profile.DokumenProfileScreen
import com.masesas.exercise.bcaf_test_1.presentation.compose.profile.FormProfileScreen
import com.masesas.exercise.bcaf_test_1.presentation.compose.profile.ProfileScreen

fun NavGraphBuilder.profileGraph(
    onOpenFormProfile: () -> Unit,
    onOpenDokumenProfile: () -> Unit,
) {
    navigation<ProfileGraph>(startDestination = ProfileRoute) {
        composable<ProfileRoute> {
            ProfileScreen(
                onOpenFormProfile = onOpenFormProfile,
                onOpenDokumenProfile = onOpenDokumenProfile,
            )
        }

        composable<FormProfileRoute> { FormProfileScreen() }

        composable<DokumenProfileRoute> { DokumenProfileScreen() }
    }
}
