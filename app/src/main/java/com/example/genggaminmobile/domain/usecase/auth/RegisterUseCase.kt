package com.example.genggaminmobile.domain.usecase.auth

import com.example.genggaminmobile.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase
@Inject
constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(
        username: String,
        email: String,
        password: String,
        fullName: String,
    ): Result<Unit> {
        if (username.isBlank() || email.isBlank() || password.isBlank() || fullName.isBlank()) {
            return Result.failure(Exception("All fields are required"))
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.failure(Exception("Invalid email format"))
        }
        return repository.register(username, email, password, fullName)
    }
}
