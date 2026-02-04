package com.example.genggaminmobile.ui.features.auth.register

data class RegisterUiState(
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val fullNameError: String? = null,
    val usernameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
) {
    val isFormValid: Boolean
        get() =
            fullName.isNotBlank() &&
                username.isNotBlank() &&
                email.isNotBlank() &&
                password.isNotBlank() &&
                fullNameError == null &&
                usernameError == null &&
                emailError == null &&
                passwordError == null
}
