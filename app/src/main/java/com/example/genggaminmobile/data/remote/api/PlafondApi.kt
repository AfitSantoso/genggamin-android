package com.example.genggaminmobile.data.remote.api

import com.example.genggaminmobile.data.model.dto.PlafondResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface PlafondApi {
    @GET("plafonds")
    suspend fun getAllPlafonds(): PlafondResponse

    @GET("plafonds/by-income/{income}")
    suspend fun getPlafondsByIncome(@Path("income") income: Long): PlafondResponse
}
