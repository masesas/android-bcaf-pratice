package com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

inline fun <reified T : Any> NavGraphBuilder.fullScreenComposable(
    navController: NavHostController,
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline content: @Composable (
        NavBackStackEntry,
        PaddingValues,
    ) -> Unit,
) {
    composable<T>(
        deepLinks = deepLinks,
        enterTransition = {
            slideInHorizontally(
                animationSpec = tween(500),
                initialOffsetX = { it / 6 },
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                animationSpec = tween(500),
                targetOffsetX = { it / 6 },
            )
        },
    ) { entry ->
        val transition = transition

        val scale by transition.animateFloat(
            transitionSpec = { tween(300) },
            label = "screenScale",
        ) { state ->
            when (state) {
                EnterExitState.PreEnter -> 0.96f
                EnterExitState.Visible -> 1f
                EnterExitState.PostExit -> 0.96f
            }
        }

        BackHandler {
            navController.popBackStack()
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    clip = true
                    shape = RoundedCornerShape(
                        if (scale < 1f) 24.dp else 0.dp
                    )
                }
        ) {
            Scaffold { innerPadding ->
                content(
                    entry,
                    innerPadding,
                )
            }
        }
    }
}