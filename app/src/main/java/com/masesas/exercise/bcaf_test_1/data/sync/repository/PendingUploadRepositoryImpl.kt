package com.masesas.exercise.bcaf_test_1.data.sync.repository

import android.util.Log
import com.masesas.exercise.bcaf_test_1.core.sync.SyncScheduler
import com.masesas.exercise.bcaf_test_1.domain.common.AppResult
import com.masesas.exercise.bcaf_test_1.domain.sync.model.PendingUpload
import com.masesas.exercise.bcaf_test_1.domain.sync.repository.PendingUploadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kerangka outbox. Seluruh akses Room dan HTTP masih berupa TODO; alur, penjadwalan,
 * dan kontrak hasilnya sudah final sehingga worker dapat dipakai apa adanya.
 */
@Singleton
class PendingUploadRepositoryImpl @Inject constructor(
    private val syncScheduler: SyncScheduler,
    // TODO: inject PendingUploadDao (Room) dan PendingUploadApi (Retrofit) di sini.
) : PendingUploadRepository {

    // TODO: ganti dengan dao.observePendingCount() agar UI ikut bereaksi pada isi antrian.
    override fun observePendingCount(): Flow<Int> = flowOf(0)

    // TODO: ganti dengan dao.countPending().
    override suspend fun pendingCount(): Int = 0

    override suspend fun enqueue(endpoint: String, payload: String) {
        // TODO: dao.insert(PendingUploadEntity(endpoint, payload, attemptCount = 0, createdAt = now)).
        Log.d(TAG, "payload masuk antrian untuk $endpoint")
        syncScheduler.syncNow(SyncScheduler.REASON_NEW_DATA)
    }

    // TODO: ganti dengan dao.selectPending(limit) yang mengurutkan createdAt ASC.
    override suspend fun nextBatch(limit: Int): List<PendingUpload> = emptyList()

    override suspend fun upload(item: PendingUpload): AppResult<Unit> {
        // TODO: runApiCatching(json) { api.post(item.endpoint, item.payload).requirePayload() }.
        Log.d(TAG, "POST ${item.endpoint} belum terimplementasi")
        return AppResult.success(Unit)
    }

    // TODO: ganti dengan dao.deleteById(id) — item terkirim tidak perlu disimpan.
    override suspend fun markUploaded(id: Long) {
        Log.d(TAG, "item $id terkirim")
    }

    // TODO: ganti dengan dao.markFailed(id, reason, attemptCount + 1).
    override suspend fun markFailed(id: Long, reason: String) {
        Log.w(TAG, "item $id gagal permanen: $reason")
    }

    private companion object {
        const val TAG = "PendingUploadRepo"
    }
}
