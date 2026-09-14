package com.masesas.exercise.bcaf_test_1.core.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.annotation.RawRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.masesas.exercise.bcaf_test_1.R
import java.util.concurrent.atomic.AtomicInteger

class AndroidAppNotifier(
    private val context: Context,
    private val target: Class<out Activity>,
) : AppNotifier {

    private val manager = NotificationManagerCompat.from(context)
    private val nextId = AtomicInteger(BASE_ID)

    init {
        LEGACY_CHANNEL_IDS.forEach(manager::deleteNotificationChannel)
        manager.createNotificationChannelsCompat(
            NotificationChannelType.entries.map { channel ->
                NotificationChannelCompat.Builder(channel.channelId, channel.importance)
                    .setName(context.getString(channel.nameRes))
                    .apply { channel.soundRes?.let { setSound(soundUri(it), SOUND_ATTRIBUTES) } }
                    .build()
            }
        )
    }

    @SuppressLint("MissingPermission")
    override fun show(notification: AppNotification): Int {
        val id = notification.id ?: nextId.incrementAndGet()
        if (!canPost()) return id

        val built = NotificationCompat.Builder(context, notification.channel.channelId)
            .setSmallIcon(R.drawable.ic_nav_notification)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(id, notification.deepLink))
            .build()

        manager.notify(id, built)
        return id
    }

    override fun cancel(id: Int) = manager.cancel(id)

    private fun canPost(): Boolean {
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun soundUri(@RawRes res: Int): Uri =
        "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$res".toUri()

    private fun contentIntent(id: Int, deepLink: String?): PendingIntent {
        val intent = Intent(context, target).apply {
            action = Intent.ACTION_VIEW
            data = deepLink?.toUri()
        }

        return checkNotNull(
            TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(intent)
                .getPendingIntent(
                    id,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
        )
    }

    private companion object {
        const val BASE_ID = 1000

        /** Channel lama tanpa sound; dihapus agar channel promo dibuat ulang dengan sound. */
        val LEGACY_CHANNEL_IDS = listOf("promo")

        val SOUND_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
    }
}
