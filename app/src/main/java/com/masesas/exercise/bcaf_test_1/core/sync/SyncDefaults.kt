package com.masesas.exercise.bcaf_test_1.core.sync

internal object SyncDefaults {
    const val MIN_DOWNSTREAM_KBPS = 320
    const val BATCH_SIZE = 20
    const val MAX_BATCH_PER_RUN = 5
    const val MAX_RUN_ATTEMPTS = 5
    const val BACKOFF_DELAY_SECONDS = 30L
    const val PERIODIC_INTERVAL_MINUTES = 15L
    const val WORK_TAG = "outbox-upload"
    const val PERIODIC_WORK_NAME = "outbox-upload-periodic"
    const val ONE_SHOT_WORK_NAME = "outbox-upload-now"
}
