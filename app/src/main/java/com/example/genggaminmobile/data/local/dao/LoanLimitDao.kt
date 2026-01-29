package com.example.genggaminmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.genggaminmobile.data.local.entity.LoanLimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanLimitDao {
    @Query("SELECT * FROM loan_limits")
    fun getLimits(): Flow<List<LoanLimitEntity>>

    @Query("SELECT * FROM loan_limits WHERE plafondId = :plafondId")
    suspend fun getLimitByPlafondId(plafondId: Long): LoanLimitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLimits(limits: List<LoanLimitEntity>)
    
    @androidx.room.Update
    suspend fun updateLimit(limit: LoanLimitEntity)
    
    @Query("DELETE FROM loan_limits")
    suspend fun clearLimits()
}
