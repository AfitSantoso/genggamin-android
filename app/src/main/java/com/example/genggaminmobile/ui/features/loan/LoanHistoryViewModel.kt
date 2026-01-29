package com.example.genggaminmobile.ui.features.loan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genggaminmobile.domain.model.Loan
import com.example.genggaminmobile.domain.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoanHistoryUiState(
    val allLoans: List<Loan> = emptyList(),
    val filteredLoans: List<Loan> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFilter: String = "ALL",
    val isHistoryView: Boolean = false
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
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // Pemicu sinkronisasi data dari API agar database lokal terisi
            loanRepository.refreshLoans()
            
            loanRepository.getMyLoans()
                .catch { e ->
                    _uiState.update { it.copy(
                        error = e.message ?: "Gagal memuat riwayat peminjaman",
                        isLoading = false
                    ) }
                }
                .collect { loans ->
                    _uiState.update { state ->
                        state.copy(
                            allLoans = loans,
                            isLoading = false
                        )
                    }
                    applyFilter(_uiState.value.selectedFilter, _uiState.value.isHistoryView)
                }
        }
    }

    fun setHistoryView(isHistory: Boolean) {
        _uiState.update { it.copy(isHistoryView = isHistory) }
        applyFilter(_uiState.value.selectedFilter, isHistory)
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
        applyFilter(filter, _uiState.value.isHistoryView)
    }

    private fun applyFilter(filter: String, isHistory: Boolean) {
        val loans = _uiState.value.allLoans
        
        val filtered = if (!isHistory) {
            // Pinjaman Aktif: Semua pinjaman yang tidak ditolak (termasuk yang sedang proses maupun yang sudah cair)
            loans.filter { 
                val status = it.status.lowercase()
                status !in listOf("rejected", "ditolak")
            }
        } else {
            // Riwayat: Semua pinjaman dengan filter
            when (filter) {
                // Filter "Aktif" di Riwayat sekarang merujuk ke pinjaman yang sudah cair/disbursed
                "ACTIVE" -> loans.filter { it.status.lowercase() in listOf("disbursed", "cair") }
                "REJECTED" -> loans.filter { it.status.lowercase() in listOf("rejected", "ditolak") }
                else -> loans // ALL
            }
        }

        _uiState.update { it.copy(filteredLoans = filtered) }
    }
}
