package com.example.genggaminmobile.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.genggaminmobile.domain.repository.ContractRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker for syncing pending contracts in the background.
 *
 * This worker:
 * 1. Uploads pending contract PDFs to Cloudinary
 * 2. Submits loans to the backend
 *
 * It is triggered when:
 * - Network becomes available after being offline
 * - Scheduled periodic sync
 * - Manual sync request
 */
@HiltWorker
class ContractSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val contractRepository: ContractRepository,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ContractSyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting contract sync work...")

        return try {
            val result = contractRepository.syncPendingContracts()

            result.fold(
                onSuccess = { syncedCount ->
                    Log.d(TAG, "Contract sync completed successfully. Synced: $syncedCount")
                    Result.success()
                },
                onFailure = { error ->
                    Log.e(TAG, "Contract sync failed", error)
                    // Retry if there are still pending items
                    if (runAttemptCount < 3) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "Contract sync error", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
