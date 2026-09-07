package com.masesas.exercise.bcaf_test_1.presentation.compose.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.core.ext.toRupiahDigits
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanProduct
import com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component.AppCard
import com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component.AppIconBox
import com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component.AppStatusChip
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.theme.Spacing

@Composable
fun LoanProductItem(product: LoanProduct, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconBox(icon = Icons.Outlined.AccountBalance)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(text = product.name, style = MaterialTheme.typography.titleMedium)
                Text(text = product.code, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        AppStatusChip(
            text = stringResource(if (product.isActive) R.string.loan_product_active else R.string.loan_product_inactive),
            containerColor = if (product.isActive) colors.primaryContainer else colors.errorContainer,
            contentColor = if (product.isActive) colors.onPrimaryContainer else colors.onErrorContainer,
        )
        HorizontalDivider(color = colors.outlineVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            LoanProductValue(
                label = stringResource(R.string.loan_product_interest_label),
                value = stringResource(R.string.loan_product_interest_value, product.interestPercent.toString()),
                modifier = Modifier.weight(1f),
            )
            LoanProductValue(
                label = stringResource(R.string.loan_product_tenor_label),
                value = stringResource(R.string.loan_product_tenor_value, product.tenorMinMonths, product.tenorMaxMonths),
                modifier = Modifier.weight(1f),
            )
        }
        LoanProductValue(
            label = stringResource(R.string.loan_product_plafond_label),
            value = stringResource(R.string.loan_product_plafond_value, product.plafondMin.toRupiahDigits(), product.plafondMax.toRupiahDigits()),
        )
    }
}

@Composable
private fun LoanProductValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}
