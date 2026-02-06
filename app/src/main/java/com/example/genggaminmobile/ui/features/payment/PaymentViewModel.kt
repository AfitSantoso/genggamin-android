package com.example.genggaminmobile.ui.features.payment

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

data class PaymentUiState(
    val activeLoans: List<Loan> = emptyList(),
    val selectedLoan: Loan? = null,
    val selectedPaymentMethod: PaymentMethod? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showPaymentMethodSheet: Boolean = false,
    val showPaymentDetail: Boolean = false,
)

enum class PaymentMethod(
    val id: String,
    val title: String,
    val subtitle: String,
    val adminFee: Long = 0,
) {
    VIRTUAL_ACCOUNT_BCA(
        id = "va_bca",
        title = "BCA Virtual Account",
        subtitle = "Proses instan 24 jam",
        adminFee = 0,
    ),
    VIRTUAL_ACCOUNT_BNI(
        id = "va_bni",
        title = "BNI Virtual Account",
        subtitle = "Proses instan 24 jam",
        adminFee = 0,
    ),
    VIRTUAL_ACCOUNT_BRI(
        id = "va_bri",
        title = "BRI Virtual Account",
        subtitle = "Proses instan 24 jam",
        adminFee = 0,
    ),
    VIRTUAL_ACCOUNT_MANDIRI(
        id = "va_mandiri",
        title = "Mandiri Virtual Account",
        subtitle = "Proses instan 24 jam",
        adminFee = 0,
    ),
    ATM_TRANSFER(
        id = "atm",
        title = "Transfer ATM",
        subtitle = "Semua bank, 1x24 jam",
        adminFee = 2500,
    ),
    ALFAMART(
        id = "alfamart",
        title = "Alfamart",
        subtitle = "Bayar di gerai terdekat",
        adminFee = 2500,
    ),
    INDOMARET(
        id = "indomaret",
        title = "Indomaret",
        subtitle = "Bayar di gerai terdekat",
        adminFee = 5000,
    ),
}

data class PaymentMethodCategory(
    val title: String,
    val methods: List<PaymentMethod>,
)

@HiltViewModel
class PaymentViewModel
@Inject
constructor(
    private val loanRepository: LoanRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    init {
        loadActiveLoans()
    }

    fun loadActiveLoans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Refresh data from backend
            try {
                loanRepository.refreshLoans()
            } catch (e: Exception) {
                // Continue with cached data if refresh fails
            }

            loanRepository.getMyLoans()
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            error = e.message ?: "Gagal memuat data pinjaman",
                            isLoading = false,
                        )
                    }
                }
                .collect { loans ->
                    // Filter only disbursed loans that need payment
                    val activeLoans = loans.filter {
                        it.status.lowercase() in listOf("disbursed", "cair", "active")
                    }
                    _uiState.update {
                        it.copy(
                            activeLoans = activeLoans,
                            isLoading = false,
                        )
                    }
                }
        }
    }

    fun selectLoan(loan: Loan) {
        _uiState.update {
            it.copy(
                selectedLoan = loan,
                showPaymentMethodSheet = true,
            )
        }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _uiState.update {
            it.copy(
                selectedPaymentMethod = method,
                showPaymentMethodSheet = false,
                showPaymentDetail = true,
            )
        }
    }

    fun dismissPaymentMethodSheet() {
        _uiState.update {
            it.copy(showPaymentMethodSheet = false)
        }
    }

    fun dismissPaymentDetail() {
        _uiState.update {
            it.copy(
                showPaymentDetail = false,
                selectedPaymentMethod = null,
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedLoan = null,
                selectedPaymentMethod = null,
                showPaymentMethodSheet = false,
                showPaymentDetail = false,
            )
        }
    }

    fun getPaymentMethodCategories(): List<PaymentMethodCategory> {
        return listOf(
            PaymentMethodCategory(
                title = "Virtual Account",
                methods = listOf(
                    PaymentMethod.VIRTUAL_ACCOUNT_BCA,
                    PaymentMethod.VIRTUAL_ACCOUNT_BNI,
                    PaymentMethod.VIRTUAL_ACCOUNT_BRI,
                    PaymentMethod.VIRTUAL_ACCOUNT_MANDIRI,
                ),
            ),
            PaymentMethodCategory(
                title = "Transfer Bank",
                methods = listOf(PaymentMethod.ATM_TRANSFER),
            ),
            PaymentMethodCategory(
                title = "Gerai Retail",
                methods = listOf(
                    PaymentMethod.ALFAMART,
                    PaymentMethod.INDOMARET,
                ),
            ),
        )
    }

    /**
     * Generate Virtual Account number for demo purposes
     */
    fun generateVANumber(loan: Loan, method: PaymentMethod): String {
        val prefix = when (method) {
            PaymentMethod.VIRTUAL_ACCOUNT_BCA -> "8888"
            PaymentMethod.VIRTUAL_ACCOUNT_BNI -> "8877"
            PaymentMethod.VIRTUAL_ACCOUNT_BRI -> "8866"
            PaymentMethod.VIRTUAL_ACCOUNT_MANDIRI -> "8855"
            else -> "9999"
        }
        val loanIdPart = (loan.id ?: 0).toString().padStart(8, '0')
        return "$prefix$loanIdPart"
    }

    /**
     * Generate payment code for retail outlets
     */
    fun generatePaymentCode(loan: Loan): String {
        val timestamp = System.currentTimeMillis().toString().takeLast(8)
        val loanIdPart = (loan.id ?: 0).toString().padStart(4, '0')
        return "GGM$loanIdPart$timestamp"
    }
}
