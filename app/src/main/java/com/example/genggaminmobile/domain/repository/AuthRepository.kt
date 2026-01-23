package com.example.genggaminmobile.domain.repository

import com.example.genggaminmobile.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<User>
    suspend fun register(username: String, email: String, password: String, fullName: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    fun getAuthToken(): Flow<String?>
    suspend fun saveAuthToken(token: String)
}
