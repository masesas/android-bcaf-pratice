package com.masesas.exercise.bcaf_test_1.presentation.compose.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.core.ext.toRupiahDigits
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanProduct

@Composable
fun LoanProductItem(product: LoanProduct, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (!product.isActive) {
                Text(
                    text = stringResource(R.string.loan_product_inactive),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Text(text = product.code, style = MaterialTheme.typography.bodySmall)

        Text(
            text = stringResource(
                R.string.loan_product_interest_format,
                product.interestPercent.toString(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = stringResource(
                R.string.loan_product_tenor_format,
                product.tenorMinMonths,
                product.tenorMaxMonths,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(
                R.string.loan_product_plafond_format,
                product.plafondMin.toRupiahDigits(),
                product.plafondMax.toRupiahDigits(),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    HorizontalDivider()
}
