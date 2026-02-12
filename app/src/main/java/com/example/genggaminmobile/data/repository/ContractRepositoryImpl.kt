package com.example.genggaminmobile.data.repository

import android.content.Context
import android.util.Log
import com.example.genggaminmobile.data.local.dao.LoanDao
import com.example.genggaminmobile.data.local.dao.LoanLimitDao
import com.example.genggaminmobile.data.local.dao.PendingContractDao
import com.example.genggaminmobile.data.local.entity.ContractStatus
import com.example.genggaminmobile.data.local.entity.PendingContractEntity
import com.example.genggaminmobile.data.local.entity.toEntity
import com.example.genggaminmobile.data.model.dto.LoanRequest
import com.example.genggaminmobile.data.remote.api.LoanApi
import com.example.genggaminmobile.data.worker.ContractSyncWorker
import com.example.genggaminmobile.domain.model.Loan
import com.example.genggaminmobile.domain.repository.ContractRepository
import com.example.genggaminmobile.domain.util.CloudinaryUploadResult
import com.example.genggaminmobile.domain.util.CloudinaryUploader
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ContractRepository with offline-first support.
 *
 * Workflow:
 * 1. Save contract locally (PDF + metadata)
 * 2. Try to upload immediately if online
 * 3. If offline, WorkManager will handle upload when online
 * 4. After successful upload, submit loan to backend
 */
@Singleton
class ContractRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pendingContractDao: PendingContractDao,
    private val loanApi: LoanApi,
    private val loanDao: LoanDao,
    private val loanLimitDao: LoanLimitDao,
) : ContractRepository {

    companion object {
        private const val TAG = "ContractRepository"
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    override suspend fun savePendingContract(contract: PendingContractEntity): Long {
        Log.d(TAG, "Saving pending contract: ${contract.customerName}")

        // Insert the contract first
        val contractId = pendingContractDao.insertContract(contract)

        // Reduce available limit locally to prevent double submission
        try {
            val limitEntity = loanLimitDao.getLimitByPlafondId(contract.plafondId)
            if (limitEntity != null) {
                val newAvailable = if (limitEntity.availableLimit >= contract.amount) {
                    limitEntity.availableLimit - contract.amount
                } else {
                    0L
                }
                loanLimitDao.updateLimit(limitEntity.copy(availableLimit = newAvailable))
                Log.d(TAG, "Reduced limit for plafond ${contract.plafondId}: ${limitEntity.availableLimit} -> $newAvailable")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update limit", e)
        }

        return contractId
    }

    override fun getPendingContracts(): Flow<List<PendingContractEntity>> {
        return pendingContractDao.getAllPendingContracts()
    }

    override fun getPendingContractsCount(): Flow<Int> {
        return pendingContractDao.countPendingContractsFlow()
    }

    override suspend fun syncPendingContracts(): Result<Int> {
        Log.d(TAG, "Starting sync of pending contracts...")

        var syncedCount = 0
        var hasErrors = false

        try {
            // Step 0a: Cleanup any stale SUBMITTED contracts that weren't deleted
            pendingContractDao.deleteSubmittedContracts()

            // Step 0b: Reset stale UPLOADING contracts
            // If a contract has been in UPLOADING status for more than 30 seconds,
            // it's likely stuck due to a crash or timeout, so reset it to PENDING_UPLOAD
            val allContracts = pendingContractDao.getUnsyncedContracts()
            for (contract in allContracts) {
                if (contract.status == ContractStatus.UPLOADING) {
                    val timeSinceLastAttempt = contract.lastAttemptAt?.let {
                        System.currentTimeMillis() - it
                    } ?: Long.MAX_VALUE

                    if (timeSinceLastAttempt > 30000) { // 30 seconds
                        Log.d(TAG, "Resetting stale UPLOADING contract: ${contract.id}")
                        pendingContractDao.updateStatus(contract.id, ContractStatus.PENDING_UPLOAD)
                    }
                }
            }

            // Step 1: Upload pending contracts
            val contractsToUpload = pendingContractDao.getContractsToUpload()
            Log.d(TAG, "Found ${contractsToUpload.size} contracts to upload")

            for (contract in contractsToUpload) {
                val uploadResult = uploadContractPdf(contract.id)
                if (uploadResult.isSuccess) {
                    syncedCount++
                } else {
                    // Best Effort approach: If upload fails, we still want to try submitting the loan
                    // because the backend might not strictly require the PDF URL.
                    Log.w(TAG, "Upload failed for ${contract.id}, but proceeding to submission (Best Effort)")
                    
                    // Force update status to UPLOADED so Step 2 picks it up
                    pendingContractDao.updateStatus(contract.id, ContractStatus.UPLOADED)
                    
                    // We don't increment syncedCount or set hasErrors here because 
                    // the real success depending on Step 2 (Submission)
                }
            }

            // Step 2: Submit loans for uploaded contracts
            // This will now include the ones that failed upload but were forced to UPLOADED
            val contractsToSubmit = pendingContractDao.getContractsToSubmit()
            Log.d(TAG, "Found ${contractsToSubmit.size} contracts to submit")

            for (contract in contractsToSubmit) {
                val submitResult = submitContractLoan(contract.id)
                if (submitResult.isSuccess) {
                    syncedCount++
                } else {
                    hasErrors = true
                }
            }

            Log.d(TAG, "Sync completed. Synced: $syncedCount, HasErrors: $hasErrors")

            return if (hasErrors) {
                Result.failure(Exception("Some contracts failed to sync"))
            } else {
                Result.success(syncedCount)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync", e)
            return Result.failure(e)
        }
    }

    override suspend fun uploadContractPdf(contractId: Long): Result<String> {
        val contract = pendingContractDao.getContractById(contractId)
            ?: return Result.failure(Exception("Contract not found"))

        // Check if already uploaded
        if (contract.status == ContractStatus.UPLOADED ||
            contract.status == ContractStatus.PENDING_SUBMIT ||
            contract.status == ContractStatus.SUBMITTED
        ) {
            return Result.success(contract.contractUrl ?: "")
        }

        // Check max retry attempts
        if (contract.uploadAttempts >= MAX_RETRY_ATTEMPTS) {
            pendingContractDao.updateStatus(contractId, ContractStatus.FAILED)
            return Result.failure(Exception("Max retry attempts exceeded"))
        }

        try {
            // Update status to uploading
            pendingContractDao.updateStatus(contractId, ContractStatus.UPLOADING)

            val pdfFile = File(contract.pdfFilePath)
            if (!pdfFile.exists()) {
                pendingContractDao.updateWithError(
                    contractId,
                    ContractStatus.FAILED,
                    "PDF file not found",
                )
                return Result.failure(Exception("PDF file not found: ${contract.pdfFilePath}"))
            }

            Log.d(TAG, "Uploading PDF for contract $contractId: ${pdfFile.absolutePath}")

            val uploadResult = CloudinaryUploader.uploadPdf(pdfFile)

            return when (uploadResult) {
                is CloudinaryUploadResult.Success -> {
                    Log.d(TAG, "Upload success: ${uploadResult.secureUrl}")
                    pendingContractDao.updateWithUploadResult(
                        contractId,
                        ContractStatus.UPLOADED,
                        uploadResult.secureUrl,
                    )
                    Result.success(uploadResult.secureUrl)
                }
                is CloudinaryUploadResult.Error -> {
                    Log.e(TAG, "Upload failed: ${uploadResult.message}")
                    pendingContractDao.updateWithError(
                        contractId,
                        ContractStatus.PENDING_UPLOAD,
                        uploadResult.message,
                    )
                    Result.failure(Exception(uploadResult.message))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload exception", e)
            pendingContractDao.updateWithError(
                contractId,
                ContractStatus.PENDING_UPLOAD,
                e.message ?: "Unknown error",
            )
            return Result.failure(e)
        }
    }

    override suspend fun submitContractLoan(contractId: Long): Result<Unit> {
        val contract = pendingContractDao.getContractById(contractId)
            ?: return Result.failure(Exception("Contract not found"))

        try {
            pendingContractDao.updateStatus(contractId, ContractStatus.PENDING_SUBMIT)

            Log.d(TAG, "Submitting loan for contract $contractId")
            Log.d(TAG, "Contract data: amount=${contract.amount}, tenor=${contract.tenor}, purpose=${contract.purpose}, plafondId=${contract.plafondId}, interestRate=${contract.interestRate}, lat=${contract.latitude}, lng=${contract.longitude}")

            val request = LoanRequest(
                amount = contract.amount,
                tenureMonths = contract.tenor,
                purpose = contract.purpose,
                plafondId = contract.plafondId,
                interestRate = contract.interestRate,
                latitude = contract.latitude,
                longitude = contract.longitude,
            )

            // Log the exact JSON body that Retrofit/Gson will send
            val jsonBody = Gson().toJson(request)
            Log.d(TAG, "=== EXACT JSON REQUEST BODY ===")
            Log.d(TAG, jsonBody)
            Log.d(TAG, "===============================")

            val response = loanApi.submitLoan(request)

            return if (response.success) {
                Log.d(TAG, "Loan submitted successfully")

                // Save loan to local database first
                val loan = Loan(
                    id = response.data?.id,
                    amount = contract.amount,
                    tenorMonths = contract.tenor,
                    purpose = contract.purpose,
                    plafondId = contract.plafondId,
                    status = response.data?.status ?: "PENDING",
                    interestRate = contract.interestRate,
                    date = java.text.SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss",
                        java.util.Locale.getDefault(),
                    ).format(java.util.Date(contract.createdAt)),
                )

                loanDao.insertLoan(
                    loan.toEntity(isSynced = true),
                )

                // Delete the PDF file to save space
                try {
                    File(contract.pdfFilePath).delete()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete PDF file", e)
                }

                // Delete the pending contract from database immediately
                // This prevents duplicate entries showing in the UI
                pendingContractDao.deleteContract(contractId)
                Log.d(TAG, "Deleted pending contract: $contractId")

                Result.success(Unit)
            } else {
                Log.e(TAG, "Loan submission failed: ${response.message}")
                pendingContractDao.updateWithError(
                    contractId,
                    ContractStatus.UPLOADED, // Revert to uploaded, can retry
                    response.message ?: "Unknown error",
                )
                Result.failure(Exception(response.message))
            }
        } catch (e: retrofit2.HttpException) {
            // Extract error body for better debugging
            val errorBody = try {
                e.response()?.errorBody()?.string()
            } catch (_: Exception) {
                null
            }
            Log.e(TAG, "Loan submission HTTP error: ${e.code()} - $errorBody", e)
            pendingContractDao.updateWithError(
                contractId,
                ContractStatus.UPLOADED,
                "HTTP ${e.code()}: ${errorBody ?: e.message()}",
            )
            return Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Loan submission exception", e)
            pendingContractDao.updateWithError(
                contractId,
                ContractStatus.UPLOADED,
                e.message ?: "Network error",
            )
            return Result.failure(e)
        }
    }

    override suspend fun deleteContract(contractId: Long) {
        val contract = pendingContractDao.getContractById(contractId)
        if (contract != null) {
            // Restore the limit if contract was not submitted
            if (contract.status != ContractStatus.SUBMITTED) {
                try {
                    val limitEntity = loanLimitDao.getLimitByPlafondId(contract.plafondId)
                    if (limitEntity != null) {
                        val newAvailable = limitEntity.availableLimit + contract.amount
                        // Don't exceed total limit
                        val cappedAvailable = minOf(newAvailable, limitEntity.totalLimit)
                        loanLimitDao.updateLimit(limitEntity.copy(availableLimit = cappedAvailable))
                        Log.d(TAG, "Restored limit for plafond ${contract.plafondId}: ${limitEntity.availableLimit} -> $cappedAvailable")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to restore limit", e)
                }
            }

            // Delete the PDF file
            try {
                File(contract.pdfFilePath).delete()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete PDF file", e)
            }

            // Delete from database
            pendingContractDao.deleteContract(contractId)
        }
    }

    override suspend fun retryContract(contractId: Long): Result<Unit> {
        val contract = pendingContractDao.getContractById(contractId)
            ?: return Result.failure(Exception("Contract not found"))

        return when (contract.status) {
            ContractStatus.PENDING_UPLOAD, ContractStatus.FAILED -> {
                // Reset status for retry
                pendingContractDao.updateStatus(contractId, ContractStatus.PENDING_UPLOAD)

                // Try upload
                val uploadResult = uploadContractPdf(contractId)
                
                // Best Effort: Proceed to submit regardless of upload result
                if (uploadResult.isFailure) {
                    Log.w(TAG, "Retry upload failed, proceeding to submit anyway")
                    // Force update status to UPLOADED so submitContractLoan accepts it
                    pendingContractDao.updateStatus(contractId, ContractStatus.UPLOADED)
                }
                
                // Try submit
                submitContractLoan(contractId)
            }
            ContractStatus.UPLOADED, ContractStatus.PENDING_SUBMIT -> {
                // Try submit
                submitContractLoan(contractId)
            }
            else -> Result.success(Unit)
        }
    }

    /**
     * Schedule immediate sync using WorkManager
     */
    fun scheduleImmediateSync() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val syncRequest = androidx.work.OneTimeWorkRequest.Builder(
            ContractSyncWorker::class.java,
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS,
            )
            .build()

        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            "ContractSync",
            androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE,
            syncRequest,
        )
    }
}
