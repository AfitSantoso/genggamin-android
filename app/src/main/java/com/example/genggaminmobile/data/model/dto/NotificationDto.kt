package com.example.genggaminmobile.data.model.dto

import com.google.gson.annotations.SerializedName

data class NotificationDto(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("read") val read: Boolean,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("type") val type: String? = null,
    @SerializedName("loanId") val loanId: Long? = null,
)

data class NotificationListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: NotificationPageData?,
)

data class NotificationPageData(
    @SerializedName("content") val content: List<NotificationDto>,
)

data class NotificationResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: NotificationDto?,
)
