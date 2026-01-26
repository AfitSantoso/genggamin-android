package com.example.genggaminmobile.ui.features.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genggaminmobile.domain.usecase.auth.LoginUseCase
import com.example.genggaminmobile.domain.repository.AuthRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username, error = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun setErrorMessage(message: String?) {
        _uiState.update { it.copy(error = message, isLoading = false) }
    }

    fun login() {
        val currentState = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val fcmToken = getFcmToken()

                loginUseCase(currentState.username, currentState.password, fcmToken)
                    .onSuccess {
                        _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isLoading = false, error = error.message ?: "Terjadi kesalahan") }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Terjadi kesalahan") }
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val fcmToken = getFcmToken()
                authRepository.loginGoogle(idToken, fcmToken)
                    .onSuccess {
                        _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isLoading = false, error = error.message ?: "Gagal login Google") }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Terjadi kesalahan") }
            }
        }
    }

    private suspend fun getFcmToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            null
        }
    }
}
