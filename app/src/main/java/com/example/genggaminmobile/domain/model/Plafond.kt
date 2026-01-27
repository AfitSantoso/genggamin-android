package com.example.genggaminmobile.domain.model

data class Plafond(
    val id: Int,
    val title: String,
    val minIncome: Long,
    val maxAmount: Long,
    val tenorMonth: Int,
    val interestRate: Double,
    val isActive: Boolean
)
