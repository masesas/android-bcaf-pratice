package com.masesas.exercise.bcaf_test_1.presentation.compose

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.masesas.exercise.bcaf_test_1.core.notification.AppNotifier
import com.masesas.exercise.bcaf_test_1.presentation.compose.auth.navigation.AuthGraph
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.theme.MyBcafTest1Theme
import com.masesas.exercise.bcaf_test_1.presentation.compose.navigation.AppRoot
import com.masesas.exercise.bcaf_test_1.presentation.compose.navigation.MainRoute
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivityCompose : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    private var pendingDeepLink by mutableStateOf<Uri?>(null)

    @Inject
    lateinit var notifier: AppNotifier

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingDeepLink = intent.data
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyBcafTest1Theme {
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()
                var navReady by remember { mutableStateOf(false) }

                LaunchedEffect(authState.status) {
                    navReady = !authState.isRestoringSession
                }

                if (navReady) {
                    AppRoot(
                        startDestination = remember {
                            if (authState.isLoggedIn) MainRoute else AuthGraph
                        },
                        isLoggedIn = authState.isLoggedIn,
                        isLoggedOut = authState.isLoggedOut,
                        onLogout = authViewModel::logout,
                        deepLink = pendingDeepLink,
                        onDeepLinkHandled = { pendingDeepLink = null },
                    )
                } else {
                    SessionRestoringIndicator()
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
