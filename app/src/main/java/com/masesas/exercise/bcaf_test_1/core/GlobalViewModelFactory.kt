package com.masesas.exercise.bcaf_test_1.core

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthViewModel

internal fun globalViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    viewModelFactory {
        initializer { AuthViewModel(authRepository = container.authRepository) }
    }
