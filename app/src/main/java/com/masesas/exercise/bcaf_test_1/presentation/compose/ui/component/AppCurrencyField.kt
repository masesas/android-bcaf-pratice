package com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.theme.MyBcafTest1Theme
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.theme.Spacing

/**
 * Input nominal rupiah.
 *
 * [value] berisi digit polos tanpa pemisah (`"25000000"`) — itulah yang diteruskan ke
 * [onValueChange] — sedangkan yang tampil di layar sudah berpemisah ribuan dan berawalan
 * "Rp". Karakter non-digit yang diketik atau ditempel akan dibuang, dan panjangnya
 * dibatasi [maxDigits] agar hasilnya selalu aman diubah dengan `value.toLongOrNull()`.
 */
@Composable
fun AppCurrencyField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorMessage: String? = null,
    isRequired: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    maxDigits: Int = 15,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    AppTextField(
        value = value,
        onValueChange = { input ->
            val digits = input.filter(Char::isDigit).take(maxDigits)
            if (digits != value) onValueChange(digits)
        },
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        helperText = helperText,
        errorMessage = errorMessage,
        isRequired = isRequired,
        enabled = enabled,
        readOnly = readOnly,
        prefix = {
            Text(
                text = "Rp",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction
        ),
        keyboardActions = keyboardActions,
        visualTransformation = ThousandSeparatorVisualTransformation()
    )
}

@Preview(showBackground = true)
@Composable
private fun AppCurrencyFieldPreview() {
    MyBcafTest1Theme {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            AppCurrencyField(
                value = "25000000",
                onValueChange = {},
                label = "Jumlah Pinjaman",
                isRequired = true,
                helperText = "Maksimal Rp 200.000.000"
            )
            AppCurrencyField(
                value = "",
                onValueChange = {},
                label = "Uang Muka",
                placeholder = "0"
            )
            AppCurrencyField(
                value = "300000000",
                onValueChange = {},
                label = "Jumlah Pinjaman",
                errorMessage = "Melebihi limit yang tersedia"
            )
        }
    }
}
