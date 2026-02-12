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
class RegisterViewModel
@Inject
constructor(
    private val registerUseCase: RegisterUseCase,
    private val authRepository: com.example.genggaminmobile.domain.repository.AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onFullNameChange(value: String) {
        _uiState.update {
            it.copy(
                fullName = value,
                fullNameError = if (value.isBlank()) "Nama lengkap tidak boleh kosong" else null,
                error = null,
            )
        }
    }

    fun onUsernameChange(value: String) {
        _uiState.update {
            it.copy(
                username = value,
                usernameError = if (value.isBlank()) "Username tidak boleh kosong" else null,
                error = null,
            )
        }
    }

    fun onEmailChange(value: String) {
        val emailPattern = android.util.Patterns.EMAIL_ADDRESS
        val error =
            when {
                value.isBlank() -> "Email tidak boleh kosong"
                !emailPattern.matcher(value).matches() -> "Format email tidak valid"
                else -> null
            }
        _uiState.update { it.copy(email = value, emailError = error, error = null) }
    }

    fun onPasswordChange(value: String) {
        val error =
            when {
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

            // 1. Call Register
            val registerResult = runCatching {
                registerUseCase(
                    username = currentState.username,
                    email = currentState.email,
                    password = currentState.password,
                    fullName = currentState.fullName,
                )
            }

            registerResult.fold(
                onSuccess = {
                    // 2. Register success, now auto-login
                    // We assume register use case returns success but might not return token directly if it follows clean architecture returning Result<Unit> or similar.
                    // So we call login explicitly.
                    loginAfterRegister()
                },
                onFailure = { error ->
                    handleError(error)
                }
            )
        }
    }

    private suspend fun loginAfterRegister() {
        val currentState = _uiState.value
        authRepository.login(
            username = currentState.username,
            password = currentState.password,
            fcmToken = null // Or fetch FCM token if needed, passing null for now as often it's optional or handled inside repo
        ).fold(
            onSuccess = {
                // 3. Login success, now we are fully signed in.
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            },
            onFailure = { error ->
                // Login failed after registration succeeded.
                // We should probably still navigate to login or show error.
                // Ideally, if register succeeds but login fails, user exists.
                // So we can tell them to login manually or show the specific error.
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Registrasi berhasil, namun gagal login otomatis: ${error.message}"
                    )
                }
            }
        )
    }

    private fun handleError(error: Throwable) {
        val errorMessage = error.message ?: "Registrasi gagal, silakan coba lagi"
        _uiState.update { state ->
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
