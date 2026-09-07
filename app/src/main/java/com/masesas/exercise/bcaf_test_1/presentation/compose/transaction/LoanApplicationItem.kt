package com.masesas.exercise.bcaf_test_1.presentation.compose.transaction

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.core.ext.toRupiahDigits
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanApplication
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanApplicationStatus

@Composable
fun LoanApplicationItem(
    application: LoanApplication,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = application.loanProductCode,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(application.status.labelRes()),
                style = MaterialTheme.typography.labelMedium,
                color = application.status.labelColor(),
            )
        }

        Text(
            text = stringResource(
                R.string.loan_application_amount_format,
                application.submittedAmount.toRupiahDigits(),
            ),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = stringResource(R.string.loan_application_tenor_format, application.tenorMonths),
            style = MaterialTheme.typography.bodyMedium,
        )

        application.note?.takeIf { it.isNotBlank() }?.let { note ->
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    HorizontalDivider()
}

@StringRes
private fun LoanApplicationStatus.labelRes(): Int = when (this) {
    LoanApplicationStatus.DRAFT -> R.string.loan_application_status_draft
    LoanApplicationStatus.SUBMITTED -> R.string.loan_application_status_submitted
    LoanApplicationStatus.APPROVED -> R.string.loan_application_status_approved
    LoanApplicationStatus.REJECTED -> R.string.loan_application_status_rejected
    LoanApplicationStatus.DISBURSED -> R.string.loan_application_status_disbursed
    LoanApplicationStatus.CANCELLED -> R.string.loan_application_status_cancelled
    LoanApplicationStatus.UNKNOWN -> R.string.loan_application_status_unknown
}

@Composable
private fun LoanApplicationStatus.labelColor(): Color = when (this) {
    LoanApplicationStatus.REJECTED,
    LoanApplicationStatus.CANCELLED,
        -> MaterialTheme.colorScheme.error

    LoanApplicationStatus.APPROVED,
    LoanApplicationStatus.DISBURSED,
        -> MaterialTheme.colorScheme.primary

    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
