package com.masesas.exercise.bcaf_test_1.core.notification

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import com.masesas.exercise.bcaf_test_1.R

enum class NotificationChannelType(
    val id: String,
    @param:StringRes val nameRes: Int,
    val importance: Int,
    val channelId: String = id,
    @param:RawRes val soundRes: Int? = null,
) {
    GENERAL(
        id = "general",
        nameRes = R.string.notification_channel_general,
        importance = NotificationManagerCompat.IMPORTANCE_DEFAULT,
    ),
    TRANSACTION(
        id = "transaction",
        nameRes = R.string.notification_channel_transaction,
        importance = NotificationManagerCompat.IMPORTANCE_HIGH,
    ),
    PROMO(
        id = "promo",
        nameRes = R.string.notification_channel_promo,
        importance = NotificationManagerCompat.IMPORTANCE_DEFAULT,
        channelId = "promo_sound_v1",
        soundRes = R.raw.faaaa,
    );

    companion object {
        fun fromId(id: String?): NotificationChannelType =
            entries.firstOrNull { it.id == id } ?: GENERAL
    }
}
