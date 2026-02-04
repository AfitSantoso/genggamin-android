package com.example.genggaminmobile.data.model.dto

import com.google.gson.annotations.SerializedName

data class EmergencyContactDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("name", alternate = ["contact_name", "contactName"]) val name: String,
    @SerializedName("relationship") val relationship: String,
    @SerializedName("phone", alternate = ["contact_phone", "contactPhone"]) val phone: String,
)

data class CustomerProfileRequest(
    @SerializedName("nik") val nik: String,
    @SerializedName("dateOfBirth") val dateOfBirth: String,
    @SerializedName("placeOfBirth") val placeOfBirth: String,
    @SerializedName("address") val address: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("monthlyIncome") val monthlyIncome: Long,
    @SerializedName("occupation") val occupation: String,
    @SerializedName("currentAddress") val currentAddress: String,
    @SerializedName("motherMaidenName") val motherMaidenName: String,
    @SerializedName("accountNumber") val accountNumber: String,
    @SerializedName("accountHolderName") val accountHolderName: String,
    @SerializedName("emergencyContact") val emergencyContact: EmergencyContactDto,
)

data class CustomerProfileResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("userId") val userId: Int,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("nik") val nik: String,
    @SerializedName("address") val address: String,
    @SerializedName("dateOfBirth") val dateOfBirth: String,
    @SerializedName("placeOfBirth") val placeOfBirth: String,
    @SerializedName("monthlyIncome") val monthlyIncome: Long,
    @SerializedName("occupation") val occupation: String,
    @SerializedName("customerPhone") val customerPhone: String,
    @SerializedName("currentAddress") val currentAddress: String,
    @SerializedName("motherMaidenName") val motherMaidenName: String,
    @SerializedName("accountNumber") val accountNumber: String,
    @SerializedName("accountHolderName") val accountHolderName: String,
    @SerializedName("ktpImagePath", alternate = ["ktp_image_path"]) val ktpImagePath: String?,
    @SerializedName("selfieImagePath", alternate = ["selfie_image_path"]) val selfieImagePath: String?,
    @SerializedName("payslipImagePath", alternate = ["payslip_image_path"]) val payslipImagePath: String?,
    @SerializedName("emergencyContacts") val emergencyContacts: List<EmergencyContactDto>,
    @SerializedName("createdAt") val createdAt: String,
)
