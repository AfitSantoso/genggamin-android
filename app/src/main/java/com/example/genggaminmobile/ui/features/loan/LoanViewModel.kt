package com.example.genggaminmobile.ui.features.loan

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.genggaminmobile.R
import com.example.genggaminmobile.data.local.entity.ContractStatus
import com.example.genggaminmobile.data.local.entity.PendingContractEntity
import com.example.genggaminmobile.data.model.dto.CustomerProfileResponse
import com.example.genggaminmobile.data.worker.ContractSyncWorker
import com.example.genggaminmobile.domain.model.LoanLimit
import com.example.genggaminmobile.domain.model.LoanSimulation
import com.example.genggaminmobile.domain.model.Plafond
import com.example.genggaminmobile.domain.repository.ContractRepository
import com.example.genggaminmobile.domain.repository.CustomerRepository
import com.example.genggaminmobile.domain.repository.LoanRepository
import com.example.genggaminmobile.domain.repository.PlafondRepository
import com.example.genggaminmobile.domain.util.ContractData
import com.example.genggaminmobile.domain.util.ContractPdfGenerator
import com.example.genggaminmobile.domain.util.LoanCalculator
import com.example.genggaminmobile.ui.components.SignaturePoint
import com.example.genggaminmobile.ui.components.signaturePathToBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class LoanApplicationUiState(
    val isLoading: Boolean = false,
    val isContractLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val plafonds: List<Plafond> = emptyList(),
    val limits: List<LoanLimit> = emptyList(),
    val selectedPlafond: Plafond? = null,
    val selectedLimit: LoanLimit? = null,
    val amountInput: String = "",
    val tenorInput: String = "",
    val purposeInput: String = "",
    val simulation: LoanSimulation? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationFetched: Boolean = false, // Track if GPS location was actually fetched
    // Contract related states
    val showContractDialog: Boolean = false,
    val customerProfile: CustomerProfileResponse? = null,
    val contractUrl: String? = null,
    // Offline-first states
    val isOfflineSubmission: Boolean = false,
    val offlineMessage: String? = null,
    // Payslip requirement for business loans
    val requiresPayslip: Boolean = false,
    val hasPayslip: Boolean = false,
)

@HiltViewModel
class LoanViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val plafondRepository: PlafondRepository,
    private val customerRepository: CustomerRepository,
    private val contractRepository: ContractRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    companion object {
        private const val TAG = "LoanViewModel"
        private const val CONTRACTS_DIR = "contracts"
    }

    private val _uiState = MutableStateFlow(LoanApplicationUiState())
    val uiState: StateFlow<LoanApplicationUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val profileResult = customerRepository.getProfile()
            val profile = profileResult.getOrNull()

            // Check if customer has uploaded payslip
            val hasPayslip = !profile?.payslipImagePath.isNullOrBlank()

            val plafondsResult = if (profile != null) {
                plafondRepository.getPlafondsByIncome(profile.monthlyIncome)
            } else {
                plafondRepository.getAllPlafonds()
            }

            val limitsResult = loanRepository.getMyLimits()

            if (plafondsResult.isSuccess && limitsResult.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    plafonds = plafondsResult.getOrNull()?.filter { it.isActive } ?: emptyList(),
                    limits = limitsResult.getOrNull() ?: emptyList(),
                    customerProfile = profile,
                    hasPayslip = hasPayslip,
                    isLoading = false,
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    error = context.getString(R.string.error_loan_load_failed),
                    isLoading = false,
                )
            }
        }
    }

    fun updateLocation(lat: Double, lon: Double) {
        Log.d(TAG, "updateLocation: lat=$lat, lon=$lon")
        _uiState.value = _uiState.value.copy(
            latitude = lat,
            longitude = lon,
            locationFetched = true,
        )
    }

    fun onPlafondSelected(plafond: Plafond) {
        val limit = _uiState.value.limits.find { it.plafondId == plafond.id.toLong() }

        // Check if this plafond requires payslip (business loan types)
        // Business loans are identified by keywords in the title
        val isBusinessLoan = isBusinessLoanPlafond(plafond)

        _uiState.value = _uiState.value.copy(
            selectedPlafond = plafond,
            selectedLimit = limit,
            amountInput = "",
            tenorInput = plafond.tenorMonth.toString(),
            simulation = null,
            requiresPayslip = isBusinessLoan,
            // Clear any previous error when changing plafond
            error = null,
        )
    }

    /**
     * Check if a plafond is a business loan type that requires payslip documentation.
     * Business loans require additional income verification to minimize default risk.
     */
    private fun isBusinessLoanPlafond(plafond: Plafond): Boolean {
        val lowerTitle = plafond.title.lowercase()
        return lowerTitle.contains("usaha") ||
            lowerTitle.contains("modal") ||
            lowerTitle.contains("business") ||
            lowerTitle.contains("umkm") ||
            lowerTitle.contains("wirausaha")
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

        val simulation = LoanCalculator().calculateSimulation(amount, tenor, plafond)
        _uiState.value = _uiState.value.copy(simulation = simulation)
    }

    fun onPurposeChanged(purpose: String) {
        _uiState.value = _uiState.value.copy(purposeInput = purpose)
    }

    /**
     * Called when user clicks "Ajukan Pinjaman Sekarang" button
     * This validates input and shows the contract dialog if valid
     */
    fun onApplyLoanClicked() {
        val state = _uiState.value
        val amount = state.amountInput.toLongOrNull() ?: 0L
        val tenor = state.tenorInput.toIntOrNull() ?: 0

        // Validate inputs before showing contract
        if (state.selectedPlafond == null) {
            _uiState.value = state.copy(error = context.getString(R.string.error_select_plafond))
            return
        }

        if (state.customerProfile == null) {
            _uiState.value = state.copy(error = context.getString(R.string.error_profile_incomplete))
            return
        }

        // Validate payslip requirement for business loans
        if (state.requiresPayslip && !state.hasPayslip) {
            _uiState.value = state.copy(
                error = context.getString(R.string.error_business_loan_payslip),
            )
            return
        }

        if (state.requiresPayslip && !isNetworkAvailable()) {
            _uiState.value = state.copy(
                error = context.getString(R.string.error_business_loan_network),
            )
            return
        }

        if (amount < 400000 || amount > state.selectedPlafond.maxAmount) {
            _uiState.value = state.copy(error = context.getString(R.string.error_loan_amount_range))
            return
        }

        val limit = state.selectedLimit
        if (limit != null && amount > limit.availableLimit) {
            _uiState.value = state.copy(error = context.getString(R.string.error_loan_amount_limit))
            return
        }

        if (tenor <= 0 || tenor > state.selectedPlafond.tenorMonth) {
            _uiState.value = state.copy(error = context.getString(R.string.error_tenor_invalid))
            return
        }

        if (state.purposeInput.isBlank()) {
            _uiState.value = state.copy(error = context.getString(R.string.error_purpose_empty))
            return
        }

        // Validate location data is available
        if (!state.locationFetched || (state.latitude == 0.0 && state.longitude == 0.0)) {
            Log.w(TAG, "Location not available: fetched=${state.locationFetched}, lat=${state.latitude}, lng=${state.longitude}")
            _uiState.value = state.copy(error = context.getString(R.string.error_location_not_available))
            return
        }

        Log.d(TAG, "onApplyLoanClicked: lat=${state.latitude}, lng=${state.longitude}, locationFetched=${state.locationFetched}")

        // All validations passed, show contract dialog
        _uiState.value = state.copy(error = null, showContractDialog = true)
    }

    /**
     * Dismiss the contract dialog
     */
    fun dismissContractDialog() {
        _uiState.value = _uiState.value.copy(showContractDialog = false)
    }

    /**
     * Called when user signs and confirms the contract
     * Uses offline-first approach:
     * 1. Generate PDF and save locally
     * 2. Save contract metadata to local database
     * 3. Try to upload and submit immediately if online
     * 4. If offline, schedule background sync
     */
    fun onContractSigned(signaturePath: List<SignaturePoint>) {
        val state = _uiState.value
        val profile = state.customerProfile ?: return
        val plafond = state.selectedPlafond ?: return
        val simulation = state.simulation ?: return
        val amount = state.amountInput.toLongOrNull() ?: return
        val tenor = state.tenorInput.toIntOrNull() ?: return

        viewModelScope.launch {
            _uiState.value = state.copy(isContractLoading = true, error = null)

            // Log location values for debugging
            Log.d(TAG, "onContractSigned: locationFetched=${state.locationFetched}, lat=${state.latitude}, lng=${state.longitude}")

            try {
                // 1. Convert signature path to Bitmap
                val signatureBitmap = withContext(Dispatchers.Default) {
                    signaturePathToBitmap(signaturePath)
                }

                // 2. Create contract data
                val contractData = ContractData(
                    customerName = profile.fullName,
                    customerNik = profile.nik,
                    customerAddress = profile.currentAddress,
                    amount = amount,
                    tenor = tenor,
                    interestRate = plafond.interestRate,
                    monthlyInstallment = simulation.monthlyInstallment,
                    totalRepayment = simulation.totalRepayment,
                    purpose = state.purposeInput,
                    signatureBitmap = signatureBitmap,
                )

                // 3. Generate PDF and save to persistent storage
                Log.d(TAG, "Generating contract PDF...")
                val pdfFile = withContext(Dispatchers.IO) {
                    val pdf = ContractPdfGenerator.generateContractPdf(context, contractData)

                    // Move to persistent storage (not cache)
                    val contractsDir = File(context.filesDir, CONTRACTS_DIR)
                    if (!contractsDir.exists()) {
                        contractsDir.mkdirs()
                    }

                    val persistentFile = File(contractsDir, "contract_${System.currentTimeMillis()}.pdf")
                    pdf.copyTo(persistentFile, overwrite = true)
                    pdf.delete() // Delete temp file
                    persistentFile
                }
                Log.d(TAG, "PDF saved: ${pdfFile.absolutePath}")

                // 4. Save contract to local database (offline-first)
                val pendingContract = PendingContractEntity(
                    customerName = profile.fullName,
                    customerNik = profile.nik,
                    customerAddress = profile.currentAddress,
                    amount = amount,
                    tenor = tenor,
                    interestRate = plafond.interestRate,
                    monthlyInstallment = simulation.monthlyInstallment,
                    totalRepayment = simulation.totalRepayment,
                    purpose = state.purposeInput,
                    plafondId = plafond.id.toLong(),
                    latitude = state.latitude,
                    longitude = state.longitude,
                    pdfFilePath = pdfFile.absolutePath,
                    status = ContractStatus.PENDING_UPLOAD,
                )

                val contractId = withContext(Dispatchers.IO) {
                    contractRepository.savePendingContract(pendingContract)
                }
                Log.d(TAG, "Contract saved locally with ID: $contractId")

                // 5. Check network and try immediate sync or schedule background sync
                if (isNetworkAvailable()) {
                    Log.d(TAG, "Network available, attempting immediate sync...")
                    tryImmediateSync(contractId)
                } else {
                    Log.d(TAG, "Network not available, scheduling background sync...")
                    scheduleBackgroundSync()

                    _uiState.value = _uiState.value.copy(
                        isContractLoading = false,
                        showContractDialog = false,
                        success = true,
                        isOfflineSubmission = true,
                        offlineMessage = context.getString(R.string.msg_submission_offline_saved),
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Contract generation error", e)
                _uiState.value = _uiState.value.copy(
                    isContractLoading = false,
                    error = context.getString(R.string.error_contract_process_failed, e.message),
                )
            }
        }
    }

    /**
     * Try to sync contract immediately when online
     */
    private suspend fun tryImmediateSync(contractId: Long) {
        try {
            // Upload PDF (Best Effort)
            val uploadResult = contractRepository.uploadContractPdf(contractId)
            val contractUrl = uploadResult.getOrNull()

            if (uploadResult.isSuccess) {
                Log.d(TAG, "Upload successful: $contractUrl")
            } else {
                Log.w(TAG, "Upload failed or skipped, proceeding to submission anyway")
            }

            // Submit loan regardless of upload result (Backend doesn't strictly require the file URL)
            val submitResult = contractRepository.submitContractLoan(contractId)

            if (submitResult.isSuccess) {
                Log.d(TAG, "Loan submitted successfully")
                _uiState.value = _uiState.value.copy(
                    isContractLoading = false,
                    showContractDialog = false,
                    success = true,
                    contractUrl = contractUrl,
                    isOfflineSubmission = false,
                )
            } else {
                // Submit failed - schedule retry
                Log.w(TAG, "Submit failed, scheduling retry")
                scheduleBackgroundSync()

                _uiState.value = _uiState.value.copy(
                    isContractLoading = false,
                    showContractDialog = false,
                    success = true,
                    isOfflineSubmission = true,
                    offlineMessage = context.getString(R.string.msg_submission_offline_stable),
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Immediate sync failed", e)
            scheduleBackgroundSync()

            _uiState.value = _uiState.value.copy(
                isContractLoading = false,
                showContractDialog = false,
                success = true,
                isOfflineSubmission = true,
                offlineMessage = context.getString(R.string.msg_submission_offline_saved),
            )
        }
    }

    /**
     * Schedule background sync using WorkManager
     */
    private fun scheduleBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequest.Builder(ContractSyncWorker::class.java)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "ContractSync",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            syncRequest,
        )

        Log.d(TAG, "Background sync scheduled")
    }

    /**
     * Check if network is available
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Legacy method - kept for backwards compatibility
     * Now redirects to the new contract flow
     */
    fun submitLoan() {
        onApplyLoanClicked()
    }

    fun resetState() {
        _uiState.value = LoanApplicationUiState(
            plafonds = _uiState.value.plafonds,
            limits = _uiState.value.limits,
            customerProfile = _uiState.value.customerProfile,
        )
    }

    /**
     * Get contract display info for the dialog
     */
    fun getContractDisplayInfo(): ContractDisplayInfo? {
        val state = _uiState.value
        val profile = state.customerProfile ?: return null
        val simulation = state.simulation ?: return null
        val amount = state.amountInput.toLongOrNull() ?: return null
        val tenor = state.tenorInput.toIntOrNull() ?: return null
        val plafond = state.selectedPlafond ?: return null

        return ContractDisplayInfo(
            customerName = profile.fullName,
            customerNik = profile.nik,
            customerAddress = profile.currentAddress,
            amount = amount,
            tenor = tenor,
            interestRate = plafond.interestRate,
            monthlyInstallment = simulation.monthlyInstallment,
            totalRepayment = simulation.totalRepayment,
            purpose = state.purposeInput,
        )
    }
}
