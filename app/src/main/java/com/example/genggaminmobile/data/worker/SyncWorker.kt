package com.example.genggaminmobile.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.genggaminmobile.domain.repository.CustomerRepository
import com.example.genggaminmobile.domain.repository.LoanRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val loanRepository: LoanRepository,
    private val customerRepository: CustomerRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val loanResult = loanRepository.syncUnsyncedLoans()
            val profileResult = customerRepository.syncPendingProfile()
            
            if (loanResult.isSuccess && profileResult.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
