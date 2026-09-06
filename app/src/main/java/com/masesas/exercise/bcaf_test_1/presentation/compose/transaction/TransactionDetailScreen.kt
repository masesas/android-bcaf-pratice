package com.masesas.exercise.bcaf_test_1.presentation.compose.transaction

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.component.PlaceholderScreen

@Composable
fun TransactionDetailScreen(
    transactionId: String,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(
            R.string.screen_title_format,
            stringResource(R.string.title_transaction_detail),
        ),
        modifier = modifier,
    ) {
        Text(text = "ID: $transactionId")
    }
}

@Preview
@Composable
private fun TransactionDetailScreenPreview() {
    TransactionDetailScreen(transactionId = "TRX-001")
}
