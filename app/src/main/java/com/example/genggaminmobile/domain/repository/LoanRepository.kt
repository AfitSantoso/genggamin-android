package com.example.genggaminmobile.domain.repository

import com.example.genggaminmobile.domain.model.Loan
import com.example.genggaminmobile.domain.model.LoanLimit
import kotlinx.coroutines.flow.Flow

interface LoanRepository {
    suspend fun submitLoan(
        amount: Long,
        tenor: Int,
        purpose: String,
        plafondId: Long,
        interestRate: Double,
        latitude: Double,
        longitude: Double,
    ): Result<Unit>

    fun getMyLoans(): Flow<List<Loan>>

    suspend fun refreshLoans(): Result<Unit>

    suspend fun getMyLimits(): Result<List<LoanLimit>>

    fun getLimitsFlow(): Flow<List<LoanLimit>>

    suspend fun syncUnsyncedLoans(): Result<Unit>
    
    suspend fun getLoanById(loanId: Long): Loan?
    
    fun getLoanFlow(loanId: Long): kotlinx.coroutines.flow.Flow<Loan?>  // Reactive Flow

    /**
     * Cancel/delete an offline loan application by its local ID.
     * Only works for loans with status "PENDING (Offline)" that haven't been synced.
     */
    suspend fun cancelOfflineLoan(localId: Long): Result<Unit>
}
