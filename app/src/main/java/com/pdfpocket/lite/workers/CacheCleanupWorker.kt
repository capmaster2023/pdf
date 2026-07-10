package com.pdfpocket.lite.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pdfpocket.lite.storage.StorageRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CacheCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val storage: StorageRepository
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = runCatching {
        storage.clearTemporaryFiles()
        Result.success()
    }.getOrElse { Result.retry() }
}
