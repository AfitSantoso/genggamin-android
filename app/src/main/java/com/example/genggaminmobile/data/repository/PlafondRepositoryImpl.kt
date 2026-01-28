package com.example.genggaminmobile.data.repository

import com.example.genggaminmobile.data.remote.api.PlafondApi
import com.example.genggaminmobile.domain.model.Plafond
import com.example.genggaminmobile.domain.repository.PlafondRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlafondRepositoryImpl @Inject constructor(
    private val plafondApi: PlafondApi
) : PlafondRepository {

    override suspend fun getAllPlafonds(): Result<List<Plafond>> {
        return try {
            val response = plafondApi.getAllPlafonds()
            if (response.success) {
                Result.success(response.data.map { dto ->
                    Plafond(
                        id = dto.id,
                        title = dto.title,
                        minIncome = dto.minIncome,
                        maxAmount = dto.maxAmount,
                        tenorMonth = dto.tenorMonth,
                        interestRate = dto.interestRate,
                        isActive = dto.isActive
                    )
                })
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPlafondsByIncome(income: Long): Result<List<Plafond>> {
        return try {
            val response = plafondApi.getPlafondsByIncome(income)
            if (response.success) {
                Result.success(response.data.map { dto ->
                    Plafond(
                        id = dto.id,
                        title = dto.title,
                        minIncome = dto.minIncome,
                        maxAmount = dto.maxAmount,
                        tenorMonth = dto.tenorMonth,
                        interestRate = dto.interestRate,
                        isActive = dto.isActive
                    )
                })
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
