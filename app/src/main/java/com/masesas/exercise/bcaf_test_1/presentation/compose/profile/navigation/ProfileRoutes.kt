package com.masesas.exercise.bcaf_test_1.presentation.compose.profile.navigation

import com.masesas.exercise.bcaf_test_1.presentation.compose.navigation.AppRoute
import kotlinx.serialization.Serializable

@Serializable
data object ProfileGraph : AppRoute

@Serializable
data object ProfileRoute : AppRoute

@Serializable
data object FormProfileRoute : AppRoute

@Serializable
data object DokumenProfileRoute : AppRoute
