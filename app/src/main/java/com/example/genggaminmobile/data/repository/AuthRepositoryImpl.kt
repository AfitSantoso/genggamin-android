package com.example.genggaminmobile.data.repository

import com.example.genggaminmobile.data.local.dao.UserDao
import com.example.genggaminmobile.data.local.datastore.PreferencesManager
import com.example.genggaminmobile.data.local.entity.UserEntity
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

    override suspend fun login(username: String, password: String): Result<User> {
        return try {
            val response = authApi.login(mapOf("username" to username, "password" to password))
            
            // Offline First: Save to local database
            userDao.insertUser(
                UserEntity(
                    id = response.id,
                    username = response.username,
                    email = response.email,
                    fullName = null,
                    isActive = response.isActive
                )
            )
            
            saveAuthToken(response.token)
            
            Result.success(User(
                id = response.id,
                username = response.username,
                email = response.email,
                fullName = null,
                isActive = response.isActive,
                roles = emptyList(),
                token = response.token
            ))
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                404 -> "Pengguna tidak ditemukan"
                401 -> "Pengguna atau kata sandi salah"
                else -> "Terjadi kesalahan server (${e.code()})"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception("Gagal terhubung ke server. Periksa koneksi internet Anda."))
        }
    }

    override suspend fun register(username: String, email: String, password: String, fullName: String): Result<Unit> {
        return try {
            authApi.register(
                mapOf(
                    "username" to username,
                    "email" to email,
                    "password" to password,
                    "fullName" to fullName
                )
            )
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
}
