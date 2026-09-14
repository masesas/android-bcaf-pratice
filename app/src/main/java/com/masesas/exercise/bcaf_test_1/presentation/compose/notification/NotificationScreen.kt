package com.masesas.exercise.bcaf_test_1.presentation.compose.notification

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component.AppPrimaryButton
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.component.PlaceholderScreen
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.theme.MyBcafTest1Theme
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.notification.NotificationViewModel

@Composable
fun NotificationScreen(
    modifier: Modifier = Modifier,
    viewModel: NotificationViewModel = hiltViewModel(),
) {
    val title = stringResource(R.string.notification_manual_title)
    val body = stringResource(R.string.notification_manual_body)

    NotificationScreen(
        onSendNotification = { viewModel.sendManualNotification(title = title, body = body) },
        modifier = modifier,
    )
}

@Composable
private fun NotificationScreen(
    onSendNotification: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(R.string.screen_title_format, stringResource(R.string.title_notification)),
        modifier = modifier,
        actions = {
            AppPrimaryButton(
                text = stringResource(R.string.notification_manual_action),
                onClick = onSendNotification,
            )
        },
    )
}

@Preview
@Composable
private fun NotificationScreenPreview() {
    MyBcafTest1Theme {
        NotificationScreen(onSendNotification = {})
    }
}
