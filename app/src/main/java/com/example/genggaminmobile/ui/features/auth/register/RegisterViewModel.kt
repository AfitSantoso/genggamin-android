package com.example.genggaminmobile.ui.features.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genggaminmobile.domain.usecase.auth.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onFullNameChange(value: String) {
        _uiState.update {
            it.copy(
                fullName = value,
                fullNameError = if (value.isBlank()) "Nama lengkap tidak boleh kosong" else null,
                error = null
            )
        }
    }

    fun onUsernameChange(value: String) {
        _uiState.update {
            it.copy(
                username = value,
                usernameError = if (value.isBlank()) "Username tidak boleh kosong" else null,
                error = null
            )
        }
    }

    fun onEmailChange(value: String) {
        val emailPattern = android.util.Patterns.EMAIL_ADDRESS
        val error = when {
            value.isBlank() -> "Email tidak boleh kosong"
            !emailPattern.matcher(value).matches() -> "Format email tidak valid"
            else -> null
        }
        _uiState.update { it.copy(email = value, emailError = error, error = null) }
    }

    fun onPasswordChange(value: String) {
        val error = when {
            value.isBlank() -> "Kata sandi tidak boleh kosong"
            value.length < 6 -> "Kata sandi minimal 6 karakter"
            else -> null
        }
        _uiState.update { it.copy(password = value, passwordError = error, error = null) }
    }

    fun register() {
        val currentState = _uiState.value
        if (!currentState.isFormValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            registerUseCase(
                username = currentState.username,
                email = currentState.email,
                password = currentState.password,
                fullName = currentState.fullName
            ).onSuccess {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            }.onFailure { error ->
                val errorMessage = error.message ?: "Registrasi gagal, silakan coba lagi"

                _uiState.update { state ->
                    // Logika pemetaan error dari backend ke field spesifik
                    when {
                        errorMessage.contains("Email", ignoreCase = true) ->
                            state.copy(isLoading = false, emailError = errorMessage)

                        errorMessage.contains("Username", ignoreCase = true) ->
                            state.copy(isLoading = false, usernameError = errorMessage)

                        else ->
                            state.copy(isLoading = false, error = errorMessage)
                    }
                }
            }
        }
    }
}