package com.masesas.exercise.bcaf_test_1.presentation.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.masesas.exercise.bcaf_test_1.presentation.auth.navigateToLoginAfterLogout
import com.masesas.exercise.bcaf_test_1.presentation.compose.navigation.AppRoot
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.theme.MyBcafTest1Theme
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivityCompose : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyBcafTest1Theme {
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()

                // Navigasi dipicu status, bukan hasil pemanggilan logout, supaya selamat dari rotasi.
                LaunchedEffect(authState.isLoggedOut) {
                    if (authState.isLoggedOut) navigateToLoginAfterLogout()
                }

                AppRoot(onLogout = authViewModel::logout)
            }
        }
    }
}
