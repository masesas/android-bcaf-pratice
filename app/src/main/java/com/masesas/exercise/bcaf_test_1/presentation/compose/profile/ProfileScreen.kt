package com.masesas.exercise.bcaf_test_1.presentation.compose.profile

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.component.PlaceholderScreen

@Composable
fun ProfileScreen(
    onOpenFormProfile: () -> Unit,
    onOpenDokumenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(R.string.screen_title_format, stringResource(R.string.title_profile)),
        modifier = modifier,
    ) {
        Button(onClick = onOpenFormProfile) {
            Text(stringResource(R.string.compose_action_edit_profile))
        }
        Button(onClick = onOpenDokumenProfile) {
            Text(stringResource(R.string.compose_action_dokumen_profile))
        }
    }
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    ProfileScreen(onOpenFormProfile = {}, onOpenDokumenProfile = {})
}
