package com.masesas.exercise.bcaf_test_1.presentation.compose.notification

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.component.PlaceholderScreen

@Preview
@Composable
fun NotificationScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.screen_title_format, stringResource(R.string.title_notification)),
        modifier = modifier,
    )
}
