package com.masesas.exercise.bcaf_test_1.presentation.compose.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.component.PlaceholderScreen

@Preview
@Composable
fun FormProfileScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.screen_title_format, stringResource(R.string.title_form_profile)),
        modifier = modifier,
    )
}
