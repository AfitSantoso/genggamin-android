package com.example.genggaminmobile.ui.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genggaminmobile.data.model.dto.CustomerProfileRequest
import com.example.genggaminmobile.data.model.dto.CustomerProfileResponse
import com.example.genggaminmobile.domain.repository.AuthRepository
import com.example.genggaminmobile.domain.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ProfileUiState(
    val profile: CustomerProfileResponse? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val isUpdateSuccess: Boolean = false,
    val currentStep: Int = 1,
    val totalSteps: Int = 5,
    val isEditing: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isLoggedIn: Boolean = true,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        checkLoginStatus()
        loadProfile()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            authRepository.getAuthToken().collect { token ->
                _uiState.update { it.copy(isLoggedIn = !token.isNullOrBlank()) }
            }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            customerRepository.getProfile().fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            profile = profile,
                            isLoading = false,
                            isEditing = false,
                            lastUpdated = System.currentTimeMillis(),
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message,
                            isLoading = false,
                            isEditing = it.profile == null, // Force edit mode if no profile
                        )
                    }
                },
            )
        }
    }

    fun startEditing() {
        _uiState.update { it.copy(isEditing = true, currentStep = 1) }
    }

    fun cancelEditing() {
        if (_uiState.value.profile != null) {
            _uiState.update { it.copy(isEditing = false) }
        }
    }

    fun nextStep() {
        _uiState.update {
            if (it.currentStep < it.totalSteps) it.copy(currentStep = it.currentStep + 1) else it
        }
    }

    fun previousStep() {
        _uiState.update {
            if (it.currentStep > 1) it.copy(currentStep = it.currentStep - 1) else it
        }
    }

    fun submitProfile(
        request: CustomerProfileRequest,
        ktp: File?,
        selfie: File?,
        payslip: File?,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            customerRepository.createOrUpdateProfile(request, ktp, selfie, payslip).fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            profile = profile,
                            isSubmitting = false,
                            isUpdateSuccess = true,
                            isEditing = false,
                            lastUpdated = System.currentTimeMillis(),
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message, isSubmitting = false) }
                },
            )
        }
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLogoutSuccess()
        }
    }

    fun resetUpdateSuccess() {
        _uiState.update { it.copy(isUpdateSuccess = false) }
    }
}
