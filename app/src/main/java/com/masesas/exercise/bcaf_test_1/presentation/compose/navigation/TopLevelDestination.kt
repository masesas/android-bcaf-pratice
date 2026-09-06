package com.masesas.exercise.bcaf_test_1.presentation.compose.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.presentation.compose.home.navigation.HomeGraph
import com.masesas.exercise.bcaf_test_1.presentation.compose.home.navigation.HomeRoute
import com.masesas.exercise.bcaf_test_1.presentation.compose.notification.navigation.NotificationGraph
import com.masesas.exercise.bcaf_test_1.presentation.compose.notification.navigation.NotificationRoute
import com.masesas.exercise.bcaf_test_1.presentation.compose.profile.navigation.ProfileGraph
import com.masesas.exercise.bcaf_test_1.presentation.compose.profile.navigation.ProfileRoute
import com.masesas.exercise.bcaf_test_1.presentation.compose.transaction.navigation.TransactionGraph
import com.masesas.exercise.bcaf_test_1.presentation.compose.transaction.navigation.TransactionRoute

/**
 * Satu-satunya definisi item bottom navigation.
 *
 * [graph] dipakai untuk menentukan tab mana yang aktif (child ikut menyorot tab induknya),
 * [startRoute] dipakai untuk menentukan apakah bottom navigation boleh tampil.
 */
enum class TopLevelDestination(
    val graph: AppRoute,
    val startRoute: AppRoute,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    HOME(HomeGraph, HomeRoute, R.string.title_home, R.drawable.ic_nav_home),
    TRANSACTION(TransactionGraph, TransactionRoute, R.string.title_transaction, R.drawable.ic_nav_transaction),
    NOTIFICATION(NotificationGraph, NotificationRoute, R.string.title_notification, R.drawable.ic_nav_notification),
    PROFILE(ProfileGraph, ProfileRoute, R.string.title_profile, R.drawable.ic_nav_profile),
}
