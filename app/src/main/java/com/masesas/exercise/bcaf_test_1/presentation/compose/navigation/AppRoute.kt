package com.masesas.exercise.bcaf_test_1.presentation.compose.navigation

import kotlinx.serialization.Serializable

interface AppRoute

@Serializable
data object MainRoute : AppRoute
