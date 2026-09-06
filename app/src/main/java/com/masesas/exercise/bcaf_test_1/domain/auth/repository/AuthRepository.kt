package com.masesas.exercise.bcaf_test_1.domain.auth.repository

import com.masesas.exercise.bcaf_test_1.domain.common.AppResult
import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthSession
import com.masesas.exercise.bcaf_test_1.domain.auth.model.LoginCredentials
import com.masesas.exercise.bcaf_test_1.domain.auth.model.RegisterCredentials
import kotlinx.coroutines.flow.Flow

/**
 * Kontrak akses data autentikasi.
 *
 * Ini satu-satunya hal yang boleh diketahui presentation layer soal auth — tidak ada
 * DataStore, tidak ada HTTP client, tidak ada Android framework yang bocor ke sini.
 * Implementasi mock hari ini bisa ditukar implementasi jaringan besok tanpa mengubah ViewModel.
 *
 * ### Sumber kebenaran
 * [observeSession] adalah satu-satunya sumber kebenaran status login. [login], [register], dan
 * [logout] menulis ke penyimpanan; perubahan status akan muncul lewat [observeSession].
 * Return value ketiganya dipakai untuk penanganan error dan event sekali-jalan (mis. navigasi),
 * bukan untuk menyetel state login secara manual.
 */
interface AuthRepository {

    /**
     * Session yang sedang tersimpan, `null` bila tidak ada user yang login.
     *
     * Flow ini hot terhadap penyimpanan: emisi pertama adalah isi penyimpanan saat itu,
     * lalu setiap perubahan (login/logout) menyusul. Tidak pernah melempar — kegagalan baca
     * dilaporkan sebagai `null`.
     *
     * Session yang sudah lewat masa berlaku dipancarkan sebagai `null`, sehingga collector
     * tidak pernah melihatnya sebagai keadaan login.
     */
    fun observeSession(): Flow<AuthSession?>

    /**
     * Membaca session tersimpan sekali saat app start dan memvalidasi masa berlakunya.
     *
     * @return [AppResult.Success] dengan session bila masih berlaku, atau `null` bila memang
     *   belum pernah login. [AppResult.Failure] dengan
     *   [com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthFailure.SessionExpired] bila session
     *   ada tapi kedaluwarsa — session tersebut sudah dihapus saat fungsi ini kembali.
     */
    suspend fun restoreSession(): AppResult<AuthSession?>

    /**
     * Autentikasi dengan email dan password. Menyimpan session bila berhasil.
     */
    suspend fun login(credentials: LoginCredentials): AppResult<AuthSession>

    /**
     * Mendaftarkan akun baru lalu langsung login. Menyimpan session bila berhasil.
     */
    suspend fun register(credentials: RegisterCredentials): AppResult<AuthSession>

    /**
     * Menghapus session tersimpan. Idempoten — memanggilnya saat sudah logout tetap sukses.
     */
    suspend fun logout(): AppResult<Unit>
}
