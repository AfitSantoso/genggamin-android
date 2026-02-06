package com.example.genggaminmobile.data.local.dao

import androidx.room.*
import com.example.genggaminmobile.data.local.entity.LoanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY localId DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE isSynced = 0")
    suspend fun getUnsyncedLoans(): List<LoanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoans(loans: List<LoanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Query("DELETE FROM loans WHERE remoteId = :remoteId")
    suspend fun deleteByRemoteId(remoteId: Long)

    @Query("DELETE FROM loans")
    suspend fun clearLoans()

    @Query("DELETE FROM loans WHERE isSynced = 1")
    suspend fun deleteSyncedLoans()

    @Query("DELETE FROM loans WHERE localId = :localId")
    suspend fun deleteByLocalId(localId: Long)
}
