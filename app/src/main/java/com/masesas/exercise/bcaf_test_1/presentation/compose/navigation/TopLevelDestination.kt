package com.masesas.exercise.bcaf_test_1.presentation.compose.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.presentation.compose.home.navigation.HomeRoute
import com.masesas.exercise.bcaf_test_1.presentation.compose.notification.navigation.NotificationRoute
import com.masesas.exercise.bcaf_test_1.presentation.compose.profile.navigation.ProfileRoute
import com.masesas.exercise.bcaf_test_1.presentation.compose.transaction.navigation.TransactionRoute

enum class TopLevelDestination(
    val route: AppRoute,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    HOME(HomeRoute, R.string.title_home, R.drawable.ic_nav_home),
    TRANSACTION(TransactionRoute, R.string.title_transaction, R.drawable.ic_nav_transaction),
    NOTIFICATION(NotificationRoute, R.string.title_notification, R.drawable.ic_nav_notification),
    PROFILE(ProfileRoute, R.string.title_profile, R.drawable.ic_nav_profile),
}
