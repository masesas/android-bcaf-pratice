package com.masesas.exercise.bcaf_test_1.presentation.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.masesas.exercise.bcaf_test_1.presentation.compose.auth.navigation.AuthGraph
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.theme.MyBcafTest1Theme
import com.masesas.exercise.bcaf_test_1.presentation.compose.navigation.AppRoot
import com.masesas.exercise.bcaf_test_1.presentation.compose.navigation.MainRoute
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

                if (authState.isRestoringSession) {
                    SessionRestoringIndicator()
                } else {
                    AppRoot(
                        startDestination = remember {
                            if (authState.isLoggedIn) MainRoute else AuthGraph
                        },
                        isLoggedIn = authState.isLoggedIn,
                        isLoggedOut = authState.isLoggedOut,
                        onLogout = authViewModel::logout,
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionRestoringIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
