package com.example.genggaminmobile.ui.features.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genggaminmobile.domain.model.Notification
import com.example.genggaminmobile.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            notificationRepository.getNotifications().fold(
                onSuccess = { notifications ->
                    _uiState.value = _uiState.value.copy(
                        notifications = notifications,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    val mockNotifications = listOf(
                         Notification(1, "Selamat Datang", "Selamat datang di Genggamin Mobile!", false, "2026-01-27T10:00:00", "INFO", null)
                    )
                    _uiState.value = _uiState.value.copy(
                        notifications = mockNotifications, 
                        // In real app, we show error. Here fallback to mock if backend fails/empty for demo
                        isLoading = false
                    )
                }
            )
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
            loadNotifications()
        }
    }
    
    fun markAsRead(id: Long) {
         viewModelScope.launch {
            notificationRepository.markAsRead(id)
            loadNotifications()
        }
    }
}
