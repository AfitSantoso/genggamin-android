package com.example.genggaminmobile.data.repository

import com.example.genggaminmobile.data.local.dao.LoanDao
import com.example.genggaminmobile.data.local.dao.LoanLimitDao
import com.example.genggaminmobile.data.local.entity.toDomain
import com.example.genggaminmobile.data.local.entity.toEntity
import com.example.genggaminmobile.data.model.dto.LoanRequest
import com.example.genggaminmobile.data.remote.api.LoanApi
import com.example.genggaminmobile.domain.model.Loan
import com.example.genggaminmobile.domain.model.LoanLimit
import com.example.genggaminmobile.domain.repository.LoanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRepositoryImpl @Inject constructor(
    private val loanApi: LoanApi,
    private val loanDao: LoanDao,
    private val loanLimitDao: LoanLimitDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : LoanRepository {
    override fun getMyLoans(): Flow<List<Loan>> {
        return loanDao.getAllLoans().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshLoans(): Result<Unit> {
        return try {
            val response = loanApi.getMyLoans()
            if (response.success) {
                val domains = response.data?.map { dto ->
                    Loan(
                        id = dto.id,
                        amount = dto.amount,
                        tenorMonths = dto.tenureMonths,
                        purpose = dto.purpose,
                        plafondId = dto.plafondId,
                        status = dto.status ?: "PENDING",
                        interestRate = dto.interestRate,
                        date = dto.submittedAt
                    )
                } ?: emptyList()
                
                // Clear old synced data and insert new
                loanDao.deleteSyncedLoans()
                loanDao.insertLoans(domains.map { it.toEntity(isSynced = true) })
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun submitLoan(
        amount: Long,
        tenor: Int,
        purpose: String,
        plafondId: Long,
        interestRate: Double,
        latitude: Double,
        longitude: Double
    ): Result<Unit> {
        val localLoan = Loan(
            id = null,
            amount = amount,
            tenorMonths = tenor,
            purpose = purpose,
            plafondId = plafondId,
            status = "PENDING (Offline)",
            interestRate = interestRate,
            date = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        )

        return try {
            // 1. Save to Local First (Offline First)
            val entity = localLoan.toEntity(isSynced = false).copy(
                latitude = latitude,
                longitude = longitude
            )
            val localId = loanDao.insertLoan(entity)

            // Update local limit immediately to reflect change in UI even if offline
            val limitEntity = loanLimitDao.getLimitByPlafondId(plafondId)
            if (limitEntity != null) {
                 val newAvailable = if (limitEntity.availableLimit >= amount) limitEntity.availableLimit - amount else 0
                 loanLimitDao.updateLimit(limitEntity.copy(availableLimit = newAvailable))
            }
            
            // 2. Try to sync immediately
            val response = loanApi.submitLoan(LoanRequest(amount, tenor, purpose, plafondId, latitude, longitude))
            if (response.success) {
                // 3. Update local record with remote ID and status
                val updatedEntity = entity.copy(
                    localId = localId,
                    remoteId = response.data?.id,
                    status = response.data?.status ?: "PENDING",
                    isSynced = true
                )
                loanDao.updateLoan(updatedEntity)
                Result.success(Unit)
            } else {
                // API Error, leave it as unsynced for WorkManager
                Result.success(Unit) 
            }
        } catch (e: Exception) {
            // Network Error, leave it as unsynced
            // Schedule immediate sync attempt for faster recovery
            scheduleImmediateSync()
            Result.success(Unit)
        }
    }

    private fun scheduleImmediateSync() {
        // Enqueue OneTimeWork to sync as soon as network is back
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()
            
        val syncRequest = androidx.work.OneTimeWorkRequest.Builder(
            com.example.genggaminmobile.data.worker.SyncWorker::class.java
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()
            
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            "ImmediateSync",
            androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE,
            syncRequest
        )
    }

    override suspend fun getMyLimits(): Result<List<LoanLimit>> {
        return try {
            val response = loanApi.getMyLimits()
            if (response.success) {
                val limits = response.data?.map { dto ->
                    LoanLimit(
                        id = dto.id,
                        plafondId = dto.plafondId,
                        plafondTitle = dto.plafondTitle,
                        totalLimit = dto.totalLimit,
                        availableLimit = dto.availableLimit,
                        isLocked = dto.isLocked
                    )
                } ?: emptyList()
                
                // Save to DB
                loanLimitDao.clearLimits()
                loanLimitDao.insertLimits(limits.map { it.toEntity() })
                
                Result.success(limits)
            } else {
                // Try from DB
                val local = loanLimitDao.getLimits().firstOrNull()
                if (!local.isNullOrEmpty()) {
                    Result.success(local.map { it.toDomain() })
                } else {
                     Result.failure(Exception(response.message))
                }
            }
        } catch (e: Exception) {
             // Try from DB
            try {
                val local = loanLimitDao.getLimits().firstOrNull()
                if (!local.isNullOrEmpty()) {
                     Result.success(local.map { it.toDomain() })
                } else {
                     Result.failure(e)
                }
            } catch (dbEx: Exception) {
                 Result.failure(e)
            }
        }
    }

    override fun getLimitsFlow(): Flow<List<LoanLimit>> {
        return loanLimitDao.getLimits().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun syncUnsyncedLoans(): Result<Unit> {
        val unsynced = loanDao.getUnsyncedLoans()
        if (unsynced.isEmpty()) return Result.success(Unit)

        var allSuccess = true
        unsynced.forEach { entity ->
            try {
                val response = loanApi.submitLoan(
                    LoanRequest(
                        entity.amount, 
                        entity.tenorMonths, 
                        entity.purpose ?: "", 
                        entity.plafondId,
                        entity.latitude ?: -6.2866713,
                        entity.longitude ?: 106.7791363
                    )
                )
                if (response.success) {
                    loanDao.updateLoan(entity.copy(
                        remoteId = response.data?.id,
                        status = response.data?.status ?: "PENDING",
                        isSynced = true
                    ))
                } else {
                    allSuccess = false
                }
            } catch (e: Exception) {
                allSuccess = false
            }
        }
        return if (allSuccess) Result.success(Unit) else Result.failure(Exception("Failed to sync some loans"))
    }
}
