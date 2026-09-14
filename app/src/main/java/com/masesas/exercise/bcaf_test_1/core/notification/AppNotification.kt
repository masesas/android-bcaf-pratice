package com.masesas.exercise.bcaf_test_1.core.notification

data class AppNotification(
    val title: String,
    val body: String,
    val channel: NotificationChannelType = NotificationChannelType.GENERAL,
    val deepLink: String? = null,
    val id: Int? = null,
)
