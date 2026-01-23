package com.example.genggaminmobile.domain.usecase.auth

import com.example.genggaminmobile.domain.model.User
import com.example.genggaminmobile.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(username: String, password: String): Result<User> {
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(Exception("Username and password cannot be empty"))
        }
        return repository.login(username, password)
    }
}
