package com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.theme.MyBcafTest1Theme
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.theme.Radius
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.theme.Spacing

private val BorderWidth = 1.dp

/**
 * Chip pilihan tunggal, dipakai untuk memilih satu opsi dari beberapa alternatif.
 *
 * Ditandai dengan [Role.RadioButton] agar pembaca layar mengumumkannya sebagai pilihan
 * yang saling meniadakan, bukan sekadar tombol.
 */
@Composable
fun AppChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(Radius.pill),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        border = if (selected) {
            null
        } else {
            BorderStroke(BorderWidth, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppChoiceChipPreview() {
    MyBcafTest1Theme {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AppChoiceChip(text = "6 bulan", selected = false, onClick = {})
            AppChoiceChip(text = "12 bulan", selected = true, onClick = {})
            AppChoiceChip(text = "24 bulan", selected = false, onClick = {})
        }
    }
}
