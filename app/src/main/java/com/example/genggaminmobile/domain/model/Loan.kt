package com.example.genggaminmobile.domain.model

data class Loan(
    val id: Long?,
    val amount: Long,
    val tenorMonths: Int,
    val purpose: String?,
    val plafondId: Long,
    val status: String,
    val interestRate: Double?,
    val date: String?,
    val submittedAt: Long = System.currentTimeMillis(), // Local timestamp
    val createdAt: Long? = null, // Backend created_at timestamp
    val updatedAt: Long? = null, // Backend updated_at timestamp
)

data class LoanLimit(
    val id: Long,
    val plafondId: Long,
    val plafondTitle: String,
    val totalLimit: Long,
    val availableLimit: Long,
    val isLocked: Boolean,
)
