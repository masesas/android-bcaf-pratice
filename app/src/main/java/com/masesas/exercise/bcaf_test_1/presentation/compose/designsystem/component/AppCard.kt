package com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.theme.Elevation
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.theme.Radius
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.theme.Spacing

/**
 * Kontainer kartu standar aplikasi.
 *
 * Semua kartu di layar manapun sebaiknya memakai komponen ini agar radius, warna,
 * dan bayangan tetap seragam. Gunakan [elevation] untuk membedakan tingkat penekanan.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(Radius.lg),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    elevation: Dp = Elevation.low,
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(Spacing.md),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CardDefaults.cardColors(
        containerColor = containerColor,
        contentColor = contentColor
    )
    val cardElevation = CardDefaults.cardElevation(defaultElevation = elevation)

    if (onClick == null) {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = cardElevation
        ) {
            AppCardContent(contentPadding, verticalArrangement, content)
        }
    } else {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = cardElevation
        ) {
            AppCardContent(contentPadding, verticalArrangement, content)
        }
    }
}

@Composable
private fun AppCardContent(
    contentPadding: PaddingValues,
    verticalArrangement: Arrangement.Vertical,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}
