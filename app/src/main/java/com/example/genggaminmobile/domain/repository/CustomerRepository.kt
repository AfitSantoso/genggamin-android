package com.example.genggaminmobile.domain.repository

import com.example.genggaminmobile.data.model.dto.CustomerProfileRequest
import com.example.genggaminmobile.data.model.dto.CustomerProfileResponse
import java.io.File

interface CustomerRepository {
    suspend fun getProfile(): Result<CustomerProfileResponse>
    suspend fun createOrUpdateProfile(
        data: CustomerProfileRequest,
        ktp: File?,
        selfie: File?,
        payslip: File?
    ): Result<CustomerProfileResponse>
}
