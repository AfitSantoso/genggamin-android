package com.example.genggaminmobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.genggaminmobile.domain.model.Plafond

@Entity(tableName = "plafonds")
data class PlafondEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val minIncome: Long,
    val maxAmount: Long,
    val tenorMonth: Int,
    val interestRate: Double,
    val isActive: Boolean
)

fun PlafondEntity.toDomain() = Plafond(
    id = id,
    title = title,
    minIncome = minIncome,
    maxAmount = maxAmount,
    tenorMonth = tenorMonth,
    interestRate = interestRate,
    isActive = isActive
)

fun Plafond.toEntity() = PlafondEntity(
    id = id,
    title = title,
    minIncome = minIncome,
    maxAmount = maxAmount,
    tenorMonth = tenorMonth,
    interestRate = interestRate,
    isActive = isActive
)
