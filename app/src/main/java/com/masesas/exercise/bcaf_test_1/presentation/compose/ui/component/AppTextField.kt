package com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.theme.MyBcafTest1Theme
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.theme.Radius
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.theme.Spacing

private const val REQUIRED_INDICATOR = " *"

/** Nilai bawaan bersama untuk seluruh varian input field. */
object AppTextFieldDefaults {

    val shape: Shape get() = RoundedCornerShape(Radius.md)

    /** Warna field: garis tepi mengikuti warna merek saat fokus, netral saat tidak. */
    @Composable
    fun colors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary
    )
}

/**
 * Input teks standar aplikasi.
 *
 * Labelnya berupa teks statis di atas kotak input — bukan label mengambang bawaan
 * Material 3 — supaya keterangan field tetap terbaca saat sedang diisi.
 *
 * Baris bantuan di bawah field menampilkan [errorMessage] bila ada; jika tidak,
 * [helperText] yang ditampilkan. Field dianggap bermasalah ketika [errorMessage]
 * tidak `null`, jadi pemanggil tidak perlu mengatur flag error terpisah.
 */
@Composable
fun AppTextField(
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
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    leadingIcon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    shape: Shape = AppTextFieldDefaults.shape
) {
    val isError = errorMessage != null
    val supportingMessage = errorMessage ?: helperText

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        if (label != null) {
            Text(
                text = fieldLabel(label = label, isRequired = isRequired),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            isError = isError,
            singleLine = singleLine,
            maxLines = maxLines,
            shape = shape,
            colors = AppTextFieldDefaults.colors(),
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            prefix = prefix,
            placeholder = placeholder?.let {
                { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
            },
            leadingIcon = leadingIcon?.let {
                { Icon(imageVector = it, contentDescription = null) }
            },
            trailingIcon = trailing,
            supportingText = supportingMessage?.let {
                { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
        )
    }
}

/** Menyusun label beserta penanda wajib isi berwarna merah bila diperlukan. */
@Composable
private fun fieldLabel(label: String, isRequired: Boolean): AnnotatedString =
    if (!isRequired) {
        AnnotatedString(label)
    } else {
        val errorColor = MaterialTheme.colorScheme.error
        buildAnnotatedString {
            append(label)
            withStyle(SpanStyle(color = errorColor)) { append(REQUIRED_INDICATOR) }
        }
    }

@Preview(showBackground = true)
@Composable
private fun AppTextFieldPreview() {
    MyBcafTest1Theme {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            AppTextField(
                value = "",
                onValueChange = {},
                label = "Nama Lengkap",
                placeholder = "Sesuai KTP",
                isRequired = true,
                helperText = "Gunakan nama tanpa gelar"
            )
            AppTextField(
                value = "3271",
                onValueChange = {},
                label = "Nomor KTP",
                errorMessage = "Nomor KTP harus 16 digit"
            )
            AppTextField(
                value = "Tidak dapat diubah",
                onValueChange = {},
                label = "Cabang",
                enabled = false
            )
        }
    }
}
