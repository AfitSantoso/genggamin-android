package com.example.genggaminmobile.data.repository

import com.example.genggaminmobile.data.local.dao.PlafondDao
import com.example.genggaminmobile.data.local.entity.toDomain
import com.example.genggaminmobile.data.local.entity.toEntity
import com.example.genggaminmobile.data.remote.api.PlafondApi
import com.example.genggaminmobile.domain.model.Plafond
import com.example.genggaminmobile.domain.repository.PlafondRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlafondRepositoryImpl @Inject constructor(
    private val plafondApi: PlafondApi,
    private val plafondDao: PlafondDao
) : PlafondRepository {

    override suspend fun getAllPlafonds(): Result<List<Plafond>> {
        return try {
            val plafonds = plafondDao.getAllPlafonds().first().map { it.toDomain() }
            if (plafonds.isEmpty()) {
                refreshPlafonds()
                val refreshedPlafonds = plafondDao.getAllPlafonds().first().map { it.toDomain() }
                Result.success(refreshedPlafonds)
            } else {
                Result.success(plafonds)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshPlafonds(): Result<Unit> {
        return try {
            val response = plafondApi.getAllPlafonds()
            if (response.success) {
                val domains = response.data.map { dto ->
                    Plafond(
                        id = dto.id,
                        title = dto.title,
                        minIncome = dto.minIncome,
                        maxAmount = dto.maxAmount,
                        tenorMonth = dto.tenorMonth,
                        interestRate = dto.interestRate,
                        isActive = dto.isActive
                    )
                }
                plafondDao.clearPlafonds()
                plafondDao.insertPlafonds(domains.map { it.toEntity() })
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPlafondsByIncome(income: Long): Result<List<Plafond>> {
        return try {
            val plafonds = plafondDao.getPlafondsByIncome(income).first().map { it.toDomain() }
            if (plafonds.isEmpty()) {
                refreshPlafonds()
                val refreshedPlafonds = plafondDao.getPlafondsByIncome(income).first().map { it.toDomain() }
                Result.success(refreshedPlafonds)
            } else {
                Result.success(plafonds)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
