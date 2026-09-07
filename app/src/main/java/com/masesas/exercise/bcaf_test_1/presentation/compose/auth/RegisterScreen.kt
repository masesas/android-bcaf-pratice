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
fun RegisterScreen(
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = sharedActivityViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    RegisterScreen(
        uiState = uiState,
        name = name,
        email = email,
        password = password,
        onNameChange = {
            name = it
            viewModel.clearFieldError(AuthField.NAME)
        },
        onEmailChange = {
            email = it
            viewModel.clearFieldError(AuthField.EMAIL)
        },
        onPasswordChange = {
            password = it
            viewModel.clearFieldError(AuthField.PASSWORD)
        },
        onSubmit = { viewModel.register(name = name, email = email, password = password) },
        onBackToLogin = onBackToLogin,
        modifier = modifier,
    )
}

@Composable
private fun RegisterScreen(
    uiState: AuthUiState,
    name: String,
    email: String,
    password: String,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBackToLogin: () -> Unit,
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
            text = stringResource(R.string.register_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.register_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AppTextField(
            value = name,
            onValueChange = onNameChange,
            label = stringResource(R.string.register_hint_name),
            errorMessage = uiState.errorOf(AuthField.NAME)?.toUiMessage()?.asString(),
            enabled = !uiState.isSubmitting,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
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
            text = stringResource(R.string.register_action),
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSubmitting,
        )

        AppSecondaryButton(
            text = stringResource(R.string.register_action_login),
            onClick = onBackToLogin,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSubmitting,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    MyBcafTest1Theme {
        RegisterScreen(
            uiState = AuthUiState(),
            name = "Khesa",
            email = "customer@masesas.test",
            password = "password123",
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onBackToLogin = {},
        )
    }
}
