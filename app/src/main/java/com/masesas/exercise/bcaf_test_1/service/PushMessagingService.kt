package com.masesas.exercise.bcaf_test_1.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.masesas.exercise.bcaf_test_1.core.notification.AppNotification
import com.masesas.exercise.bcaf_test_1.core.notification.AppNotifier
import com.masesas.exercise.bcaf_test_1.core.notification.NotificationChannelType
import com.masesas.exercise.bcaf_test_1.domain.notification.repository.DeviceRegistrationRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TOPIC_PREFIX = "/topics/"

@AndroidEntryPoint
class PushMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notifier: AppNotifier

    @Inject
    lateinit var deviceRegistration: DeviceRegistrationRepository

    /** Callback FCM berjalan di background thread, jadi aman menunggu sinkronisasi selesai. */
    override fun onRegistered(installationId: String) {
        Log.d("BISMISLLAH_TOKEN", installationId)
        //runBlocking { deviceRegistration.syncInstallationId(installationId) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = message.notification?.title ?: data[KEY_TITLE] ?: return
        val body = message.notification?.body ?: data[KEY_BODY].orEmpty()

        notifier.show(
            AppNotification(
                title = title,
                body = body,
                channel = NotificationChannelType.fromId(data[KEY_CHANNEL] ?: message.topic()),
                deepLink = data[KEY_DEEP_LINK],
            )
        )
    }

    /** Nama topic bila pesan dikirim ke topic, null bila dikirim ke perangkat. */
    private fun RemoteMessage.topic(): String? =
        from?.takeIf { it.startsWith(TOPIC_PREFIX) }?.removePrefix(TOPIC_PREFIX)

    private companion object {
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val KEY_CHANNEL = "channel"
        const val KEY_DEEP_LINK = "deeplink"
    }
}
