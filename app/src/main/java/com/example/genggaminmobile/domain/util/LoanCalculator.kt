package com.example.genggaminmobile.domain.util

import com.example.genggaminmobile.domain.model.LoanSimulation
import com.example.genggaminmobile.domain.model.Plafond

class LoanCalculator {

    fun calculateSimulation(
        amount: Long,
        tenor: Int,
        plafond: Plafond,
    ): LoanSimulation? {
        if (amount <= 0 || tenor <= 0) {
            return null
        }

        val monthlyInterestRatePercent = plafond.interestRate
        val totalInterestPercent = monthlyInterestRatePercent * tenor

        val totalInterest = (amount * (totalInterestPercent / 100.0)).toLong()
        val totalRepayment = amount + totalInterest
        val monthlyInstallment = totalRepayment / tenor

        return LoanSimulation(
            monthlyInstallment = monthlyInstallment,
            totalInterest = totalInterest,
            totalRepayment = totalRepayment,
            interestRate = plafond.interestRate,
        )
    }
}
