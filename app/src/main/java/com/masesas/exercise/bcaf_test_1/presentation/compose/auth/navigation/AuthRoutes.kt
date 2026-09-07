package com.masesas.exercise.bcaf_test_1.presentation.compose.auth.navigation

import com.masesas.exercise.bcaf_test_1.presentation.compose.navigation.AppRoute
import kotlinx.serialization.Serializable

@Serializable
data object AuthGraph : AppRoute

@Serializable
data object LoginRoute : AppRoute

@Serializable
data object RegisterRoute : AppRoute
