package com.example.genggaminmobile.domain.repository

import com.example.genggaminmobile.domain.model.Plafond

interface PlafondRepository {
    suspend fun getAllPlafonds(): Result<List<Plafond>>

    suspend fun getPlafondsByIncome(income: Long): Result<List<Plafond>>

    suspend fun refreshPlafonds(): Result<Unit>
}
