package com.example.genggaminmobile.core.di

import android.content.Context
import androidx.room.Room
import com.example.genggaminmobile.data.local.dao.LoanDao
import com.example.genggaminmobile.data.local.dao.LoanLimitDao
import com.example.genggaminmobile.data.local.dao.PlafondDao
import com.example.genggaminmobile.data.local.dao.ProfileDao
import com.example.genggaminmobile.data.local.dao.UserDao
import com.example.genggaminmobile.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "genggamin_db"
        )
        .fallbackToDestructiveMigration() // For development purposes
        .build()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun providePlafondDao(database: AppDatabase): PlafondDao {
        return database.plafondDao()
    }

    @Provides
    fun provideLoanDao(database: AppDatabase): LoanDao {
        return database.loanDao()
    }

    @Provides
    fun provideProfileDao(database: AppDatabase): ProfileDao {
        return database.profileDao()
    }

    @Provides
    fun provideLoanLimitDao(database: AppDatabase): LoanLimitDao {
        return database.loanLimitDao()
    }
}
