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
            // Simple Flat Rate Calculation as requested by User
            // Logic: Interest = Principal * (Rate%) * Tenor
            // Note: User specified 4% flat charged per month -> rate treated as monthly rate?
            // "bung 4% itu di kenakan 6x karena 6 bulan tenor" -> 4% * 6
            // The rate in Plafond object usually is Annual or Monthly? 
            // In absence of confirmation, I will treat plafond.interestRate as the rate to be applied monthly.
            
            // However, typically rates are Annual. If 4% is Annual, then monthly is 4/12 %.
            // User EXAMPLE: "bunga 4%, tenor 6 bulan -> 4% dikenakan 6x".
            // This strongly implies provided rate (4) is MONTHLY rate.
            // Or maybe the user means 4% per month. 
            // I will use plafond.interestRate as MONTHLY percentage for this calculation.
            
            val monthlyInterestRatePercent = plafond.interestRate // e.g. 4.0
            val totalInterestPercent = monthlyInterestRatePercent * tenor // e.g. 24.0%
            
            val totalInterest = (amount * (totalInterestPercent / 100.0)).toLong()
            val totalRepayment = amount + totalInterest
            val monthlyInstallment = totalRepayment / tenor



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
