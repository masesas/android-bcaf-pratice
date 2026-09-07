package com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component.AppCard
import com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component.AppIconBox
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.theme.Spacing

@Composable
fun AppMenuItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            AppIconBox(icon = icon)
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}
