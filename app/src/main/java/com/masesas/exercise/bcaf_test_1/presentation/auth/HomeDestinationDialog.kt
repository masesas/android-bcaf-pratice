package com.masesas.exercise.bcaf_test_1.presentation.auth

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.presentation.compose.HomeActivityCompose
import com.masesas.exercise.bcaf_test_1.presentation.legacy.HomeActivityLegacy

/**
 * Pemilih stack UI setelah session dipastikan valid.
 *
 * Dipakai dua pemanggil: MainActivity (session sudah tersimpan saat app dibuka) dan
 * LoginActivity (session baru saja dibuat). Membatalkan dialog menutup Activity pemanggil
 * supaya tidak menyisakan layar router yang kosong.
 */
fun Activity.showHomeDestinationDialog(userLabel: String): AlertDialog =
    MaterialAlertDialogBuilder(this)
        .setTitle(R.string.home_choice_title)
        .setMessage(getString(R.string.home_choice_message, userLabel))
        .setPositiveButton(R.string.home_choice_compose) { _, _ ->
            openHome(HomeActivityCompose::class.java)
        }
        .setNegativeButton(R.string.home_choice_legacy) { _, _ ->
            openHome(HomeActivityLegacy::class.java)
        }
        .setOnCancelListener { finish() }
        .show()

/** Activity pemanggil ditutup agar tombol back dari Home tidak kembali ke dialog. */
private fun Activity.openHome(target: Class<out Activity>) {
    startActivity(Intent(this, target))
    finish()
}
