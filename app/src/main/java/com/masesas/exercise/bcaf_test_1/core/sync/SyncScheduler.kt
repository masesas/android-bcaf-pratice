package com.masesas.exercise.bcaf_test_1.core.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.masesas.exercise.bcaf_test_1.core.sync.SyncDefaults.BACKOFF_DELAY_SECONDS
import com.masesas.exercise.bcaf_test_1.core.sync.SyncDefaults.ONE_SHOT_WORK_NAME
import com.masesas.exercise.bcaf_test_1.core.sync.SyncDefaults.PERIODIC_INTERVAL_MINUTES
import com.masesas.exercise.bcaf_test_1.core.sync.SyncDefaults.PERIODIC_WORK_NAME
import com.masesas.exercise.bcaf_test_1.core.sync.SyncDefaults.WORK_TAG
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Satu-satunya pintu penjadwalan [OutboxUploadWorker]. */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val workManager = WorkManager.getInstance(context)

    /** Jaring pengaman berkala; aman dipanggil berulang karena memakai policy KEEP. */
    fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<OutboxUploadWorker>(
            PERIODIC_INTERVAL_MINUTES, TimeUnit.MINUTES,
        )
            .setConstraints(constraints(requiresBatteryNotLow = true))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_SECONDS, TimeUnit.SECONDS)
            .addTag(WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
        Log.i(TAG, "sync berkala terjadwal setiap $PERIODIC_INTERVAL_MINUTES menit")
    }

    /** Dipanggil otomatis saat ada data baru, atau manual dari tombol retry. */
    fun syncNow(reason: String = REASON_MANUAL) {
        val request = OneTimeWorkRequestBuilder<OutboxUploadWorker>()
            .setConstraints(constraints(requiresBatteryNotLow = false))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_SECONDS, TimeUnit.SECONDS)
            .addTag(WORK_TAG)
            .build()

        // APPEND_OR_REPLACE: data yang masuk saat worker sedang jalan tetap kebagian giliran.
        workManager.enqueueUniqueWork(
            ONE_SHOT_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
        Log.i(TAG, "sync sekali jalan di-enqueue, alasan=$reason")
    }

    fun observeSyncState(): Flow<List<WorkInfo>> =
        workManager.getWorkInfosByTagFlow(WORK_TAG)

    fun cancelAll() {
        workManager.cancelAllWorkByTag(WORK_TAG)
        Log.i(TAG, "semua pekerjaan sync dibatalkan")
    }

    private fun constraints(requiresBatteryNotLow: Boolean): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(requiresBatteryNotLow)
            .build()

    companion object {
        private const val TAG = "SyncScheduler"
        const val REASON_MANUAL = "manual"
        const val REASON_NEW_DATA = "data-baru"
        const val REASON_APP_START = "app-start"
    }
}
