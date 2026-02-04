package com.example.genggaminmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.genggaminmobile.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile LIMIT 1")
    fun getProfile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile LIMIT 1")
    suspend fun getProfileOneShot(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Query("DELETE FROM profile")
    suspend fun clearProfile()

    // Pending Update Methods
    @Query("SELECT * FROM pending_profile_update LIMIT 1")
    suspend fun getPendingUpdate(): com.example.genggaminmobile.data.local.entity.PendingProfileUpdateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingUpdate(update: com.example.genggaminmobile.data.local.entity.PendingProfileUpdateEntity)

    @Query("DELETE FROM pending_profile_update")
    suspend fun clearPendingUpdate()
}
