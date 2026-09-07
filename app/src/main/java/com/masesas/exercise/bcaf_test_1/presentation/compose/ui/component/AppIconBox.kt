package com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.theme.Radius

private val DefaultBoxSize = 44.dp
private val DefaultIconSize = 22.dp

/**
 * Ikon di dalam kotak berwarna dengan sudut membulat.
 * Dipakai sebagai elemen `leading` pada kartu dan list item.
 */
@Composable
fun AppIconBox(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    boxSize: Dp = DefaultBoxSize,
    iconSize: Dp = DefaultIconSize
) {
    Box(
        modifier = modifier
            .size(boxSize)
            .background(color = containerColor, shape = RoundedCornerShape(Radius.md)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
