package com.example.genggaminmobile.domain.model

data class Notification(
    val id: Long,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: String,
    val type: String?,
    val loanId: Long?,
)
