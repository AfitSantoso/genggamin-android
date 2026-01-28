package com.example.genggaminmobile.data.repository

import com.example.genggaminmobile.data.model.dto.LoanRequest
import com.example.genggaminmobile.data.remote.api.LoanApi
import com.example.genggaminmobile.domain.model.Loan
import com.example.genggaminmobile.domain.model.LoanLimit
import com.example.genggaminmobile.domain.repository.LoanRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRepositoryImpl @Inject constructor(
    private val loanApi: LoanApi
) : LoanRepository {
    override suspend fun submitLoan(
        amount: Long,
        tenor: Int,
        purpose: String,
        plafondId: Long
    ): Result<Loan> {
        return try {
            val response = loanApi.submitLoan(
                LoanRequest(amount, tenor, purpose, plafondId)
            )
            if (response.success && response.data != null) {
                val dto = response.data
                Result.success(
                    Loan(
                        id = dto.id,
                        amount = dto.amount,
                        tenorMonths = dto.tenureMonths,
                        purpose = dto.purpose,
                        plafondId = dto.plafondId,
                        status = dto.status ?: "UNKNOWN",
                        interestRate = dto.interestRate,
                        date = dto.submittedAt
                    )
                )
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMyLimits(): Result<List<LoanLimit>> {
        return try {
            val response = loanApi.getMyLimits()
            if (response.success && response.data != null) {
                Result.success(response.data.map { dto ->
                    LoanLimit(
                        id = dto.id,
                        plafondId = dto.plafondId,
                        plafondTitle = dto.plafondTitle,
                        totalLimit = dto.totalLimit,
                        availableLimit = dto.availableLimit,
                        isLocked = dto.isLocked
                    )
                })
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMyLoans(): Result<List<Loan>> {
        return try {
            val response = loanApi.getMyLoans()
            if (response.success && response.data != null) {
                Result.success(response.data.map { dto ->
                    Loan(
                        id = dto.id,
                        amount = dto.amount,
                        tenorMonths = dto.tenureMonths,
                        purpose = dto.purpose,
                        plafondId = dto.plafondId,
                        status = dto.status ?: "UNKNOWN",
                        interestRate = dto.interestRate,
                        date = dto.submittedAt
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
