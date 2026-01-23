package com.example.genggaminmobile.data.remote.api

import com.example.genggaminmobile.core.network.ApiResponse
import com.example.genggaminmobile.data.model.dto.LoginResponse
import com.example.genggaminmobile.data.model.dto.RegisterResponse
import com.example.genggaminmobile.data.model.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/auth/login")
    suspend fun login(@Body request: Map<String, String>): LoginResponse

    @POST("/auth/register")
    suspend fun register(@Body request: Map<String, Any>): RegisterResponse

    @POST("/auth/logout")
    suspend fun logout(): ApiResponse<Unit>
}
