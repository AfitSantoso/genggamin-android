package com.example.genggaminmobile.ui.features.auth.login

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoginSuccess: Boolean = false,
)
