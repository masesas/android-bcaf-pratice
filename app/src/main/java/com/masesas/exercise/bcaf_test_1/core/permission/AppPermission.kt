package com.masesas.exercise.bcaf_test_1.core.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.masesas.exercise.bcaf_test_1.R

/**
 * Daftar izin runtime yang dipakai aplikasi, lengkap dengan pemetaan ke nama permission Android.
 *
 * Pemetaan ditaruh di sini supaya perbedaan antar versi OS (mis. READ_MEDIA_IMAGES di API 33+
 * versus READ_EXTERNAL_STORAGE di bawahnya) hanya ditulis satu kali.
 */
enum class AppPermission(@param:StringRes val labelRes: Int) {
    Camera(R.string.permission_camera),
    ImagePicker(R.string.permission_image_picker),
    Storage(R.string.permission_storage),
    Location(R.string.permission_location);

    val manifestPermissions: List<String>
        get() = when (this) {
            Camera -> listOf(Manifest.permission.CAMERA)
            // Sejak scoped storage (API 30) tidak ada izin tulis; baca galeri memakai izin yang sama.
            ImagePicker, Storage ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            Location -> listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        }

    /** Lokasi cukup salah satu (user boleh memilih akurasi perkiraan); izin lain wajib semua. */
    private val requiresAllGranted: Boolean get() = this != Location

    fun isGrantedIn(context: Context): Boolean = evaluate { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun isGrantedIn(grants: Map<String, Boolean>): Boolean = evaluate { permission ->
        grants[permission] == true
    }

    fun shouldShowRationale(activity: Activity): Boolean = manifestPermissions.any { permission ->
        ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    private inline fun evaluate(granted: (String) -> Boolean): Boolean =
        if (requiresAllGranted) manifestPermissions.all(granted) else manifestPermissions.any(granted)
}
