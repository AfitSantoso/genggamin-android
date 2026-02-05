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
    @SerializedName("totalPages") val totalPages: Int = 0,
    @SerializedName("totalElements") val totalElements: Long = 0,
    @SerializedName("last") val last: Boolean = true,
    @SerializedName("first") val first: Boolean = true,
    @SerializedName("empty") val empty: Boolean = true,
    @SerializedName("number") val number: Int = 0,
    @SerializedName("size") val size: Int = 20,
)

data class NotificationResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: Any?,
)

data class UnreadCountResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: Int,
)
