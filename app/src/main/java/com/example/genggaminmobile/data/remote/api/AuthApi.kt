package com.example.genggaminmobile.data.remote.api

import com.example.genggaminmobile.core.network.ApiResponse
import com.example.genggaminmobile.data.model.dto.*
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    @POST("/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<RegisterResponse>

    @POST("/auth/logout")
    suspend fun logout(): ApiResponse<Unit>

    @POST("/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): ApiResponse<ForgotPasswordResponse>

    @POST("/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): ApiResponse<Unit>
}
