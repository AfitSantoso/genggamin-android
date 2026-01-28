package com.example.genggaminmobile.data.model.dto

import com.google.gson.annotations.SerializedName

data class PlafondDto(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("minIncome") val minIncome: Long,
    @SerializedName("maxAmount") val maxAmount: Long,
    @SerializedName("tenorMonth") val tenorMonth: Int,
    @SerializedName("interestRate") val interestRate: Double,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class PlafondResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<PlafondDto>
)
