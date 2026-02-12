package com.example.genggaminmobile.ui.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genggaminmobile.domain.model.Loan
import com.example.genggaminmobile.domain.model.LoanLimit
import com.example.genggaminmobile.domain.model.Plafond
import com.example.genggaminmobile.domain.repository.AuthRepository
import com.example.genggaminmobile.domain.repository.CustomerRepository
import com.example.genggaminmobile.domain.repository.LoanRepository
import com.example.genggaminmobile.domain.repository.PlafondRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val plafonds: List<Plafond> = emptyList(),
    val loanLimits: List<LoanLimit> = emptyList(),
    val activeLoans: List<Loan> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val hasProfile: Boolean = false,
    val monthlyIncome: Long? = null,
    val showProfilePrompt: Boolean = false,
    val showPromoPopup: Boolean = false,
)

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val plafondRepository: PlafondRepository,
    private val authRepository: AuthRepository,
    private val customerRepository: CustomerRepository,
    private val loanRepository: LoanRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        checkLoginStatusAndLoadPlafonds()
    }

    private var hasShownPromo = false

    fun checkLoginStatusAndLoadPlafonds() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Only show promo once per session
            if (!hasShownPromo) {
                _uiState.value = _uiState.value.copy(showPromoPopup = true)
                hasShownPromo = true
            }

            val token = authRepository.getAuthToken().first()
            val isLoggedIn = !token.isNullOrBlank()

            _uiState.value =
                _uiState.value.copy(
                    isLoggedIn = isLoggedIn,
                    isLoading = false,
                )

            if (isLoggedIn) {
                launch { loadProfile() }
                launch { loadLimits() }
                launch { loadActiveLoans() }
            } else {
                launch { loadAllPlafonds() }
            }
        }
    }

    private suspend fun loadProfile() {
        customerRepository.getProfile().fold(
            onSuccess = { profile ->
                _uiState.value =
                    _uiState.value.copy(
                        hasProfile = true,
                        monthlyIncome = profile.monthlyIncome,
                    )
                loadPlafondsByIncome(profile.monthlyIncome)
            },
            onFailure = {
                _uiState.value =
                    _uiState.value.copy(
                        hasProfile = false,
                        monthlyIncome = null,
                    )
                loadAllPlafonds()
            },
        )
    }

    private suspend fun loadLimits() {
        viewModelScope.launch {
            loanRepository.getLimitsFlow().collect { limits ->
                _uiState.value = _uiState.value.copy(loanLimits = limits)
            }
        }
        loanRepository.getMyLimits()
    }

    private suspend fun loadActiveLoans() {
        // Karena getMyLoans() mengembalikan Flow, kita kumpulkan (collect) datanya
        loanRepository.getMyLoans().collect { loans ->
            _uiState.value = _uiState.value.copy(activeLoans = loans)
        }
    }

    private suspend fun loadAllPlafonds() {
        plafondRepository.getAllPlafonds().fold(
            onSuccess = { plafonds ->
                _uiState.value = _uiState.value.copy(plafonds = plafonds)
            },
            onFailure = { e ->
                _uiState.value = _uiState.value.copy(error = e.message)
            },
        )
    }

    private suspend fun loadPlafondsByIncome(income: Long) {
        plafondRepository.getPlafondsByIncome(income).fold(
            onSuccess = { plafonds ->
                _uiState.value = _uiState.value.copy(plafonds = plafonds)
            },
            onFailure = {
                loadAllPlafonds() // Fallback
            },
        )
    }

    fun onPlafondClick(plafond: Plafond) {
        if (!_uiState.value.isLoggedIn) {
            // UI handles login navigation
        } else if (!_uiState.value.hasProfile) {
            _uiState.value = _uiState.value.copy(showProfilePrompt = true)
        } else {
            // Ready for loan application
        }
    }

    fun dismissProfilePrompt() {
        _uiState.value = _uiState.value.copy(showProfilePrompt = false)
    }

    fun dismissPromoPopup() {
        _uiState.value = _uiState.value.copy(showPromoPopup = false)
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = HomeUiState() // Reset state
            checkLoginStatusAndLoadPlafonds()
        }
    }
}
