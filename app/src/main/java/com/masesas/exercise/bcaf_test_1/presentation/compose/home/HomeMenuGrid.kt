package com.masesas.exercise.bcaf_test_1.presentation.compose.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component.AppSectionHeader
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.component.AppMenuItem
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.theme.Spacing

@Composable
fun HomeMenuGrid(onOpenMenu: (HomeMenu) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        AppSectionHeader(title = stringResource(R.string.home_menu_title))
        HomeMenu.entries.chunked(2).forEach { menus ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                menus.forEach { menu ->
                    AppMenuItem(
                        title = stringResource(menu.titleRes),
                        icon = menu.icon,
                        onClick = { onOpenMenu(menu) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
