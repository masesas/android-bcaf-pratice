package com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.theme.Radius
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.theme.Spacing

/** Label kecil untuk menandai status atau kategori. */
@Composable
fun AppStatusChip(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
        modifier = modifier
            .background(color = containerColor, shape = RoundedCornerShape(Radius.pill))
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
    )
}
