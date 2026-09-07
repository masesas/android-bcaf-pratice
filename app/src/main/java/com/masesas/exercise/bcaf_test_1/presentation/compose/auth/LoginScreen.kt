package com.masesas.exercise.bcaf_test_1.presentation.compose.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component.AppPasswordField
import com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component.AppPrimaryButton
import com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component.AppSecondaryButton
import com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component.AppTextField
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.core.ui.asString
import com.masesas.exercise.bcaf_test_1.core.ui.toUiMessage
import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthField
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.sharedActivityViewModel
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.theme.MyBcafTest1Theme
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.theme.Spacing
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthUiState
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthViewModel

@Composable
fun LoginScreen(
    onOpenRegister: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = sharedActivityViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("customer.seed08@masesas.test") }
    var password by rememberSaveable { mutableStateOf("password123") }

    LoginScreen(
        uiState = uiState,
        email = email,
        password = password,
        onEmailChange = {
            email = it
            viewModel.clearFieldError(AuthField.EMAIL)
        },
        onPasswordChange = {
            password = it
            viewModel.clearFieldError(AuthField.PASSWORD)
        },
        onSubmit = { viewModel.login(email = email, password = password) },
        onOpenRegister = onOpenRegister,
        modifier = modifier,
    )
}

@Composable
private fun LoginScreen(
    uiState: AuthUiState,
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpenRegister: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterVertically),
    ) {
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AppTextField(
            value = email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.login_hint_email),
            errorMessage = uiState.errorOf(AuthField.EMAIL)?.toUiMessage()?.asString(),
            enabled = !uiState.isSubmitting,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
        )

        AppPasswordField(
            value = password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.login_hint_password),
            errorMessage = uiState.errorOf(AuthField.PASSWORD)?.toUiMessage()?.asString(),
            enabled = !uiState.isSubmitting,
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        )

        uiState.generalFailure?.let { failure ->
            Text(
                text = failure.toUiMessage().asString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        AppPrimaryButton(
            text = stringResource(R.string.login_action),
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSubmitting,
        )

        AppSecondaryButton(
            text = stringResource(R.string.login_action_register),
            onClick = onOpenRegister,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSubmitting,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    MyBcafTest1Theme {
        LoginScreen(
            uiState = AuthUiState(),
            email = "customer@masesas.test",
            password = "password123",
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onOpenRegister = {},
        )
    }
}
