package com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.theme.Radius

private val PlaceholderIconSize = 24.dp
private val ProgressIndicatorSize = 20.dp
private val ProgressIndicatorStroke = 2.dp

/**
 * Pemuat gambar dari jaringan dengan state loading dan error yang seragam.
 *
 * Semua gambar remote di aplikasi harus lewat komponen ini supaya perilaku
 * placeholder-nya konsisten dan pemanggil tidak perlu tahu detail Coil.
 */
@Composable
fun AppNetworkImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radius.md),
    contentScale: ContentScale = ContentScale.Crop
) {
    SubcomposeAsyncImage(
        model = imageUrl,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier.clip(shape),
        loading = { ImagePlaceholder(showProgress = true) },
        error = { ImagePlaceholder(showProgress = false) }
    )
}

@Composable
private fun ImagePlaceholder(showProgress: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(ProgressIndicatorSize),
                strokeWidth = ProgressIndicatorStroke,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                imageVector = Icons.Filled.BrokenImage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(PlaceholderIconSize)
            )
        }
    }
}
