package com.masesas.exercise.bcaf_test_1.presentation.compose.home.navigation

import com.masesas.exercise.bcaf_test_1.presentation.compose.home.HomeMenu
import com.masesas.exercise.bcaf_test_1.presentation.compose.navigation.AppRoute
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : AppRoute

@Serializable
data class HomeMenuRoute(val menu: HomeMenu) : AppRoute
