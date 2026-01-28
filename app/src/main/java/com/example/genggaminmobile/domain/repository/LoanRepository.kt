package com.example.genggaminmobile.domain.repository

import com.example.genggaminmobile.domain.model.Loan
import com.example.genggaminmobile.domain.model.LoanLimit

interface LoanRepository {
    suspend fun submitLoan(amount: Long, tenor: Int, purpose: String, plafondId: Long): Result<Loan>
    suspend fun getMyLimits(): Result<List<LoanLimit>>
    suspend fun getMyLoans(): Result<List<Loan>>
}
