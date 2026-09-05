package com.ecomap.socio.presentation.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.socio.data.model.Notification
import com.ecomap.socio.data.model.NotificationType
import com.ecomap.socio.presentation.viewmodel.NotificationViewModel
import com.ecomap.socio.utils.UiState
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val notificationsState by viewModel.notificationsState.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    // Local filter state
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    val filteredNotifications = remember(notifications, selectedFilter) {
        if (selectedFilter == null) {
            notifications
        } else {
            notifications.filter { it.type == selectedFilter }
        }
    }

    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Notificaciones")
                        if (unreadCount > 0) {
                            Text(
                                text = "$unreadCount sin leer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(onClick = { viewModel.markAllAsRead() }) {
                            Text("Marcar todas como leídas")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when (notificationsState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = (notificationsState as UiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.loadNotifications() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Filter chips - SIMPLIFICADO
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilter == null,
                                onClick = { selectedFilter = null },
                                label = { Text("Todas") }
                            )
                        }

                        item {
                            FilterChip(
                                selected = selectedFilter == NotificationType.ACCOUNT_APPROVED,
                                onClick = { selectedFilter = NotificationType.ACCOUNT_APPROVED },
                                label = { Text("Aprobadas") }
                            )
                        }

                        item {
                            FilterChip(
                                selected = selectedFilter == NotificationType.ACCOUNT_REJECTED,
                                onClick = { selectedFilter = NotificationType.ACCOUNT_REJECTED },
                                label = { Text("Rechazadas") }
                            )
                        }

                        item {
                            FilterChip(
                                selected = selectedFilter == NotificationType.GENERAL,
                                onClick = { selectedFilter = NotificationType.GENERAL },
                                label = { Text("General") }
                            )
                        }
                    }

                    HorizontalDivider()

                    // Notifications list
                    if (filteredNotifications.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                                Text(
                                    text = if (selectedFilter == null) "No hay notificaciones"
                                    else "No hay notificaciones de este tipo",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        LazyColumn {
                            items(filteredNotifications, key = { it.id }) { notification ->
                                NotificationItem(
                                    notification = notification,
                                    onMarkAsRead = { viewModel.markAsRead(it) },
                                    onDelete = { showDeleteDialog = it }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { notificationId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Eliminar Notificación") },
            text = { Text("¿Estás seguro de que deseas eliminar esta notificación?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNotification(notificationId)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onMarkAsRead: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val backgroundColor = if (!notification.read) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable {
                if (!notification.read) {
                    onMarkAsRead(notification.id)
                }
            }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(getNotificationColor(notification.type).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getNotificationIcon(notification.type),
                contentDescription = notification.type,
                tint = getNotificationColor(notification.type),
                modifier = Modifier.size(24.dp)
            )
        }

        // Content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (!notification.read) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (!notification.read) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Text(
                text = notification.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatNotificationDate(notification.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Chip(
                    label = { Text(notification.type) },
                    colors = ChipDefaults.chipColors(
                        containerColor = getNotificationColor(notification.type).copy(alpha = 0.2f),
                        labelColor = getNotificationColor(notification.type)
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }
        }

        // Delete button
        IconButton(
            onClick = { onDelete(notification.id) },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Eliminar",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun Chip(
    label: @Composable () -> Unit,
    colors: ChipColors,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = colors.containerColor
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                label()
            }
        }
    }
}

data class ChipColors(
    val containerColor: androidx.compose.ui.graphics.Color,
    val labelColor: androidx.compose.ui.graphics.Color
)

object ChipDefaults {
    @Composable
    fun chipColors(
        containerColor: androidx.compose.ui.graphics.Color,
        labelColor: androidx.compose.ui.graphics.Color
    ) = ChipColors(containerColor, labelColor)
}

@Composable
fun getNotificationIcon(type: String): ImageVector {
    return when (type) {
        NotificationType.ACCOUNT_APPROVED -> Icons.Default.CheckCircle
        NotificationType.ACCOUNT_REJECTED -> Icons.Default.Warning
        NotificationType.TEST -> Icons.Default.Notifications
        else -> Icons.Default.Notifications
    }
}

@Composable
fun getNotificationColor(type: String): androidx.compose.ui.graphics.Color {
    return when (type) {
        NotificationType.ACCOUNT_APPROVED -> MaterialTheme.colorScheme.tertiary
        NotificationType.ACCOUNT_REJECTED -> MaterialTheme.colorScheme.error
        NotificationType.TEST -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }
}

fun formatNotificationDate(dateString: String): String {
    return try {
        val instant = Instant.parse(dateString)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val now = kotlinx.datetime.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        when {
            localDateTime.date == now.date -> {
                // Today - show time
                String.format("%02d:%02d", localDateTime.hour, localDateTime.minute)
            }
            localDateTime.date.dayOfYear == now.date.dayOfYear - 1 -> {
                // Yesterday
                "Ayer"
            }
            else -> {
                // Other dates
                "${localDateTime.dayOfMonth}/${localDateTime.monthNumber}/${localDateTime.year}"
            }
        }
    } catch (e: Exception) {
        dateString
    }
}
