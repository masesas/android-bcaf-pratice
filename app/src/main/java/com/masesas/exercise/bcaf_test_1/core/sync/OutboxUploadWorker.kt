package com.masesas.exercise.bcaf_test_1.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.masesas.exercise.bcaf_test_1.core.sync.SyncDefaults.BATCH_SIZE
import com.masesas.exercise.bcaf_test_1.core.sync.SyncDefaults.MAX_BATCH_PER_RUN
import com.masesas.exercise.bcaf_test_1.core.sync.SyncDefaults.MAX_RUN_ATTEMPTS
import com.masesas.exercise.bcaf_test_1.domain.common.AppResult
import com.masesas.exercise.bcaf_test_1.domain.common.CommonFailure
import com.masesas.exercise.bcaf_test_1.domain.sync.model.PendingUpload
import com.masesas.exercise.bcaf_test_1.domain.sync.repository.PendingUploadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Menguras antrian outbox Room lewat HTTP POST.
 *
 * Urutan: verifikasi kualitas koneksi -> ambil batch dari Room -> POST per item ->
 * tandai terkirim / gagal -> ulangi sampai antrian kosong atau batas batch tercapai.
 */
@HiltWorker
class OutboxUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: PendingUploadRepository,
    private val networkQualityChecker: NetworkQualityChecker,
) : CoroutineWorker(appContext, params) {

    private data class BatchOutcome(val uploaded: Int, val retryable: Boolean)

    override suspend fun doWork(): Result {
        Log.i(TAG, "mulai, percobaan ke-${runAttemptCount + 1} dari $MAX_RUN_ATTEMPTS")

        return when (val quality = networkQualityChecker.current()) {
            is NetworkQuality.Degraded -> {
                Log.w(TAG, "koneksi belum layak: ${quality.reason} (${quality.downstreamKbps} kbps)")
                retryOrGiveUp("koneksi ${quality.reason}")
            }

            is NetworkQuality.Good -> {
                Log.i(TAG, "koneksi layak (${quality.downstreamKbps} kbps), mulai menguras outbox")
                drainOutbox()
            }
        }
    }

    private suspend fun drainOutbox(): Result {
        var uploaded = 0

        repeat(MAX_BATCH_PER_RUN) { round ->
            val batch = repository.nextBatch(BATCH_SIZE)

            if (batch.isEmpty()) {
                Log.i(TAG, "outbox kosong, selesai. total terkirim=$uploaded")
                return Result.success(workDataOf(KEY_UPLOADED_COUNT to uploaded))
            }

            Log.d(TAG, "batch ${round + 1} berisi ${batch.size} item")
            val outcome = uploadBatch(batch)
            uploaded += outcome.uploaded

            if (outcome.retryable) {
                Log.w(TAG, "berhenti karena kegagalan jaringan, terkirim=$uploaded")
                return retryOrGiveUp("kegagalan jaringan saat POST")
            }
        }

        Log.i(TAG, "batas $MAX_BATCH_PER_RUN batch tercapai, sisa antrian dijadwalkan ulang")
        return Result.retry()
    }

    private suspend fun uploadBatch(batch: List<PendingUpload>): BatchOutcome {
        var uploaded = 0

        batch.forEach { item ->
            when (val result = repository.upload(item)) {
                is AppResult.Success -> {
                    repository.markUploaded(item.id)
                    uploaded++
                    Log.d(TAG, "POST sukses id=${item.id} endpoint=${item.endpoint}")
                }

                is AppResult.Failure -> {
                    val failure = result.failure
                    Log.w(TAG, "POST gagal id=${item.id} failure=$failure")

                    // Kegagalan jaringan bersifat sementara: hentikan batch, biarkan backoff yang mengatur.
                    if (failure is CommonFailure.Network) return BatchOutcome(uploaded, retryable = true)

                    repository.markFailed(item.id, failure.toString())
                }
            }
        }

        return BatchOutcome(uploaded, retryable = false)
    }

    private fun retryOrGiveUp(reason: String): Result =
        if (runAttemptCount + 1 >= MAX_RUN_ATTEMPTS) {
            Log.e(TAG, "menyerah setelah $MAX_RUN_ATTEMPTS percobaan: $reason")
            Result.failure(workDataOf(KEY_FAILURE_REASON to reason))
        } else {
            Log.i(TAG, "menjadwalkan percobaan ulang: $reason")
            Result.retry()
        }

    companion object {
        private const val TAG = "OutboxUploadWorker"
        const val KEY_UPLOADED_COUNT = "uploaded_count"
        const val KEY_FAILURE_REASON = "failure_reason"
    }
}
