package com.example.genggaminmobile.data.remote.api

import com.example.genggaminmobile.data.model.dto.LoanLimitResponse
import com.example.genggaminmobile.data.model.dto.LoanListResponse
import com.example.genggaminmobile.data.model.dto.LoanRequest
import com.example.genggaminmobile.data.model.dto.LoanResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface LoanApi {
    @POST("loans/submit")
    suspend fun submitLoan(
        @Body request: LoanRequest,
    ): LoanResponse

    @GET("loans/my-limits")
    suspend fun getMyLimits(): LoanLimitResponse

    @GET("loans/my-loans")
    suspend fun getMyLoans(): LoanListResponse
}
