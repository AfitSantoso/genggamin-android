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
)

data class LoanLimit(
    val id: Long,
    val plafondId: Long,
    val plafondTitle: String,
    val totalLimit: Long,
    val availableLimit: Long,
    val isLocked: Boolean,
)
