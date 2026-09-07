package com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.theme.MyBcafTest1Theme
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.theme.Spacing

/**
 * Input kata sandi dengan tombol tampilkan/sembunyikan isi.
 *
 * Status terlihat/tersembunyi disimpan di dalam komponen dan bertahan melewati
 * perubahan konfigurasi, namun sengaja tidak diangkat ke pemanggil karena murni
 * urusan tampilan.
 */
@Composable
fun AppPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorMessage: String? = null,
    isRequired: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var isValueVisible by rememberSaveable { mutableStateOf(false) }

    AppTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        helperText = helperText,
        errorMessage = errorMessage,
        isRequired = isRequired,
        enabled = enabled,
        leadingIcon = leadingIcon,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        keyboardActions = keyboardActions,
        visualTransformation = if (isValueVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailing = {
            IconButton(
                onClick = { isValueVisible = !isValueVisible },
                enabled = enabled
            ) {
                Icon(
                    imageVector = if (isValueVisible) {
                        Icons.Filled.VisibilityOff
                    } else {
                        Icons.Filled.Visibility
                    },
                    contentDescription = if (isValueVisible) {
                        "Sembunyikan kata sandi"
                    } else {
                        "Tampilkan kata sandi"
                    }
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun AppPasswordFieldPreview() {
    MyBcafTest1Theme() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            AppPasswordField(
                value = "rahasia123",
                onValueChange = {},
                label = "Kata Sandi",
                isRequired = true,
                helperText = "Minimal 8 karakter"
            )
            AppPasswordField(
                value = "123",
                onValueChange = {},
                label = "Kata Sandi Baru",
                errorMessage = "Kata sandi terlalu pendek"
            )
        }
    }
}
