package com.masesas.exercise.bcaf_test_1.presentation.compose.home

import androidx.annotation.Keep
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.ui.graphics.vector.ImageVector
import com.masesas.exercise.bcaf_test_1.R

@Keep
enum class HomeMenu(@param:StringRes val titleRes: Int, val icon: ImageVector) {
    Simulation(R.string.home_menu_simulation, Icons.Outlined.Calculate),
    Application(R.string.home_menu_application, Icons.Outlined.Description),
    Promo(R.string.home_menu_promo, Icons.Outlined.LocalOffer),
    Help(R.string.home_menu_help, Icons.Outlined.SupportAgent),
}
