package com.example.genggaminmobile.ui.features.home

import app.cash.turbine.test
import com.example.genggaminmobile.domain.model.Plafond
import com.example.genggaminmobile.domain.repository.AuthRepository
import com.example.genggaminmobile.domain.repository.CustomerRepository
import com.example.genggaminmobile.domain.repository.LoanRepository
import com.example.genggaminmobile.domain.repository.PlafondRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val plafondRepository = mockk<PlafondRepository>()
    private val authRepository = mockk<AuthRepository>()
    private val customerRepository = mockk<CustomerRepository>()
    private val loanRepository = mockk<LoanRepository>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Setup mock default values for mandatory calls in HomeViewModel init
        every { authRepository.getAuthToken() } returns flowOf(null)
        coEvery { plafondRepository.getAllPlafonds() } returns Result.success(emptyList())
        every { loanRepository.getLimitsFlow() } returns flowOf(emptyList())
        every { loanRepository.getMyLoans() } returns flowOf(emptyList())
        coEvery { loanRepository.getMyLimits() } returns Result.success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when user is not logged in, should load all plafonds`() = runTest {
        // Given
        val mockPlafonds = listOf(
            Plafond(
                id = 1,
                title = "Test Plafond",
                minIncome = 1000000L,
                maxAmount = 5000000L,
                tenorMonth = 12,
                interestRate = 0.1,
                isActive = true,
            ),
        )
        coEvery { plafondRepository.getAllPlafonds() } returns Result.success(mockPlafonds)

        // When
        val viewModel = HomeViewModel(
            plafondRepository,
            authRepository,
            customerRepository,
            loanRepository,
        )

        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoggedIn)
            assertEquals(mockPlafonds, state.plafonds)
        }
    }

    @Test
    fun `logout should reset state and check login status`() = runTest {
        // Given
        coEvery { authRepository.logout() } returns Result.success(Unit)

        val viewModel = HomeViewModel(
            plafondRepository,
            authRepository,
            customerRepository,
            loanRepository,
        )

        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { authRepository.logout() }
        assertFalse(viewModel.uiState.value.isLoggedIn)
    }
}
