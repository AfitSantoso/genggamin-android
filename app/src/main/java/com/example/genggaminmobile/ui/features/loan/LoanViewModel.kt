package com.example.genggaminmobile.ui.features.loan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genggaminmobile.domain.model.LoanLimit
import com.example.genggaminmobile.domain.model.Plafond
import com.example.genggaminmobile.domain.repository.LoanRepository
import com.example.genggaminmobile.domain.repository.PlafondRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.pow

data class LoanSimulation(
    val monthlyInstallment: Long = 0,
    val totalInterest: Long = 0,
    val totalRepayment: Long = 0,
    val interestRate: Double = 0.0
)

data class LoanApplicationUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val plafonds: List<Plafond> = emptyList(),
    val limits: List<LoanLimit> = emptyList(),
    val selectedPlafond: Plafond? = null,
    val selectedLimit: LoanLimit? = null,
    val amountInput: String = "",
    val tenorInput: String = "",
    val purposeInput: String = "",
    val simulation: LoanSimulation? = null
)

@HiltViewModel
class LoanViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val plafondRepository: PlafondRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoanApplicationUiState())
    val uiState: StateFlow<LoanApplicationUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val plafondsResult = plafondRepository.getAllPlafonds()
            val limitsResult = loanRepository.getMyLimits()

            if (plafondsResult.isSuccess && limitsResult.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    plafonds = plafondsResult.getOrNull()?.filter { it.isActive } ?: emptyList(),
                    limits = limitsResult.getOrNull() ?: emptyList(),
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "Gagal memuat data pinjaman",
                    isLoading = false
                )
            }
        }
    }

    fun onPlafondSelected(plafond: Plafond) {
        val limit = _uiState.value.limits.find { it.plafondId == plafond.id.toLong() }
        _uiState.value = _uiState.value.copy(
            selectedPlafond = plafond,
            selectedLimit = limit,
            amountInput = "",
            tenorInput = plafond.tenorMonth.toString(),
            simulation = null
        )
    }

    fun onAmountChanged(amount: String) {
        val filteredAmount = amount.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(amountInput = filteredAmount)
        calculateSimulation()
    }

    fun onTenorChanged(tenor: String) {
        val filteredTenor = tenor.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(tenorInput = filteredTenor)
        calculateSimulation()
    }

    private fun calculateSimulation() {
        val state = _uiState.value
        val plafond = state.selectedPlafond ?: return
        val amount = state.amountInput.toLongOrNull() ?: 0L
        val tenor = state.tenorInput.toIntOrNull() ?: 0

        if (amount > 0 && tenor > 0) {
            val annualInterestRate = plafond.interestRate / 100.0
            val monthlyInterestRate = annualInterestRate / 12.0
            
            // Fixed Installment Formula: P * r * (1+r)^n / ((1+r)^n - 1)
            val monthlyInstallment = if (monthlyInterestRate > 0) {
                val factor = (1.0 + monthlyInterestRate).pow(tenor.toDouble())
                (amount * monthlyInterestRate * factor / (factor - 1.0)).toLong()
            } else {
                amount / tenor
            }

            val totalRepayment = monthlyInstallment * tenor
            val totalInterest = totalRepayment - amount

            _uiState.value = _uiState.value.copy(
                simulation = LoanSimulation(
                    monthlyInstallment = monthlyInstallment,
                    totalInterest = totalInterest,
                    totalRepayment = totalRepayment,
                    interestRate = plafond.interestRate
                )
            )
        } else {
            _uiState.value = _uiState.value.copy(simulation = null)
        }
    }

    fun onPurposeChanged(purpose: String) {
        _uiState.value = _uiState.value.copy(purposeInput = purpose)
    }

    fun submitLoan() {
        val state = _uiState.value
        val amount = state.amountInput.toLongOrNull() ?: 0L
        val tenor = state.tenorInput.toIntOrNull() ?: 0
        
        if (state.selectedPlafond == null) {
            _uiState.value = state.copy(error = "Pilih plafond terlebih dahulu")
            return
        }

        if (amount <= 0 || amount > state.selectedPlafond.maxAmount) {
            _uiState.value = state.copy(error = "Jumlah pinjaman tidak valid")
            return
        }

        val limit = state.selectedLimit
        if (limit != null && amount > limit.availableLimit) {
            _uiState.value = state.copy(error = "Jumlah pinjaman melebihi sisa limit Anda")
            return
        }

        if (tenor <= 0 || tenor > state.selectedPlafond.tenorMonth) {
            _uiState.value = state.copy(error = "Tenor tidak valid")
            return
        }

        if (state.purposeInput.isBlank()) {
            _uiState.value = state.copy(error = "Mohon isi tujuan pinjaman")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            loanRepository.submitLoan(
                amount = amount,
                tenor = tenor,
                purpose = state.purposeInput,
                plafondId = state.selectedPlafond.id.toLong()
            ).fold(
                onSuccess = {
                    _uiState.value = state.copy(isLoading = false, success = true)
                },
                onFailure = { e ->
                    _uiState.value = state.copy(isLoading = false, error = e.message ?: "Gagal mengajukan pinjaman")
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = LoanApplicationUiState(
            plafonds = _uiState.value.plafonds,
            limits = _uiState.value.limits
        )
    }
}
