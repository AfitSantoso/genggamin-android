package com.example.genggaminmobile.domain.model

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val fullName: String?,
    val isActive: Boolean,
    val roles: List<String>,
    val token: String? = null,
)
