package com.masesas.exercise.bcaf_test_1.domain.sync.repository

import com.masesas.exercise.bcaf_test_1.domain.common.AppResult
import com.masesas.exercise.bcaf_test_1.domain.sync.model.PendingUpload
import kotlinx.coroutines.flow.Flow

/** Antrian outbox: satu-satunya sumber data yang diawasi dan dikirim oleh worker. */
interface PendingUploadRepository {

    fun observePendingCount(): Flow<Int>

    suspend fun pendingCount(): Int

    /** Menyimpan payload ke Room lalu memicu worker. */
    suspend fun enqueue(endpoint: String, payload: String)

    suspend fun nextBatch(limit: Int): List<PendingUpload>

    suspend fun upload(item: PendingUpload): AppResult<Unit>

    suspend fun markUploaded(id: Long)

    suspend fun markFailed(id: Long, reason: String)
}
