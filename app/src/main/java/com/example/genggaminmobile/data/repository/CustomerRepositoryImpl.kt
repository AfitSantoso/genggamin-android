package com.example.genggaminmobile.data.repository

import com.example.genggaminmobile.data.model.dto.CustomerProfileResponse
import com.example.genggaminmobile.data.remote.api.CustomerApi
import com.example.genggaminmobile.domain.repository.CustomerRepository
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepositoryImpl @Inject constructor(
    private val customerApi: CustomerApi
) : CustomerRepository {

    override suspend fun getProfile(): Result<CustomerProfileResponse> {
        return try {
            val response = customerApi.getProfile()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: HttpException) {
            Result.failure(Exception("Gagal mengambil profil: ${e.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
