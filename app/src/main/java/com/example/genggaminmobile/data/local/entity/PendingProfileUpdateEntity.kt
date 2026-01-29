package com.example.genggaminmobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_profile_update")
data class PendingProfileUpdateEntity(
    @PrimaryKey val id: Int = 1, // Only one pending update at a time
    val jsonRequest: String,
    val ktpPath: String?,
    val selfiePath: String?,
    val payslipPath: String?
)
