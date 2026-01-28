package com.example.genggaminmobile.core.di

import com.example.genggaminmobile.data.repository.AuthRepositoryImpl
import com.example.genggaminmobile.data.repository.CustomerRepositoryImpl
import com.example.genggaminmobile.data.repository.PlafondRepositoryImpl
import com.example.genggaminmobile.domain.repository.AuthRepository
import com.example.genggaminmobile.domain.repository.CustomerRepository
import com.example.genggaminmobile.data.repository.LoanRepositoryImpl
import com.example.genggaminmobile.data.repository.NotificationRepositoryImpl
import com.example.genggaminmobile.domain.repository.LoanRepository
import com.example.genggaminmobile.domain.repository.NotificationRepository
import com.example.genggaminmobile.domain.repository.PlafondRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindPlafondRepository(
        plafondRepositoryImpl: PlafondRepositoryImpl
    ): PlafondRepository

    @Binds
    @Singleton
    abstract fun bindCustomerRepository(
        customerRepositoryImpl: CustomerRepositoryImpl
    ): CustomerRepository

    @Binds
    @Singleton
    abstract fun bindLoanRepository(
        loanRepositoryImpl: LoanRepositoryImpl
    ): LoanRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: NotificationRepositoryImpl
    ): NotificationRepository
}
