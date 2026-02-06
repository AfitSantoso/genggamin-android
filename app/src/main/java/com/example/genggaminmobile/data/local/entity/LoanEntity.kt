package com.example.genggaminmobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.genggaminmobile.domain.model.Loan

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val remoteId: Long? = null,
    val amount: Long,
    val tenorMonths: Int,
    val purpose: String?,
    val plafondId: Long,
    val status: String,
    val interestRate: Double?,
    val date: String?,
    val isSynced: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val submittedAt: Long = System.currentTimeMillis(), // Local timestamp
    val createdAt: Long? = null, // Backend created_at
    val updatedAt: Long? = null, // Backend updated_at
)

fun LoanEntity.toDomain() =
    Loan(
        id = remoteId ?: localId,
        amount = amount,
        tenorMonths = tenorMonths,
        purpose = purpose,
        plafondId = plafondId,
        status = status,
        interestRate = interestRate,
        date = date,
        submittedAt = submittedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Loan.toEntity(isSynced: Boolean = true) =
    LoanEntity(
        remoteId = id,
        amount = amount,
        tenorMonths = tenorMonths,
        purpose = purpose,
        plafondId = plafondId,
        status = status,
        interestRate = interestRate,
        date = date,
        isSynced = isSynced,
        submittedAt = submittedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
