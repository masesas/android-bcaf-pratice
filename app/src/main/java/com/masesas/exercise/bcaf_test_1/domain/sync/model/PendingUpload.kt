package com.masesas.exercise.bcaf_test_1.domain.sync.model

// TODO: sesuaikan field dengan entity Room antrian upload yang sebenarnya.
data class PendingUpload(
    val id: Long,
    val endpoint: String,
    val payload: String,
    val attemptCount: Int,
    val createdAtMillis: Long,
)
