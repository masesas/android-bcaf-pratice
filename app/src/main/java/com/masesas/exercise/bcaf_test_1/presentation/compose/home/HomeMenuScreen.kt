package com.masesas.exercise.bcaf_test_1.presentation.compose.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component.AppIconBox
import com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component.AppSecondaryButton
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.component.PlaceholderScreen

@Composable
fun HomeMenuScreen(menu: HomeMenu, onBack: () -> Unit, modifier: Modifier = Modifier) {
    PlaceholderScreen(title = stringResource(menu.titleRes), modifier = modifier) {
        AppIconBox(icon = menu.icon)
        Text(
            text = stringResource(R.string.home_menu_placeholder),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        AppSecondaryButton(text = stringResource(R.string.home_back), onClick = onBack)
    }
}
