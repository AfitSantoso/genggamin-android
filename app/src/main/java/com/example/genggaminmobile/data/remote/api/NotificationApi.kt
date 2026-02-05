package com.example.genggaminmobile.data.remote.api

import com.example.genggaminmobile.data.model.dto.NotificationListResponse
import com.example.genggaminmobile.data.model.dto.NotificationResponse
import com.example.genggaminmobile.data.model.dto.UnreadCountResponse
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApi {
    @GET("api/notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): NotificationListResponse

    @GET("api/notifications/unread-count")
    suspend fun getUnreadCount(): UnreadCountResponse

    @PATCH("api/notifications/read-all")
    suspend fun markAllAsRead(): NotificationResponse

    @PATCH("api/notifications/{id}/read")
    suspend fun markAsRead(
        @Path("id") id: Long,
    ): NotificationResponse
}
