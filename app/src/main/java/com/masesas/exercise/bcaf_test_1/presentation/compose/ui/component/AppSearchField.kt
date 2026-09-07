package com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.theme.MyBcafTest1Theme
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.theme.Radius
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.theme.Spacing

/**
 * Input pencarian bersudut penuh, dengan tombol hapus yang muncul saat sudah ada isian.
 *
 * Menekan tombol cari pada papan ketik akan menutup papan ketik lalu memanggil [onSearch].
 */
@Composable
fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Cari...",
    enabled: Boolean = true,
    onSearch: () -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    AppTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        enabled = enabled,
        shape = RoundedCornerShape(Radius.pill),
        leadingIcon = Icons.Filled.Search,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboardController?.hide()
                onSearch()
            }
        ),
        trailing = if (value.isEmpty()) {
            null
        } else {
            {
                IconButton(onClick = { onValueChange("") }, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Hapus teks",
                    )
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun AppSearchFieldPreview() {
    MyBcafTest1Theme {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            AppSearchField(value = "", onValueChange = {})
            AppSearchField(value = "Pinjaman multiguna", onValueChange = {})
        }
    }
}
