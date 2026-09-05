package com.ecomap.socio.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.socio.data.model.Notification
import com.ecomap.socio.domain.repository.NotificationRepository
import com.ecomap.socio.utils.NotificationHelper
import com.ecomap.socio.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para notificaciones con Supabase Realtime
 * Sin Firebase - 100% Supabase
 */
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _notificationsState = MutableStateFlow<UiState<List<Notification>>>(UiState.Idle)
    val notificationsState: StateFlow<UiState<List<Notification>>> = _notificationsState.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        println("📱 NotificationViewModel: Inicializado")
        subscribeToNotifications()
        loadNotifications()
    }

    // Set para trackear IDs de notificaciones ya mostradas
    private val shownNotificationIds = mutableSetOf<String>()

    /**
     * Suscribirse a notificaciones con polling (cada 30s)
     */
    private fun subscribeToNotifications() {
        viewModelScope.launch {
            println("📡 NotificationViewModel: Iniciando suscripción con polling")
            try {
                notificationRepository.subscribeToNotifications().collect { notificationsList ->
                    println("🔔 NotificationViewModel: Notificaciones recibidas: ${notificationsList.size}")

                    // Buscar notificaciones NUEVAS que no hayamos mostrado antes
                    val newUnread = notificationsList.filter {
                        !it.read && !shownNotificationIds.contains(it.id)
                    }

                    // Mostrar solo notificaciones nuevas
                    newUnread.forEach { notification ->
                        println("🆕 NotificationViewModel: Nueva notificación detectada: ${notification.title}")
                        showLocalNotification(notification)
                        shownNotificationIds.add(notification.id)
                    }

                    _notifications.value = notificationsList
                    updateUnreadCount()
                }
            } catch (e: Exception) {
                println("❌ NotificationViewModel: Error en suscripción: ${e.message}")
            }
        }
    }

    /**
     * Cargar notificaciones desde la base de datos
     */
    fun loadNotifications() {
        viewModelScope.launch {
            println("📋 NotificationViewModel: Cargando notificaciones...")
            _notificationsState.value = UiState.Loading

            val result = notificationRepository.getNotifications()
            _notificationsState.value = result.fold(
                onSuccess = { notificationsList ->
                    println("✅ NotificationViewModel: ${notificationsList.size} notificaciones cargadas")
                    _notifications.value = notificationsList
                    updateUnreadCount()
                    UiState.Success(notificationsList)
                },
                onFailure = { error ->
                    println("❌ NotificationViewModel: Error al cargar: ${error.message}")
                    UiState.Error(error.message ?: "Error al cargar notificaciones")
                }
            )
        }
    }

    /**
     * Marcar una notificación como leída
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            println("✓ NotificationViewModel: Marcando notificación como leída: $notificationId")
            notificationRepository.markAsRead(notificationId)
            loadNotifications()
        }
    }

    /**
     * Marcar todas las notificaciones como leídas
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            println("✓ NotificationViewModel: Marcando todas como leídas")
            notificationRepository.markAllAsRead()
            loadNotifications()
        }
    }

    /**
     * Eliminar una notificación
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            println("🗑️ NotificationViewModel: Eliminando notificación: $notificationId")
            notificationRepository.deleteNotification(notificationId)
            loadNotifications()
        }
    }

    /**
     * Actualizar contador de notificaciones no leídas
     */
    private fun updateUnreadCount() {
        viewModelScope.launch {
            val result = notificationRepository.getUnreadCount()
            _unreadCount.value = result.getOrDefault(0)
            println("🔢 NotificationViewModel: Notificaciones no leídas: ${_unreadCount.value}")
        }
    }

    /**
     * Mostrar notificación local de Android
     */
    private fun showLocalNotification(notification: Notification) {
        println("🔔 NotificationViewModel: Mostrando notificación local: ${notification.title}")
        NotificationHelper.showNotification(
            context = context,
            title = notification.title,
            body = notification.body,
            notificationType = notification.type
        )
    }

    override fun onCleared() {
        super.onCleared()
        println("🔌 NotificationViewModel: Cancelando suscripción a Realtime")
        notificationRepository.unsubscribeFromNotifications()
    }
}
