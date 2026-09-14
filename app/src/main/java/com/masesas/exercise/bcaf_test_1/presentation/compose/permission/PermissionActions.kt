package com.masesas.exercise.bcaf_test_1.presentation.compose.permission

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Membuat URI tujuan di cache aplikasi untuk foto yang akan diambil kamera. */
fun createCaptureUri(context: Context): Uri {
    val directory = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File.createTempFile("capture_", ".jpg", directory)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** Menghitung gambar yang terbaca di galeri — bukti izin media benar-benar berlaku. */
suspend fun countGalleryImages(context: Context): Result<Int> = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            null,
            null,
            null,
        ).use { cursor -> cursor?.count ?: 0 }
    }
}

/**
 * Lokasi terakhir yang diketahui perangkat. Bisa null kalau layanan lokasi mati atau belum ada
 * satu pun fix — pemanggil wajib menangani kasus itu.
 */
@SuppressLint("MissingPermission")
fun lastKnownLocation(context: Context): Location? {
    val manager = context.getSystemService(LocationManager::class.java) ?: return null
    return manager.getProviders(true).firstNotNullOfOrNull { provider ->
        runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
    }
}
