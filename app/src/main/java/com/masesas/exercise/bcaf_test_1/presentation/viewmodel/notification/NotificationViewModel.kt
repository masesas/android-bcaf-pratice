package com.masesas.exercise.bcaf_test_1.presentation.viewmodel.notification

import androidx.lifecycle.ViewModel
import com.masesas.exercise.bcaf_test_1.core.notification.AppNotification
import com.masesas.exercise.bcaf_test_1.core.notification.AppNotifier
import com.masesas.exercise.bcaf_test_1.core.notification.NotificationChannelType
import com.masesas.exercise.bcaf_test_1.presentation.compose.transaction.navigation.TRANSACTION_DETAIL_DEEP_LINK
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notifier: AppNotifier,
) : ViewModel() {

    fun sendManualNotification(title: String, body: String) {
        notifier.show(
            AppNotification(
                title = title,
                body = body,
                channel = NotificationChannelType.TRANSACTION,
                deepLink = "$TRANSACTION_DETAIL_DEEP_LINK/$SAMPLE_TRANSACTION_ID",
            )
        )
    }

    private companion object {
        const val SAMPLE_TRANSACTION_ID = "TRX-001"
    }
}
