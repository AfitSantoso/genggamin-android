package com.example.genggaminmobile.data.repository

import com.example.genggaminmobile.data.local.dao.UserDao
import com.example.genggaminmobile.data.local.datastore.PreferencesManager
import com.example.genggaminmobile.data.local.entity.UserEntity
import com.example.genggaminmobile.data.model.dto.*
import com.example.genggaminmobile.data.remote.api.AuthApi
import com.example.genggaminmobile.domain.model.User
import com.example.genggaminmobile.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val userDao: UserDao,
    private val preferencesManager: PreferencesManager
) : AuthRepository {

    override suspend fun login(username: String, password: String, fcmToken: String?): Result<User> {
        return try {
            val response = authApi.login(LoginRequest(username, password, fcmToken))
            val loginData = response.data ?: throw Exception("Data response kosong")
            
            // Offline First: Save to local database
            userDao.insertUser(
                UserEntity(
                    id = loginData.id,
                    username = loginData.username,
                    email = loginData.email,
                    fullName = null,
                    isActive = loginData.isActive
                )
            )
            
            saveAuthToken(loginData.token)
            
            Result.success(User(
                id = loginData.id,
                username = loginData.username,
                email = loginData.email,
                fullName = null,
                isActive = loginData.isActive,
                roles = emptyList(),
                token = loginData.token
            ))
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                404 -> "Pengguna tidak ditemukan"
                401 -> "Pengguna atau kata sandi salah"
                else -> "Terjadi kesalahan server (${e.code()})"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Gagal terhubung ke server. Periksa koneksi internet Anda."))
        }
    }

    override suspend fun register(username: String, email: String, password: String, fullName: String): Result<Unit> {
        return try {
            authApi.register(RegisterRequest(username, password, email, fullName))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            authApi.logout()
            userDao.clearUser()
            preferencesManager.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAuthToken(): Flow<String?> = preferencesManager.authToken

    override suspend fun saveAuthToken(token: String) {
        preferencesManager.saveAuthToken(token)
    }

    override suspend fun forgotPassword(email: String): Result<Pair<String, String?>> {
        return try {
            val response = authApi.forgotPassword(ForgotPasswordRequest(email))
            Result.success(Pair(response.message, response.data?.token))
        } catch (e: HttpException) {
            val message = e.response()?.errorBody()?.string() ?: e.message()
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(token: String, newPassword: String): Result<String> {
        return try {
            val response = authApi.resetPassword(ResetPasswordRequest(token, newPassword))
            Result.success(response.message)
        } catch (e: HttpException) {
            val message = e.response()?.errorBody()?.string() ?: e.message()
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
