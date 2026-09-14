package com.masesas.exercise.bcaf_test_1.domain.notification.repository

import com.masesas.exercise.bcaf_test_1.domain.notification.model.NotificationTopic

/**
 * Kontrak pendaftaran perangkat ke layanan push.
 *
 * Kegagalan tidak pernah dilempar ke pemanggil: pendaftaran bukan aksi yang dipicu user dan
 * tidak punya UI untuk melaporkan error. Kegagalan dicatat ke log dan dikirim ulang otomatis
 * pada [ensureRegistered] berikutnya.
 */
interface DeviceRegistrationRepository {

    /** Idempoten — panggil saat app start dan setiap kali sesi berubah. */
    suspend fun ensureRegistered()

    /** Dipanggil entry point push saat identitas perangkat terbit atau berubah. */
    suspend fun syncInstallationId(installationId: String)

    fun subscribe(topics: Set<NotificationTopic>)

    fun unsubscribe(topics: Set<NotificationTopic>)

    /** Melepas perangkat dari backend, semua topic, dan FCM. Panggil saat logout. */
    suspend fun unregister()
}
