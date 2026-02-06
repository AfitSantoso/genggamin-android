package com.example.genggaminmobile.ui.features.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genggaminmobile.domain.model.Notification
import com.example.genggaminmobile.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for Notification screen.
 */
data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val unreadCount: Int = 0,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val isLastPage: Boolean = true,
    val hasError: Boolean = false,
)

/**
 * ViewModel for managing notification screen state and business logic.
 * Fetches notifications from backend API without any hardcoded data.
 */
@HiltViewModel
class NotificationViewModel
@Inject
constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    companion object {
        private const val PAGE_SIZE = 20
    }

    init {
        loadNotifications()
    }

    /**
     * Load notifications from the first page.
     * Called on initial load and pull-to-refresh.
     */
    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, hasError = false) }

            notificationRepository.getNotifications(page = 0, size = PAGE_SIZE).fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            notifications = result.notifications,
                            isLoading = false,
                            currentPage = result.currentPage,
                            totalPages = result.totalPages,
                            isLastPage = result.isLastPage,
                            unreadCount = result.notifications.count { n -> !n.isRead },
                            hasError = false,
                        )
                    }
                },
                onFailure = { e ->
                    val errorMessage = when {
                        e.message?.contains("401") == true ||
                            e.message?.contains("Unauthorized") == true ||
                            e.message?.contains("authentication", ignoreCase = true) == true ->
                            "Silakan login terlebih dahulu untuk melihat notifikasi"
                        e.message?.contains("timeout", ignoreCase = true) == true ->
                            "Koneksi timeout. Periksa jaringan Anda"
                        e.message?.contains("Unable to resolve host") == true ||
                            e.message?.contains("No address associated") == true ->
                            "Tidak dapat terhubung ke server. Periksa koneksi internet Anda"
                        else -> e.message ?: "Gagal memuat notifikasi"
                    }
                    _uiState.update {
                        it.copy(
                            notifications = emptyList(),
                            isLoading = false,
                            error = errorMessage,
                            hasError = true,
                        )
                    }
                },
            )
        }
    }

    /**
     * Load more notifications (pagination).
     * Called when user scrolls to bottom of the list.
     */
    fun loadMoreNotifications() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || currentState.isLastPage) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            val nextPage = currentState.currentPage + 1
            notificationRepository.getNotifications(page = nextPage, size = PAGE_SIZE).fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            notifications = it.notifications + result.notifications,
                            isLoadingMore = false,
                            currentPage = result.currentPage,
                            totalPages = result.totalPages,
                            isLastPage = result.isLastPage,
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            error = e.message ?: "Gagal memuat notifikasi lainnya",
                        )
                    }
                },
            )
        }
    }

    /**
     * Fetch unread notification count for badge display.
     */
    fun fetchUnreadCount() {
        viewModelScope.launch {
            notificationRepository.getUnreadCount().fold(
                onSuccess = { count ->
                    _uiState.update { it.copy(unreadCount = count) }
                },
                onFailure = { /* Silently fail, badge just won't update */ },
            )
        }
    }

    /**
     * Mark all notifications as read.
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead().fold(
                onSuccess = {
                    // Optimistically update UI
                    _uiState.update {
                        it.copy(
                            notifications = it.notifications.map { n -> n.copy(isRead = true) },
                            unreadCount = 0,
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(error = e.message ?: "Gagal menandai semua notifikasi")
                    }
                },
            )
        }
    }

    /**
     * Mark a specific notification as read.
     * @param id Notification ID to mark as read
     */
    fun markAsRead(id: Long) {
        viewModelScope.launch {
            notificationRepository.markAsRead(id).fold(
                onSuccess = {
                    // Optimistically update UI
                    _uiState.update { state ->
                        val updatedNotifications = state.notifications.map { n ->
                            if (n.id == id) n.copy(isRead = true) else n
                        }
                        state.copy(
                            notifications = updatedNotifications,
                            unreadCount = updatedNotifications.count { !it.isRead },
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(error = e.message ?: "Gagal menandai notifikasi")
                    }
                },
            )
        }
    }

    /**
     * Clear error state after it's been shown.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
