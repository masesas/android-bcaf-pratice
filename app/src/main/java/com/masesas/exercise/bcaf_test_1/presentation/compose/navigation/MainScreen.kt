package com.masesas.exercise.bcaf_test_1.presentation.compose.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.component.AppBottomBar
import com.masesas.exercise.bcaf_test_1.presentation.compose.home.HomeMenu
import com.masesas.exercise.bcaf_test_1.presentation.compose.home.navigation.HomeRoute
import com.masesas.exercise.bcaf_test_1.presentation.compose.home.navigation.homeTab
import com.masesas.exercise.bcaf_test_1.presentation.compose.notification.navigation.notificationTab
import com.masesas.exercise.bcaf_test_1.presentation.compose.profile.navigation.profileTab
import com.masesas.exercise.bcaf_test_1.presentation.compose.transaction.navigation.transactionTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenMenu: (HomeMenu) -> Unit,
    onOpenTransactionDetail: (String) -> Unit,
    onOpenFormProfile: () -> Unit,
    onOpenDokumenProfile: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val destinations = remember { TopLevelDestination.entries }
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val currentTab = destinations.firstOrNull { destination ->
        currentDestination?.hasRoute(destination.route::class) == true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(currentTab?.labelRes ?: R.string.app_name)) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            painter = painterResource(R.drawable.ic_logout),
                            contentDescription = stringResource(R.string.action_logout),
                        )
                    }
                },
            )
        },
        bottomBar = {
            AppBottomBar(
                destinations = destinations,
                isSelected = { it == currentTab },
                onSelect = { navController.navigateToTab(it) },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(150)) },
            exitTransition = { fadeOut(animationSpec = tween(150)) },
        ) {
            homeTab(onOpenMenu = onOpenMenu)
            transactionTab(onOpenTransactionDetail = onOpenTransactionDetail)
            notificationTab()
            profileTab(
                onOpenFormProfile = onOpenFormProfile,
                onOpenDokumenProfile = onOpenDokumenProfile,
            )
        }
    }
}

private fun NavHostController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
