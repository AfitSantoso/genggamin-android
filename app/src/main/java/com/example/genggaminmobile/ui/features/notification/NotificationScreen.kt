package com.example.genggaminmobile.ui.features.notification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    viewModel: NotificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.example.genggaminmobile.R.string.notification_title)) },
                actions = {
                    IconButton(onClick = viewModel::markAllAsRead) {
                        Text(stringResource(com.example.genggaminmobile.R.string.notification_mark_all_read))
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(com.example.genggaminmobile.R.string.notification_empty))
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(uiState.notifications) { notification ->
                    NotificationItem(notification) {
                        viewModel.markAsRead(notification.id)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: com.example.genggaminmobile.domain.model.Notification,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
        ),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(notification.title, fontWeight = FontWeight.Bold)
                Text(notification.message, style = MaterialTheme.typography.bodyMedium)
                Text(notification.createdAt, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}
