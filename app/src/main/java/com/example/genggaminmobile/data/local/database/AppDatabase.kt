package com.example.genggaminmobile.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.genggaminmobile.data.local.dao.LoanDao
import com.example.genggaminmobile.data.local.dao.LoanLimitDao
import com.example.genggaminmobile.data.local.dao.PlafondDao
import com.example.genggaminmobile.data.local.dao.ProfileDao
import com.example.genggaminmobile.data.local.dao.UserDao
import com.example.genggaminmobile.data.local.entity.LoanEntity
import com.example.genggaminmobile.data.local.entity.LoanLimitEntity
import com.example.genggaminmobile.data.local.entity.PlafondEntity
import com.example.genggaminmobile.data.local.entity.ProfileEntity
import com.example.genggaminmobile.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, PlafondEntity::class, LoanEntity::class, ProfileEntity::class, LoanLimitEntity::class, com.example.genggaminmobile.data.local.entity.PendingProfileUpdateEntity::class],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun plafondDao(): PlafondDao
    abstract fun loanDao(): LoanDao
    abstract fun profileDao(): ProfileDao
    abstract fun loanLimitDao(): LoanLimitDao
}
