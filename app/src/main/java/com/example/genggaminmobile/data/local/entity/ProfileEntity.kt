package com.example.genggaminmobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val username: String,
    val email: String,
    val fullName: String,
    val nik: String,
    val address: String,
    val dateOfBirth: String,
    val placeOfBirth: String,
    val monthlyIncome: Long,
    val occupation: String,
    val customerPhone: String,
    val currentAddress: String,
    val motherMaidenName: String,
    val accountNumber: String,
    val accountHolderName: String,
    val ktpImagePath: String?,
    val selfieImagePath: String?,
    val payslipImagePath: String?,
    val localKtpPath: String? = null,
    val localSelfiePath: String? = null,
    val localPayslipPath: String? = null,
    val emergencyContactsJson: String, // Stored as JSON string
    val createdAt: String
)
