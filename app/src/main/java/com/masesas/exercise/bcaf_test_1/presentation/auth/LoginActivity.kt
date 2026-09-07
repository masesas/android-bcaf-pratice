package com.masesas.exercise.bcaf_test_1.presentation.auth

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.snackbar.Snackbar
import com.masesas.exercise.bcaf_test_1.core.collectOnLifecycle
import com.masesas.exercise.bcaf_test_1.core.ui.resolve
import com.masesas.exercise.bcaf_test_1.core.ui.toUiMessage
import com.masesas.exercise.bcaf_test_1.databinding.ActivityLoginBinding
import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthField
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthUiState
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Form login. Seluruh logika auth ada di [AuthViewModel] → AuthRepository (domain) →
 * AuthApi + DataStore (data); Activity ini hanya merender state dan meneruskan input.
 */
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var binding: ActivityLoginBinding
    private var destinationDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpForm()
        observeState()
    }

    override fun onDestroy() {
        destinationDialog?.dismiss()
        destinationDialog = null
        super.onDestroy()
    }

    private fun setUpForm() = with(binding) {
        etEmail.setText("customer1@masesas.test")
        etPassword.setText("password123")

        btnLogin.setOnClickListener {
            authViewModel.login(
                email = etEmail.text?.toString().orEmpty(),
                password = etPassword.text?.toString().orEmpty(),
            )
        }

        etEmail.doAfterTextChanged { authViewModel.clearFieldError(AuthField.EMAIL) }
        etPassword.doAfterTextChanged { authViewModel.clearFieldError(AuthField.PASSWORD) }
    }

    private fun observeState() {
        authViewModel.uiState.collectOnLifecycle(this) { state ->
            renderForm(state)
            renderFailure(state)

            if (state.isLoggedIn) showDestinationDialog(state)
        }
    }

    private fun renderForm(state: AuthUiState) = with(binding) {
        tilEmail.error = state.errorOf(AuthField.EMAIL)?.let { resolve(it.toUiMessage()) }
        tilPassword.error = state.errorOf(AuthField.PASSWORD)?.let { resolve(it.toUiMessage()) }

        btnLogin.isEnabled = !state.isSubmitting
        progressLogin.isVisible = state.isSubmitting
    }

    private fun renderFailure(state: AuthUiState) {
        val failure = state.generalFailure ?: return

        Snackbar.make(binding.root, resolve(failure.toUiMessage()), Snackbar.LENGTH_LONG).show()
        authViewModel.clearFailure()
    }

    /** Dialog didorong oleh state, bukan event, supaya selamat dari rotasi layar. */
    private fun showDestinationDialog(state: AuthUiState) {
        if (destinationDialog?.isShowing == true) return

        val label = state.user?.name?.takeIf { it.isNotBlank() }
            ?: state.user?.email.orEmpty()
        destinationDialog = showHomeDestinationDialog(label)
    }
}
