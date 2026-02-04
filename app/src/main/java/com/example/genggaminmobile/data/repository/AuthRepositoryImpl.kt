package com.example.genggaminmobile.data.repository

import com.example.genggaminmobile.core.network.ApiResponse
import com.example.genggaminmobile.data.local.dao.LoanDao
import com.example.genggaminmobile.data.local.dao.LoanLimitDao
import com.example.genggaminmobile.data.local.dao.ProfileDao
import com.example.genggaminmobile.data.local.dao.UserDao
import com.example.genggaminmobile.data.local.datastore.PreferencesManager
import com.example.genggaminmobile.data.local.entity.UserEntity
import com.example.genggaminmobile.data.model.dto.*
import com.example.genggaminmobile.data.remote.api.AuthApi
import com.example.genggaminmobile.domain.model.User
import com.example.genggaminmobile.domain.repository.AuthRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val userDao: UserDao,
    private val loanDao: LoanDao,
    private val profileDao: ProfileDao,
    private val loanLimitDao: LoanLimitDao,
    private val preferencesManager: PreferencesManager,
    private val gson: Gson,
) : AuthRepository {

    private val SESSION_TIMEOUT = 24 * 60 * 60 * 1000L // 24 Hours in milliseconds

    // ... (login/loginGoogle/handleLoginResponse - no changes) ...

    override suspend fun login(username: String, password: String, fcmToken: String?): Result<User> {
        return try {
            val response = authApi.login(LoginRequest(username, password, fcmToken))
            handleLoginResponse(response)
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

    override suspend fun loginGoogle(idToken: String, fcmToken: String?): Result<User> {
        return try {
            val response = authApi.loginGoogle(GoogleLoginRequest(idToken, fcmToken))
            handleLoginResponse(response)
        } catch (e: HttpException) {
            val code = e.code()
            val errorBody = e.response()?.errorBody()?.string()

            val errorMessage = try {
                val apiResponse = gson.fromJson(errorBody, ApiResponse::class.java)
                apiResponse.message
            } catch (ex: Exception) {
                if (code == 401) {
                    "Gagal memverifikasi akun Google. Silakan coba lagi."
                } else {
                    "Terjadi kesalahan server ($code)"
                }
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Gagal terhubung ke server"))
        }
    }

    private suspend fun handleLoginResponse(response: ApiResponse<LoginResponse>): Result<User> {
        val loginData = response.data ?: throw Exception("Data response kosong")

        // Offline First: Save to local database
        userDao.insertUser(
            UserEntity(
                id = loginData.id,
                username = loginData.username,
                email = loginData.email,
                fullName = null,
                isActive = loginData.isActive,
            ),
        )

        saveAuthToken(loginData.token)

        return Result.success(
            User(
                id = loginData.id,
                username = loginData.username,
                email = loginData.email,
                fullName = null,
                isActive = loginData.isActive,
                roles = emptyList(),
                token = loginData.token,
            ),
        )
    }

    override suspend fun register(username: String, email: String, password: String, fullName: String): Result<Unit> {
        return try {
            val request = RegisterRequest(
                username = username,
                password = password,
                email = email,
                fullName = fullName,
                roles = listOf("CUSTOMER"),
            )
            authApi.register(request)
            Result.success(Unit)
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val errorMessage = try {
                val apiResponse = gson.fromJson(errorBody, ApiResponse::class.java)
                apiResponse.message
            } catch (ex: Exception) {
                "Terjadi kesalahan server (${e.code()})"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Gagal terhubung ke server"))
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            // Best practice: Try to notify backend, but always clear local data
            try {
                authApi.logout()
            } catch (e: Exception) {
                // Ignore API failure for logout to ensure user can still logout locally
            }
            // Clear ALL sensitive user data
            userDao.clearUser()
            profileDao.clearProfile()
            loanDao.clearLoans() // Do not clear synced vs unsynced distinction, just clear all for privacy
            // Wait, clearing all loans deletes also unsynced ones?
            // "kalo sudah logout pastikan riwayat dan semua profile tidak bisa di lihat"
            // Usually logout means wiping the session on this device.
            // If there are offline pending loans, they will be LOST if we clearLoans().
            // Ideally we should warn the user, but for now I will strictly follow "clear data".
            // Actually, for a banking app, logout SHOULD clear local sensitive data to prevent others from seeing it.
            // Persisted unsynced loans should be sent before logout or lost.

            loanDao.clearLoans()
            loanLimitDao.clearLimits()

            preferencesManager.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAuthToken(): Flow<String?> {
        return combine(
            preferencesManager.authToken,
            preferencesManager.lastLoginTime,
        ) { token, lastLoginTime ->
            val currentTime = System.currentTimeMillis()
            if (token != null) {
                // Jika lastLoginTime 0 (error simpan/legacy), atau belum expired -> Return Token
                if (lastLoginTime == 0L || (currentTime - lastLoginTime) < SESSION_TIMEOUT) {
                    token
                } else {
                    // Session expired
                    null
                }
            } else {
                null
            }
        }
    }

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
