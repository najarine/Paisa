package com.paisa.najarine.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.paisa.najarine.data.local.PaisaDatabase

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = PaisaDatabase.getDatabase(applicationContext)
            val syncManager = FirestoreSyncManager(applicationContext, database)
            val flushedCount = syncManager.flushOutbox()
            Log.d("SyncWorker", "Flushed $flushedCount outbox mutations")
            Result.success()
        } catch (e: Exception) {
            Log.w("SyncWorker", "Outbox flush failed, will retry: ${e.message}")
            if (runAttemptCount < 5) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
