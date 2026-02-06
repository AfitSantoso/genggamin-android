package com.example.genggaminmobile.ui.features.loan

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
    import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genggaminmobile.domain.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoanProgressViewModel @Inject constructor(
    private val loanRepository: LoanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoanProgressUiState())
    val uiState: StateFlow<LoanProgressUiState> = _uiState.asStateFlow()

    fun startTracking(loanId: Long) {
        viewModelScope.launch {
            // 1. Initial Load & Setup
            val initialLoan = loanRepository.getLoanById(loanId)
            
            if (initialLoan != null) {
                // Initial setup time & steps
                val actualSubmissionTime = initialLoan.createdAt ?: initialLoan.submittedAt
                _uiState.update { it.copy(submissionTime = actualSubmissionTime) }
                initializeProgressSteps(actualSubmissionTime)
                
                // Initial status update
                updateProgressBasedOnStatus(initialLoan)
                
                // 3-SECOND TIMER LOGIC for Step 0 (Submission)
                val normalizedStatus = initialLoan.status.lowercase()
                if (normalizedStatus in listOf("submitted", "pending", "menunggu")) {
                    val now = System.currentTimeMillis()
                    val submissionTime = actualSubmissionTime ?: now
                    val elapsed = now - submissionTime
                    
                    if (elapsed > 3000) {
                        _uiState.update { it.copy(currentStep = 1, statusMessage = "Sedang Diverifikasi") }
                    } else {
                        val remainingDelay = 3000 - elapsed
                        if (remainingDelay > 0) {
                            viewModelScope.launch {
                                delay(remainingDelay)
                                _uiState.update { currentState ->
                                    if (currentState.currentStep == 0 && 
                                        currentState.status.lowercase() in listOf("submitted", "pending", "menunggu")) {
                                        currentState.copy(currentStep = 1, statusMessage = "Sedang Diverifikasi")
                                    } else {
                                        currentState
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Fallback if not found yet
                 val fallbackTime = System.currentTimeMillis()
                _uiState.update { it.copy(submissionTime = fallbackTime) }
                initializeProgressSteps(fallbackTime)
            }

            // 2. REACTIVE UPDATES: Observe Database Changes
            // This ensures verify/approve/disburse updates appear immediately
            launch {
                loanRepository.getLoanFlow(loanId).collect { loan ->
                    if (loan != null) {
                        // Update submission time if we finally got it from backend
                        if (_uiState.value.submissionTime == null && (loan.createdAt != null || loan.submittedAt != 0L)) {
                             val time = loan.createdAt ?: loan.submittedAt
                             _uiState.update { it.copy(submissionTime = time) }
                        }
                        
                        updateProgressBasedOnStatus(loan)
                    }
                }
            }
            
            // 3. BACKGROUND POLLING: Fetch from API -> DB
            launch {
                startBackgroundPolling(loanId)
            }
        }
    }

    private suspend fun startBackgroundPolling(loanId: Long) {
        var pollCount = 0
        val maxPolls = 200 // Poll for 10 minutes

        while (pollCount < maxPolls) {
            // Poll every 5 seconds
            delay(5000) 

            try {
                // Refresh loans from repository (API -> DB)
                // The UI will update automatically via the Flow collector above
                loanRepository.refreshLoans()
                
                // Check if we should stop polling (Final Status)
                val currentLoan = loanRepository.getLoanById(loanId)
                if (currentLoan?.status?.lowercase() in listOf("approved", "disbursed", "rejected", "cair", "disetujui", "ditolak")) {
                    return
                }
            } catch (e: Exception) {
                // Ignore network errors, keep polling
            }

            pollCount++
        }
    }

    private fun updateProgressBasedOnStatus(loan: com.example.genggaminmobile.domain.model.Loan) {
        val normalizedStatus = loan.status.lowercase()

        // Determine target step based on status
        val (targetStep, message) = when (normalizedStatus) {
            "submitted", "pending", "menunggu" -> {
                // If we've already auto-advanced to step 1 (after 3s timer), don't go back to 0.
                // Keep showing "Verifikasi Data" (Step 1) as processing.
                if (_uiState.value.currentStep >= 1) {
                     1 to "Sedang Diverifikasi"
                } else {
                     0 to "Pengajuan Diterima"
                }
            }
            "under_review", "proses_verifikasi" -> {
                2 to "Proses Analisa"
            }
            "approved", "disetujui" -> {
                2 to "Pengajuan Disetujui!"
            }
            "disbursed", "cair" -> {
                4 to "Dana Telah Cair!" // Step 4 = all steps complete (0,1,2,3 are green)
            }
            "rejected", "ditolak" -> {
                1 to "Pengajuan Ditolak"
            }
            else -> {
                _uiState.value.currentStep to _uiState.value.statusMessage
            }
        }

        _uiState.update { currentState ->
            val timestamp = loan.updatedAt ?: System.currentTimeMillis()
            
            // Mark ALL steps up to (and including) target step with timestamps
            // This ensures all previous checkmarks are shown as green
            val updatedSteps = currentState.progressSteps.mapIndexed { index, progressStep ->
                when {
                    // Keep existing timestamp if already set
                    progressStep.timestamp != null -> progressStep
                    
                    // For DISBURSED status, mark ALL steps (0,1,2,3) as completed
                    normalizedStatus in listOf("disbursed", "cair") -> {
                        progressStep.copy(timestamp = timestamp)
                    }
                    
                    // For APPROVED status, mark steps 0,1,2 as completed
                    normalizedStatus in listOf("approved", "disetujui") && index <= 2 -> {
                        progressStep.copy(timestamp = timestamp)
                    }
                    
                    // For UNDER_REVIEW status, mark steps 0,1 as completed
                    normalizedStatus in listOf("under_review", "proses_verifikasi") && index <= 1 -> {
                        progressStep.copy(timestamp = timestamp)
                    }
                    
                    // For SUBMITTED status, mark step 0 as completed
                    normalizedStatus in listOf("submitted", "pending", "menunggu") && index == 0 -> {
                        progressStep.copy(timestamp = timestamp)
                    }
                    
                    else -> progressStep
                }
            }

            // Adjust currentStep for UI display (max is 3 for the 4-step UI)
            val displayStep = if (targetStep >= 4) 3 else targetStep

            currentState.copy(
                currentStep = displayStep,
                status = loan.status,
                statusMessage = message,
                progressSteps = updatedSteps
            )
        }
    }
    
    private fun initializeProgressSteps(submissionTime: Long?) {
        val steps = listOf(
            ProgressStep(
                title = "Pengajuan Diproses",
                description = "Kami telah menerima pengajuan Anda.",
                icon = Icons.Default.Send,
                timestamp = submissionTime
            ),
            ProgressStep(
                title = "Verifikasi Data",
                description = "Sistem sedang memverifikasi data Anda.",
                icon = Icons.Default.Verified
            ),
            ProgressStep(
                title = "Analisa & Persetujuan",
                description = "Tim kami sedang menganalisa pengajuan Anda.",
                icon = Icons.Default.ThumbUp
            ),
            ProgressStep(
                title = "Pencairan Dana",
                description = "Dana akan segera ditransfer ke rekening Anda.",
                icon = Icons.Default.AccountBalanceWallet
            )
        )
        _uiState.update { it.copy(progressSteps = steps) }
    }
}

data class LoanProgressUiState(
    val currentStep: Int = 0,
    val status: String = "SUBMITTED",
    val statusMessage: String = "Memulai...",
    val submissionTime: Long? = null,
    val progressSteps: List<ProgressStep> = emptyList()
)

data class ProgressStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val timestamp: Long? = null
)
