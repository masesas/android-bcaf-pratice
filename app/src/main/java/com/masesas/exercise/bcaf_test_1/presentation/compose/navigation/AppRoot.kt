package com.masesas.exercise.bcaf_test_1.presentation.compose.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.component.AppBottomBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val destinations = remember { TopLevelDestination.entries }
    val showBottomBar = destinations.any { currentDestination.isRoute(it.startRoute) }
    val currentTab = destinations.firstOrNull { currentDestination.isInGraph(it.graph) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(currentTab?.labelRes ?: R.string.app_name))
                },
                actions = {
                    // Logout hanya di level tab, sejajar dengan aturan bottom bar.
                    if (showBottomBar) {
                        IconButton(onClick = onLogout) {
                            Icon(
                                painter = painterResource(R.drawable.ic_logout),
                                contentDescription = stringResource(R.string.action_logout),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    destinations = destinations,
                    isSelected = { currentDestination.isInGraph(it.graph) },
                    onSelect = { navController.navigateToTopLevel(it) },
                )
            }
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

private fun NavDestination?.isRoute(route: AppRoute): Boolean =
    this?.hasRoute(route::class) == true

private fun NavDestination?.isInGraph(graph: AppRoute): Boolean =
    this?.hierarchy?.any { it.hasRoute(graph::class) } == true

private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.graph) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
