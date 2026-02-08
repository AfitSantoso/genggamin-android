package com.example.genggaminmobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing pending contract uploads.
 * This enables offline-first contract signing - contracts are saved locally
 * and uploaded when internet is available.
 */
@Entity(tableName = "pending_contracts")
data class PendingContractEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // Customer info for contract
    val customerName: String,
    val customerNik: String,
    val customerAddress: String,

    // Loan info
    val amount: Long,
    val tenor: Int,
    val interestRate: Double,
    val monthlyInstallment: Long,
    val totalRepayment: Long,
    val purpose: String,
    val plafondId: Long,
    val latitude: Double,
    val longitude: Double,

    // Contract file path (stored locally)
    val pdfFilePath: String,

    // Upload status
    val status: String = ContractStatus.PENDING_UPLOAD,
    val contractUrl: String? = null, // Cloudinary URL after upload
    val errorMessage: String? = null,

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val uploadAttempts: Int = 0,
    val lastAttemptAt: Long? = null,
)

/**
 * Status constants for pending contracts
 */
object ContractStatus {
    const val PENDING_UPLOAD = "PENDING_UPLOAD" // Waiting to upload PDF
    const val UPLOADING = "UPLOADING" // Currently uploading
    const val UPLOADED = "UPLOADED" // PDF uploaded, waiting loan submit
    const val PENDING_SUBMIT = "PENDING_SUBMIT" // Waiting to submit loan
    const val SUBMITTED = "SUBMITTED" // Loan submitted successfully
    const val FAILED = "FAILED" // Failed after max retries
}
