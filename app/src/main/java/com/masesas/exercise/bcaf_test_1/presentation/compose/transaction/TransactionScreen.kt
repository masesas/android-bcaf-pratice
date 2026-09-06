package com.masesas.exercise.bcaf_test_1.presentation.compose.transaction

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.component.PlaceholderScreen

@Composable
fun TransactionScreen(
    onOpenTransactionDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(R.string.screen_title_format, stringResource(R.string.title_transaction)),
        modifier = modifier,
    ) {
        Button(onClick = onOpenTransactionDetail) {
            Text(stringResource(R.string.compose_action_open_transaction_detail))
        }
    }
}

@Preview
@Composable
private fun TransactionScreenPreview() {
    TransactionScreen(onOpenTransactionDetail = {})
}
