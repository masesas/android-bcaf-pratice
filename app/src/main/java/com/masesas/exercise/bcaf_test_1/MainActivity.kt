package com.masesas.exercise.bcaf_test_1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.masesas.exercise.bcaf_test_1.core.collectOnLifecycle
import com.masesas.exercise.bcaf_test_1.databinding.ActivityMainBinding
import com.masesas.exercise.bcaf_test_1.presentation.auth.showHomeDestinationDialog
import com.masesas.exercise.bcaf_test_1.presentation.compose.HomeActivityCompose
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthUiState
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Router aplikasi — tidak punya UI selain indikator loading.
 *
 * [AuthViewModel] membaca session dari DataStore saat dibuat; selama status masih
 * [com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthStatus.UNKNOWN] layar ini
 * menahan tampilan agar tidak berkedip ke Login untuk user yang sebenarnya sudah login.
 *
 * Sudah login → dialog pemilih stack UI. Belum login → layar login Compose di [HomeActivityCompose].
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding
    private var destinationDialog: AlertDialog? = null
    private var loginLaunched = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { startRouting() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNotificationPermission()
    }

    /** Routing ditahan sampai dialog izin selesai; kalau tidak, finish() menutup dialog itu sendiri. */
    private fun requestNotificationPermission() {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

        if (granted) startRouting()
        else notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun startRouting() {
        authViewModel.uiState.collectOnLifecycle(this) { state -> route(state) }
    }

    override fun onDestroy() {
        destinationDialog?.dismiss()
        destinationDialog = null
        super.onDestroy()
    }

    private fun route(state: AuthUiState) {
        binding.progressRouting.isVisible = state.isRestoringSession

        when {
            state.isRestoringSession -> Unit
            state.isLoggedIn -> showDestinationDialog(state)
            else -> goToLogin()
        }
    }

    private fun showDestinationDialog(state: AuthUiState) {
        if (destinationDialog?.isShowing == true) return

        val label = state.user?.name?.takeIf { it.isNotBlank() }
            ?: state.user?.email.orEmpty()
        destinationDialog = showHomeDestinationDialog(label)
    }

    /** Flag mencegah Activity login dibuka dua kali kalau state ter-emit ulang. */
    private fun goToLogin() {
        if (loginLaunched) return
        loginLaunched = true

        startActivity(Intent(this, HomeActivityCompose::class.java))
        finish()
    }
}
