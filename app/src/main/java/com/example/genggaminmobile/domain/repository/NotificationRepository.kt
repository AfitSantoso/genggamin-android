package com.example.genggaminmobile.domain.repository

import com.example.genggaminmobile.domain.model.Notification

interface NotificationRepository {
    suspend fun getNotifications(page: Int = 0, size: Int = 20): Result<List<Notification>>
    suspend fun markAllAsRead(): Result<Unit>
    suspend fun markAsRead(id: Long): Result<Unit>
}
