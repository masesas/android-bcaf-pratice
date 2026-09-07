package com.masesas.exercise.bcaf_test_1.presentation.compose.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component.AppCard
import com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component.AppIconBox
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.theme.Spacing

@Composable
fun HomeHeader(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    AppCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        contentPadding = PaddingValues(Spacing.none),
    ) {
        Box {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = colors.primary.copy(alpha = 0.08f),
                    radius = size.width * 0.42f,
                    center = Offset(size.width, 0f),
                )
                drawCircle(
                    color = colors.primary.copy(alpha = 0.08f),
                    radius = size.width * 0.28f,
                    center = Offset(size.width, 0f),
                )
            }
            Column(
                modifier = Modifier.padding(Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                AppIconBox(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                )
                Text(stringResource(R.string.home_greeting), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.home_headline), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.home_description), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
