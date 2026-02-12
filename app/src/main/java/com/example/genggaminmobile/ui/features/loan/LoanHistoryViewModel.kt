package com.example.genggaminmobile.ui.features.loan

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genggaminmobile.data.local.entity.ContractStatus
import com.example.genggaminmobile.data.local.entity.PendingContractEntity
import com.example.genggaminmobile.domain.model.Loan
import com.example.genggaminmobile.domain.repository.ContractRepository
import com.example.genggaminmobile.domain.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoanHistoryUiState(
    val allLoans: List<Loan> = emptyList(),
    val filteredLoans: List<Loan> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFilter: String = "ALL",
    val isHistoryView: Boolean = false,
    val isCancelling: Boolean = false,
    val cancelSuccess: Boolean = false,
    val cancelError: String? = null,
    val pendingContractsCount: Int = 0,
)

@HiltViewModel
class LoanHistoryViewModel
@Inject
constructor(
    private val loanRepository: LoanRepository,
    private val contractRepository: ContractRepository,
) : ViewModel() {

    companion object {
        private const val TAG = "LoanHistoryViewModel"
    }

    private val _uiState = MutableStateFlow(LoanHistoryUiState())
    val uiState: StateFlow<LoanHistoryUiState> = _uiState.asStateFlow()

    private var pollingJob: kotlinx.coroutines.Job? = null

    init {
        loadLoans()
        startPolling()
        observePendingContracts()
    }

    /**
     * Observe pending contracts and combine with regular loans
     * This provides reactive updates when contracts are synced
     */
    private fun observePendingContracts() {
        viewModelScope.launch {
            combine(
                loanRepository.getMyLoans(),
                contractRepository.getPendingContracts(),
            ) { loans, pendingContracts ->
                Pair(loans, pendingContracts)
            }
                .catch { e ->
                    Log.e(TAG, "Error observing loans/contracts", e)
                    _uiState.update {
                        it.copy(
                            error = e.message ?: "Gagal memuat data",
                            isLoading = false,
                        )
                    }
                }
                .distinctUntilChanged() // Prevent duplicate emissions
                .collect { (loans, pendingContracts) ->
                    // Filter out SUBMITTED and UPLOADING that have been processed
                    val activePendingContracts = pendingContracts.filter { contract ->
                        contract.status != ContractStatus.SUBMITTED
                    }

                    Log.d(TAG, "Loans: ${loans.size}, Pending: ${activePendingContracts.size}")

                    // Convert pending contracts to Loan objects with offline status
                    val pendingLoans = activePendingContracts.map { contract -> contract.toLoan() }

                    // Combine: pending contracts first, then regular loans
                    val combinedLoans = pendingLoans + loans

                    _uiState.update { state ->
                        state.copy(
                            allLoans = combinedLoans,
                            isLoading = false,
                            pendingContractsCount = pendingLoans.size,
                        )
                    }
                    applyFilter(_uiState.value.selectedFilter, _uiState.value.isHistoryView)
                }
        }
    }

    /**
     * Convert PendingContractEntity to Loan for display
     */
    private fun PendingContractEntity.toLoan(): Loan {
        // Determine display status based on contract status
        // UPLOADING should show as "Mengunggah" only briefly during actual upload
        // If it stays UPLOADING for too long, it means upload failed and should show as pending
        val isStaleUploading = status == ContractStatus.UPLOADING &&
            lastAttemptAt != null &&
            (System.currentTimeMillis() - lastAttemptAt) > 30000 // More than 30 seconds = stale

        val displayStatus = when {
            isStaleUploading -> "PENDING (Offline)" // Stale uploading, treat as pending
            status == ContractStatus.PENDING_UPLOAD -> "PENDING (Offline)"
            status == ContractStatus.UPLOADING -> "MENGUNGGAH..."
            status == ContractStatus.UPLOADED -> "PENDING (Mengirim)"
            status == ContractStatus.PENDING_SUBMIT -> "PENDING (Mengirim)"
            status == ContractStatus.FAILED -> "GAGAL (Offline)"
            else -> "PENDING (Offline)"
        }

        return Loan(
            id = -id, // Negative ID to distinguish from real loans
            amount = amount,
            tenorMonths = tenor,
            purpose = if (!errorMessage.isNullOrBlank()) "$purpose (Error: $errorMessage)" else purpose,
            plafondId = plafondId,
            status = displayStatus,
            interestRate = interestRate,
            date = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                java.util.Locale.getDefault(),
            ).format(java.util.Date(createdAt)),
            submittedAt = createdAt,
            createdAt = createdAt,
            updatedAt = lastAttemptAt,
        )
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(10000) // Poll every 10 seconds
                try {
                    // Refresh from backend silently
                    loanRepository.refreshLoans()
                    // Also try to sync pending contracts
                    contractRepository.syncPendingContracts()
                } catch (e: Exception) {
                    // Ignore errors during polling
                    Log.w(TAG, "Polling error", e)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    fun loadLoans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Trigger sync from API to update local database
            try {
                loanRepository.refreshLoans()
                // Also sync pending contracts
                contractRepository.syncPendingContracts()
            } catch (e: Exception) {
                Log.w(TAG, "Refresh error", e)
            }

            // The observePendingContracts() will handle the reactive updates
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

    private fun applyFilter(
        filter: String,
        isHistory: Boolean,
    ) {
        val loans = _uiState.value.allLoans

        val filtered =
            if (!isHistory) {
                // Pinjaman Aktif: Semua pinjaman yang tidak ditolak (termasuk yang sedang proses maupun yang sudah cair)
                // Also include offline pending loans
                loans.filter {
                    val status = it.status.lowercase()
                    status !in listOf("rejected", "ditolak") &&
                        !status.contains("gagal")
                }
            } else {
                // Riwayat: Semua pinjaman dengan filter
                when (filter) {
                    // Filter "Aktif" di Riwayat sekarang merujuk ke pinjaman yang sudah cair/disbursed
                    "ACTIVE" -> loans.filter { it.status.lowercase() in listOf("disbursed", "cair") }
                    "REJECTED" -> loans.filter {
                        val status = it.status.lowercase()
                        status in listOf("rejected", "ditolak") || status.contains("gagal")
                    }
                    else -> loans // ALL
                }
            }

        _uiState.update { it.copy(filteredLoans = filtered) }
    }

    /**
     * Cancel an offline loan application.
     * This will delete the loan from local database and restore the limit.
     * Also handles pending contracts cancellation.
     */
    fun cancelOfflineLoan(localId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true, cancelError = null) }

            // Check if this is a pending contract (negative ID)
            if (localId < 0) {
                // It's a pending contract
                val contractId = -localId
                try {
                    contractRepository.deleteContract(contractId)
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            cancelSuccess = true,
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            cancelError = e.message ?: "Gagal membatalkan pengajuan",
                        )
                    }
                }
            } else {
                // It's a regular offline loan
                val result = loanRepository.cancelOfflineLoan(localId)
                result.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isCancelling = false,
                                cancelSuccess = true,
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isCancelling = false,
                                cancelError = error.message ?: "Gagal membatalkan pengajuan",
                            )
                        }
                    },
                )
            }
        }
    }

    /**
     * Clear cancel state after showing feedback to user
     */
    fun clearCancelState() {
        _uiState.update {
            it.copy(
                cancelSuccess = false,
                cancelError = null,
            )
        }
    }
}
