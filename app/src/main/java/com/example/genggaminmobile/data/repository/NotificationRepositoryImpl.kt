package com.example.genggaminmobile.data.repository

import com.example.genggaminmobile.data.remote.api.NotificationApi
import com.example.genggaminmobile.domain.model.Notification
import com.example.genggaminmobile.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl
@Inject
constructor(
    private val notificationApi: NotificationApi,
) : NotificationRepository {
    override suspend fun getNotifications(
        page: Int,
        size: Int,
    ): Result<List<Notification>> {
        return try {
            val response = notificationApi.getNotifications(page, size)
            if (response.success && response.data != null) {
                Result.success(
                    response.data.content.map { dto ->
                        Notification(
                            id = dto.id,
                            title = dto.title,
                            message = dto.message,
                            isRead = dto.read,
                            createdAt = dto.createdAt,
                            type = dto.type,
                            loanId = dto.loanId,
                        )
                    },
                )
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAllAsRead(): Result<Unit> {
        return try {
            val response = notificationApi.markAllAsRead()
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(id: Long): Result<Unit> {
        return try {
            val response = notificationApi.markAsRead(id)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
