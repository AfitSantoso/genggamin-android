package com.example.genggaminmobile.data.local.dao

import androidx.room.*
import com.example.genggaminmobile.data.local.entity.PendingContractEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing pending contract uploads.
 * Supports offline-first contract signing workflow.
 */
@Dao
interface PendingContractDao {

    /**
     * Get all pending contracts ordered by creation date (newest first)
     * Excludes SUBMITTED contracts as they should have been deleted
     */
    @Query("SELECT * FROM pending_contracts WHERE status != 'SUBMITTED' ORDER BY createdAt DESC")
    fun getAllPendingContracts(): Flow<List<PendingContractEntity>>

    /**
     * Get contracts that need to be uploaded (PENDING_UPLOAD status)
     */
    @Query("SELECT * FROM pending_contracts WHERE status = 'PENDING_UPLOAD' ORDER BY createdAt ASC")
    suspend fun getContractsToUpload(): List<PendingContractEntity>

    /**
     * Get contracts that need loan submission (UPLOADED status)
     */
    @Query("SELECT * FROM pending_contracts WHERE status = 'UPLOADED' OR status = 'PENDING_SUBMIT' ORDER BY createdAt ASC")
    suspend fun getContractsToSubmit(): List<PendingContractEntity>

    /**
     * Get all unsynced contracts (not yet submitted)
     */
    @Query("SELECT * FROM pending_contracts WHERE status NOT IN ('SUBMITTED', 'FAILED') ORDER BY createdAt ASC")
    suspend fun getUnsyncedContracts(): List<PendingContractEntity>

    /**
     * Get a specific contract by ID
     */
    @Query("SELECT * FROM pending_contracts WHERE id = :id")
    suspend fun getContractById(id: Long): PendingContractEntity?

    /**
     * Insert a new pending contract
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContract(contract: PendingContractEntity): Long

    /**
     * Update a pending contract
     */
    @Update
    suspend fun updateContract(contract: PendingContractEntity)

    /**
     * Update contract status
     */
    @Query("UPDATE pending_contracts SET status = :status, lastAttemptAt = :timestamp WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Update contract with upload result
     */
    @Query("UPDATE pending_contracts SET status = :status, contractUrl = :url, lastAttemptAt = :timestamp WHERE id = :id")
    suspend fun updateWithUploadResult(id: Long, status: String, url: String?, timestamp: Long = System.currentTimeMillis())

    /**
     * Update contract with error
     */
    @Query("UPDATE pending_contracts SET status = :status, errorMessage = :error, uploadAttempts = uploadAttempts + 1, lastAttemptAt = :timestamp WHERE id = :id")
    suspend fun updateWithError(id: Long, status: String, error: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Delete a pending contract
     */
    @Query("DELETE FROM pending_contracts WHERE id = :id")
    suspend fun deleteContract(id: Long)

    /**
     * Delete all submitted contracts (cleanup)
     */
    @Query("DELETE FROM pending_contracts WHERE status = 'SUBMITTED'")
    suspend fun deleteSubmittedContracts()

    /**
     * Count pending contracts
     */
    @Query("SELECT COUNT(*) FROM pending_contracts WHERE status NOT IN ('SUBMITTED', 'FAILED')")
    suspend fun countPendingContracts(): Int

    /**
     * Count pending contracts as Flow for reactive UI
     */
    @Query("SELECT COUNT(*) FROM pending_contracts WHERE status NOT IN ('SUBMITTED', 'FAILED')")
    fun countPendingContractsFlow(): Flow<Int>
}
