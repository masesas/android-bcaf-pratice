package com.masesas.exercise.bcaf_test_1.core.notification

interface AppNotifier {
    fun show(notification: AppNotification): Int

    fun cancel(id: Int)
}
