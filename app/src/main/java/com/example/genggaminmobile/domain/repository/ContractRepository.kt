package com.example.genggaminmobile.domain.repository

import com.example.genggaminmobile.data.local.entity.PendingContractEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing contract operations.
 * Supports offline-first workflow for contract signing.
 */
interface ContractRepository {

    /**
     * Save a pending contract locally.
     * This is the first step in offline-first contract submission.
     */
    suspend fun savePendingContract(contract: PendingContractEntity): Long

    /**
     * Get all pending contracts as a Flow for reactive UI
     */
    fun getPendingContracts(): Flow<List<PendingContractEntity>>

    /**
     * Get count of pending contracts
     */
    fun getPendingContractsCount(): Flow<Int>

    /**
     * Sync all pending contracts:
     * 1. Upload PDFs to Cloudinary
     * 2. Submit loans to backend
     */
    suspend fun syncPendingContracts(): Result<Int>

    /**
     * Upload a single contract's PDF to Cloudinary
     */
    suspend fun uploadContractPdf(contractId: Long): Result<String>

    /**
     * Submit a loan for a contract that has been uploaded
     */
    suspend fun submitContractLoan(contractId: Long): Result<Unit>

    /**
     * Delete a pending contract
     */
    suspend fun deleteContract(contractId: Long)

    /**
     * Retry failed contract
     */
    suspend fun retryContract(contractId: Long): Result<Unit>
}
