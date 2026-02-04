package com.example.genggaminmobile.domain.util

import com.example.genggaminmobile.domain.model.Plafond
import org.junit.Assert.*
import org.junit.Test

class LoanCalculatorTest {

    private val calculator = LoanCalculator()

    @Test
    fun `calculateSimulation returns correct values for valid input`() {
        // Arrange
        val amount = 10_000_000L
        val tenor = 12
        val plafond = Plafond(
            id = 1,
            title = "Plafond Reguler",
            minIncome = 5_000_000,
            maxAmount = 20_000_000,
            tenorMonth = 12,
            interestRate = 1.0,
            isActive = true,
        )

        // Act
        val result = calculator.calculateSimulation(amount, tenor, plafond)

        // Assert
        assertNotNull("Simulation result should not be null", result)
        result?.let {
            // Total Bunga = 10,000,000 * (1% * 12) = 1,200_000
            assertEquals(1_200_000L, it.totalInterest)

            // Total Bayar = 10,000,000 + 1,200,000 = 11,200,000
            assertEquals(11_200_000L, it.totalRepayment)

            // Angsuran per bulan = 11,200,000 / 12 = 933,333
            assertEquals(933_333L, it.monthlyInstallment)

            assertEquals(1.0, it.interestRate, 0.0)
        }
    }

    @Test
    fun `calculateSimulation returns null for zero or negative amount`() {
        val amount = 0L
        val tenor = 12
        val plafond = Plafond(
            id = 1,
            title = "Test",
            minIncome = 5000000,
            maxAmount = 20000000,
            tenorMonth = 12,
            interestRate = 1.0,
            isActive = true,
        )

        val result = calculator.calculateSimulation(amount, tenor, plafond)
        assertNull("Result should be null for zero amount", result)
    }
}
