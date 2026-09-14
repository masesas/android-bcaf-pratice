package com.masesas.exercise.bcaf_test_1.core.permission

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Membuka halaman izin aplikasi di Settings — satu-satunya jalan keluar ketika user memilih
 * "Don't allow" dua kali, karena sejak saat itu dialog izin tidak akan muncul lagi.
 *
 * Android tidak menyediakan deep link ke satu izin tertentu, jadi tujuan yang paling spesifik
 * yang tersedia adalah halaman detail aplikasi tempat semua izin runtime dikelola.
 */
fun Context.openPermissionSettings(): Boolean {
    val appDetails = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    )
    return startSettings(appDetails) || startSettings(Intent(Settings.ACTION_SETTINGS))
}

/** Izin lokasi tidak berguna selama layanan lokasi perangkat mati; ini membuka toggle-nya. */
fun Context.openLocationServicesSettings(): Boolean =
    startSettings(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))

private fun Context.startSettings(intent: Intent): Boolean = try {
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: ActivityNotFoundException) {
    false
}
