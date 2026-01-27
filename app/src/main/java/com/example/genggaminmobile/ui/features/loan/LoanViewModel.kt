package com.example.genggaminmobile.ui.features.loan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genggaminmobile.domain.model.Plafond
import com.example.genggaminmobile.domain.repository.LoanRepository
import com.example.genggaminmobile.domain.repository.PlafondRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoanApplicationUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val plafonds: List<Plafond> = emptyList(),
    val selectedPlafond: Plafond? = null,
    val amountInput: String = "",
    val tenorInput: String = "",
    val purposeInput: String = ""
)

@HiltViewModel
class LoanViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val plafondRepository: PlafondRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoanApplicationUiState())
    val uiState: StateFlow<LoanApplicationUiState> = _uiState.asStateFlow()

    init {
        loadPlafonds()
    }

    private fun loadPlafonds() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            plafondRepository.getAllPlafonds().fold(
                onSuccess = { plafonds ->
                    _uiState.value = _uiState.value.copy(
                        plafonds = plafonds.filter { it.isActive },
                        isLoading = false
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        error = "Gagal memuat produk pinjaman",
                        isLoading = false
                    )
                }
            )
        }
    }

    fun onPlafondSelected(plafond: Plafond) {
        _uiState.value = _uiState.value.copy(selectedPlafond = plafond)
    }

    fun onAmountChanged(amount: String) {
        _uiState.value = _uiState.value.copy(amountInput = amount)
    }

    fun onTenorChanged(tenor: String) {
        _uiState.value = _uiState.value.copy(tenorInput = tenor)
    }

    fun onPurposeChanged(purpose: String) {
        _uiState.value = _uiState.value.copy(purposeInput = purpose)
    }

    fun submitLoan() {
        val state = _uiState.value
        if (state.selectedPlafond == null || state.amountInput.isBlank() || state.tenorInput.isBlank() || state.purposeInput.isBlank()) {
            _uiState.value = state.copy(error = "Mohon lengkapi semua data")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            val amount = state.amountInput.toLongOrNull() ?: 0L
            val tenor = state.tenorInput.toIntOrNull() ?: 0

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
        _uiState.value = LoanApplicationUiState(plafonds = _uiState.value.plafonds)
    }
}
