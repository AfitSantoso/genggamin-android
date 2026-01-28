package com.example.genggaminmobile.ui.features.loan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genggaminmobile.domain.model.Loan
import com.example.genggaminmobile.domain.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoanHistoryUiState(
    val loans: List<Loan> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoanHistoryViewModel @Inject constructor(
    private val loanRepository: LoanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoanHistoryUiState())
    val uiState: StateFlow<LoanHistoryUiState> = _uiState.asStateFlow()

    init {
        loadLoans()
    }

    fun loadLoans() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            loanRepository.getMyLoans().fold(
                onSuccess = { loans ->
                    _uiState.value = _uiState.value.copy(
                        loans = loans,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Gagal memuat riwayat peminjaman",
                        isLoading = false
                    )
                }
            )
        }
    }
}
