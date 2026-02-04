package com.example.genggaminmobile.domain.model

data class LoanSimulation(
    val monthlyInstallment: Long = 0,
    val totalInterest: Long = 0,
    val totalRepayment: Long = 0,
    val interestRate: Double = 0.0,
)
