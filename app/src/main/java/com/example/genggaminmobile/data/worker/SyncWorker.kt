package com.example.genggaminmobile.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.genggaminmobile.domain.repository.ContractRepository
import com.example.genggaminmobile.domain.repository.CustomerRepository
import com.example.genggaminmobile.domain.repository.LoanRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Main sync worker that syncs all pending data when network is available
 */
@HiltWorker
class SyncWorker
@AssistedInject
constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val loanRepository: LoanRepository,
    private val customerRepository: CustomerRepository,
    private val contractRepository: ContractRepository,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting sync work...")

        return try {
            // Sync loans
            val loanResult = loanRepository.syncUnsyncedLoans()
            Log.d(TAG, "Loan sync: ${if (loanResult.isSuccess) "success" else "failed"}")

            // Sync profiles
            val profileResult = customerRepository.syncPendingProfile()
            Log.d(TAG, "Profile sync: ${if (profileResult.isSuccess) "success" else "failed"}")

            // Sync contracts
            val contractResult = contractRepository.syncPendingContracts()
            Log.d(TAG, "Contract sync: ${if (contractResult.isSuccess) "success" else "failed"}")

            if (loanResult.isSuccess && profileResult.isSuccess && contractResult.isSuccess) {
                Log.d(TAG, "All sync completed successfully")
                Result.success()
            } else {
                Log.w(TAG, "Some sync failed, will retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync error", e)
            Result.retry()
        }
    }
}
