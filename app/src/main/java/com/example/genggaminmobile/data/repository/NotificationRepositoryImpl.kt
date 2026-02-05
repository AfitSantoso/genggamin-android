package com.example.genggaminmobile.data.repository

import com.example.genggaminmobile.data.remote.api.NotificationApi
import com.example.genggaminmobile.domain.model.Notification
import com.example.genggaminmobile.domain.repository.NotificationRepository
import com.example.genggaminmobile.domain.repository.NotificationResult
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
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
    ): Result<NotificationResult> {
        return try {
            val response = notificationApi.getNotifications(page, size)
            if (response.success && response.data != null) {
                val notifications = response.data.content.map { dto ->
                    Notification(
                        id = dto.id,
                        title = dto.title,
                        message = dto.message,
                        isRead = dto.read,
                        createdAt = dto.createdAt,
                        type = dto.type,
                        loanId = dto.loanId,
                    )
                }
                Result.success(
                    NotificationResult(
                        notifications = notifications,
                        totalPages = response.data.totalPages,
                        totalElements = response.data.totalElements,
                        isLastPage = response.data.last,
                        currentPage = response.data.number,
                    ),
                )
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "401 Unauthorized - Silakan login terlebih dahulu"
                403 -> "403 Forbidden - Akses ditolak"
                404 -> "Data tidak ditemukan"
                500 -> "Terjadi kesalahan pada server"
                else -> "HTTP Error ${e.code()}: ${e.message()}"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Koneksi timeout - server tidak merespons"))
        } catch (e: UnknownHostException) {
            Result.failure(Exception("Tidak dapat terhubung ke server"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUnreadCount(): Result<Int> {
        return try {
            val response = notificationApi.getUnreadCount()
            if (response.success) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: HttpException) {
            // Silently fail for unread count - it's not critical
            Result.failure(Exception("HTTP ${e.code()}"))
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
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Sesi login telah berakhir"
                else -> "Gagal menandai notifikasi"
            }
            Result.failure(Exception(errorMessage))
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
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Sesi login telah berakhir"
                404 -> "Notifikasi tidak ditemukan"
                else -> "Gagal menandai notifikasi"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
