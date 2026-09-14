package com.masesas.exercise.bcaf_test_1.notification

import com.masesas.exercise.bcaf_test_1.domain.notification.model.NotificationTopic
import com.masesas.exercise.bcaf_test_1.domain.notification.repository.DeviceRegistrationRepository

class FakeDeviceRegistrationRepository : DeviceRegistrationRepository {

    var registerCallCount = 0
        private set

    var unregisterCallCount = 0
        private set

    var subscribedTopics: Set<NotificationTopic> = emptySet()
        private set

    override suspend fun ensureRegistered() {
        registerCallCount++
    }

    override suspend fun syncInstallationId(installationId: String) = Unit

    override fun subscribe(topics: Set<NotificationTopic>) {
        subscribedTopics = subscribedTopics + topics
    }

    override fun unsubscribe(topics: Set<NotificationTopic>) {
        subscribedTopics = subscribedTopics - topics
    }

    override suspend fun unregister() {
        unregisterCallCount++
        subscribedTopics = emptySet()
    }
}
