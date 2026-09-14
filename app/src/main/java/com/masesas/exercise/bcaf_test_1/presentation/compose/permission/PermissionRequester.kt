package com.masesas.exercise.bcaf_test_1.presentation.compose.permission

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.masesas.exercise.bcaf_test_1.core.permission.AppPermission

enum class PermissionOutcome {
    Granted,

    /** Ditolak sekali; dialog masih bisa dimunculkan lagi. */
    Denied,

    /** Ditolak permanen — hanya bisa dipulihkan lewat Settings. */
    PermanentlyDenied,
}

@Stable
class PermissionRequester internal constructor(private val onRequest: (AppPermission) -> Unit) {
    fun request(permission: AppPermission) = onRequest(permission)
}

/**
 * Satu requester untuk semua izin di layar: panggil [PermissionRequester.request] dan terima
 * hasilnya di [onResult]. Izin yang sudah diberikan langsung memanggil [onResult] tanpa dialog.
 */
@Composable
fun rememberPermissionRequester(
    onResult: (AppPermission, PermissionOutcome) -> Unit,
): PermissionRequester {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val currentOnResult by rememberUpdatedState(onResult)
    var pending by remember { mutableStateOf<AppPermission?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val permission = pending ?: return@rememberLauncherForActivityResult
        pending = null
        currentOnResult(
            permission,
            when {
                permission.isGrantedIn(grants) -> PermissionOutcome.Granted
                activity?.let(permission::shouldShowRationale) == true -> PermissionOutcome.Denied
                else -> PermissionOutcome.PermanentlyDenied
            },
        )
    }

    return remember(context, launcher) {
        PermissionRequester { permission ->
            if (permission.isGrantedIn(context)) {
                currentOnResult(permission, PermissionOutcome.Granted)
            } else {
                pending = permission
                launcher.launch(permission.manifestPermissions.toTypedArray())
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
