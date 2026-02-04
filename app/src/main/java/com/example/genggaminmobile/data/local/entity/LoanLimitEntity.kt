package com.example.genggaminmobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.genggaminmobile.domain.model.LoanLimit

@Entity(tableName = "loan_limits")
data class LoanLimitEntity(
    @PrimaryKey val id: Long,
    val plafondId: Long,
    val plafondTitle: String,
    val totalLimit: Long,
    val availableLimit: Long,
    val isLocked: Boolean,
)

fun LoanLimitEntity.toDomain() =
    LoanLimit(
        id = id,
        plafondId = plafondId,
        plafondTitle = plafondTitle,
        totalLimit = totalLimit,
        availableLimit = availableLimit,
        isLocked = isLocked,
    )

fun LoanLimit.toEntity() =
    LoanLimitEntity(
        id = id,
        plafondId = plafondId,
        plafondTitle = plafondTitle,
        totalLimit = totalLimit,
        availableLimit = availableLimit,
        isLocked = isLocked,
    )
