package com.masesas.exercise.bcaf_test_1.presentation.compose.home

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component.AppCard
import com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component.AppSecondaryButton
import com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component.AppSectionHeader
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.core.permission.AppPermission
import com.masesas.exercise.bcaf_test_1.core.permission.openLocationServicesSettings
import com.masesas.exercise.bcaf_test_1.core.permission.openPermissionSettings
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.theme.Radius
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.theme.Spacing
import com.masesas.exercise.bcaf_test_1.presentation.compose.permission.PermissionOutcome
import com.masesas.exercise.bcaf_test_1.presentation.compose.permission.countGalleryImages
import com.masesas.exercise.bcaf_test_1.presentation.compose.permission.createCaptureUri
import com.masesas.exercise.bcaf_test_1.presentation.compose.permission.lastKnownLocation
import com.masesas.exercise.bcaf_test_1.presentation.compose.permission.rememberPermissionRequester
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape

private val PreviewSize = 96.dp

/** Tombol demo izin runtime: minta izin, lalu langsung jalankan aksinya begitu diberikan. */
@Composable
fun HomePermissionSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var message by remember { mutableStateOf<String?>(null) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }
    var blockedPermission by remember { mutableStateOf<AppPermission?>(null) }
    var locationServicesOff by remember { mutableStateOf(false) }

    var captureUri by remember { mutableStateOf<Uri?>(null) }
    val cameraCancelled = stringResource(R.string.permission_camera_cancelled)
    val cameraSaved = stringResource(R.string.permission_camera_saved)
    val cameraUnavailable = stringResource(R.string.permission_camera_unavailable)
    val imageSelected = stringResource(R.string.permission_image_selected)
    val imageCancelled = stringResource(R.string.permission_image_cancelled)
    val storageError = stringResource(R.string.permission_storage_error)
    val locationUnavailable = stringResource(R.string.permission_location_unavailable)

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        previewUri = captureUri.takeIf { saved }
        message = if (saved) cameraSaved else cameraCancelled
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        previewUri = uri
        message = if (uri != null) imageSelected else imageCancelled
    }

    val requester = rememberPermissionRequester { permission, outcome ->
        blockedPermission = permission.takeIf { outcome == PermissionOutcome.PermanentlyDenied }
        locationServicesOff = false
        if (outcome != PermissionOutcome.Granted) {
            previewUri = null
            message = context.getString(
                if (outcome == PermissionOutcome.Denied) {
                    R.string.permission_denied
                } else {
                    R.string.permission_permanently_denied
                },
                context.getString(permission.labelRes),
            )
            return@rememberPermissionRequester
        }

        when (permission) {
            AppPermission.Camera -> {
                val target = createCaptureUri(context)
                captureUri = target
                try {
                    takePicture.launch(target)
                } catch (_: ActivityNotFoundException) {
                    message = cameraUnavailable
                }
            }

            AppPermission.ImagePicker -> pickImage.launch("image/*")

            AppPermission.Storage -> scope.launch {
                previewUri = null
                message = countGalleryImages(context).fold(
                    onSuccess = { count -> context.getString(R.string.permission_storage_result, count) },
                    onFailure = { storageError },
                )
            }

            AppPermission.Location -> {
                previewUri = null
                val location = lastKnownLocation(context)
                locationServicesOff = location == null
                message = location?.let {
                    context.getString(R.string.permission_location_result, it.latitude, it.longitude)
                } ?: locationUnavailable
            }
        }
    }

    AppCard(modifier = modifier.fillMaxWidth()) {
        AppSectionHeader(title = stringResource(R.string.permission_section_title))
        Text(
            text = stringResource(R.string.permission_section_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AppPermission.entries.chunked(2).forEach { rowPermissions ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                rowPermissions.forEach { permission ->
                    AppSecondaryButton(
                        text = stringResource(permission.labelRes),
                        onClick = { requester.request(permission) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        message?.let { text ->
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }

        previewUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = stringResource(R.string.permission_result_image),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(PreviewSize).clip(RoundedCornerShape(Radius.md)),
            )
        }

        val settingsUnavailable = stringResource(R.string.permission_settings_unavailable)
        if (blockedPermission != null) {
            AppSecondaryButton(
                text = stringResource(R.string.permission_open_settings),
                onClick = { if (!context.openPermissionSettings()) message = settingsUnavailable },
            )
        } else if (locationServicesOff) {
            AppSecondaryButton(
                text = stringResource(R.string.permission_location_open_services),
                onClick = { if (!context.openLocationServicesSettings()) message = settingsUnavailable },
            )
        }
    }
}
