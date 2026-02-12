package com.example.genggaminmobile.data.model.dto

import com.google.gson.annotations.SerializedName

data class LoanDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("amount") val amount: Long,
    @SerializedName("tenureMonths") val tenureMonths: Int,
    @SerializedName("purpose") val purpose: String?,
    @SerializedName("plafondId") val plafondId: Long,
    @SerializedName("status") val status: String? = null,
    @SerializedName("interestRate") val interestRate: Double? = null,
    @SerializedName("submittedAt") val submittedAt: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("createdAt") val createdAt: String? = null, // Backend SQL Server created_at
    @SerializedName("updatedAt") val updatedAt: String? = null, // Backend SQL Server updated_at
)

data class LoanLimitDto(
    @SerializedName("id") val id: Long,
    @SerializedName("plafondId") val plafondId: Long,
    @SerializedName("plafondTitle") val plafondTitle: String,
    @SerializedName("totalLimit") val totalLimit: Long,
    @SerializedName("availableLimit") val availableLimit: Long,
    @SerializedName("isLocked") val isLocked: Boolean,
)

data class LoanRequest(
    @SerializedName("amount") val amount: Long,
    @SerializedName("tenureMonths") val tenureMonths: Int,
    @SerializedName("purpose") val purpose: String,
    @SerializedName("plafondId") val plafondId: Long,
    @SerializedName("interestRate") val interestRate: Double,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
)

data class LoanResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: LoanDto?,
)

data class LoanListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<LoanDto>?,
)

data class LoanLimitResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<LoanLimitDto>?,
)
