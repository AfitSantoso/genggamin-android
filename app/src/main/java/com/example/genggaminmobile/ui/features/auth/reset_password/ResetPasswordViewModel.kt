package com.example.genggaminmobile.ui.features.auth.reset_password

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genggaminmobile.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResetPasswordUiState(
    val token: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val successMessage: String? = null
)

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    init {
        // Get token from navigation arguments if available
        savedStateHandle.get<String>("token")?.let { token ->
            _uiState.update { it.copy(token = token) }
        }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(newPassword = password, error = null) }
    }

    fun onConfirmPasswordChange(password: String) {
        _uiState.update { it.copy(confirmPassword = password, error = null) }
    }

    fun resetPassword() {
        val state = _uiState.value
        if (state.newPassword != state.confirmPassword) {
            _uiState.update { it.copy(error = "Kata sandi tidak cocok") }
            return
        }
        
        if (state.newPassword.length < 6) {
            _uiState.update { it.copy(error = "Kata sandi minimal 6 karakter") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.resetPassword(state.token, state.newPassword)
                .onSuccess { message ->
                    _uiState.update { it.copy(isLoading = false, isSuccess = true, successMessage = message) }
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message) }
                }
        }
    }
}
