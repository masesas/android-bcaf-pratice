package com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Skala jarak tunggal untuk seluruh aplikasi — hindari angka `dp` liar di dalam screen. */
object Spacing {
    val none: Dp = 0.dp
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp
}

/** Radius sudut standar komponen. */
object Radius {
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val pill: Dp = 999.dp
}

/** Ketinggian bayangan standar komponen. */
object Elevation {
    val none: Dp = 0.dp
    val low: Dp = 1.dp
    val medium: Dp = 3.dp
    val high: Dp = 6.dp
}
