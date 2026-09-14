package com.masesas.exercise.bcaf_test_1.data.notification.remote

import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Pembungkus SDK Firebase agar repository tidak memanggil API statis dan tetap bisa diuji. */
class FirebaseMessagingClient(
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance(),
) {

    suspend fun register() {
        messaging.register().await()
    }

    suspend fun unregister() {
        messaging.unregister().await()
    }

    // Firebase menyimpan dan mengulang operasi topic sendiri, jadi hasilnya tidak perlu ditunggu.
    fun subscribe(topicId: String) {
        messaging.subscribeToTopic(topicId)
    }

    fun unsubscribe(topicId: String) {
        messaging.unsubscribeFromTopic(topicId)
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
    }
}
