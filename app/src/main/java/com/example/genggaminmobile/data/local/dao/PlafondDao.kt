package com.example.genggaminmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.genggaminmobile.data.local.entity.PlafondEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlafondDao {
    @Query("SELECT * FROM plafonds")
    fun getAllPlafonds(): Flow<List<PlafondEntity>>

    @Query("SELECT * FROM plafonds WHERE minIncome <= :income")
    fun getPlafondsByIncome(income: Long): Flow<List<PlafondEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlafonds(plafonds: List<PlafondEntity>)

    @Query("DELETE FROM plafonds")
    suspend fun clearPlafonds()
}
