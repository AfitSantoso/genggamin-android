package com.example.genggaminmobile.domain.repository

import com.example.genggaminmobile.domain.model.Notification

/**
 * Repository interface for notification operations.
 * Handles fetching, reading, and managing user notifications.
 */
interface NotificationRepository {
    /**
     * Get paginated list of notifications for the current user.
     * @param page Page number (0-indexed)
     * @param size Number of items per page
     * @return Result containing list of notifications or error
     */
    suspend fun getNotifications(
        page: Int = 0,
        size: Int = 20,
    ): Result<NotificationResult>

    /**
     * Get the count of unread notifications for badge display.
     * @return Result containing unread count or error
     */
    suspend fun getUnreadCount(): Result<Int>

    /**
     * Mark all notifications as read.
     * @return Result indicating success or error
     */
    suspend fun markAllAsRead(): Result<Unit>

    /**
     * Mark a specific notification as read.
     * @param id Notification ID to mark as read
     * @return Result indicating success or error
     */
    suspend fun markAsRead(id: Long): Result<Unit>
}

/**
 * Data class containing notification list with pagination info.
 */
data class NotificationResult(
    val notifications: List<Notification>,
    val totalPages: Int,
    val totalElements: Long,
    val isLastPage: Boolean,
    val currentPage: Int,
)
