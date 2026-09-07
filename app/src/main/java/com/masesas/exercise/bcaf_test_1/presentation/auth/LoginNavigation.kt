package com.masesas.exercise.bcaf_test_1.presentation.auth

import android.app.Activity
import android.content.Intent

/**
 * Membuka LoginActivity sebagai root task baru.
 *
 * `CLEAR_TASK` membuang seluruh back stack Home, sehingga tombol back setelah logout keluar
 * dari aplikasi — bukan kembali ke layar yang sudah tidak boleh diakses.
 */
fun Activity.navigateToLoginAfterLogout() {
    val intent = Intent(this, LoginActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

    startActivity(intent)
    finish()
}
