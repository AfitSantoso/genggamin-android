package com.example.genggaminmobile.data.remote.api

import com.example.genggaminmobile.core.network.ApiResponse
import com.example.genggaminmobile.data.model.dto.CustomerProfileResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface CustomerApi {
    @Multipart
    @POST("customers/profile")
    suspend fun createOrUpdateProfile(
        @Part("data") data: RequestBody,
        @Part ktp: MultipartBody.Part?,
        @Part selfie: MultipartBody.Part?,
        @Part payslip: MultipartBody.Part? = null,
    ): ApiResponse<CustomerProfileResponse>

    @GET("customers/profile")
    suspend fun getProfile(): ApiResponse<CustomerProfileResponse>
}
