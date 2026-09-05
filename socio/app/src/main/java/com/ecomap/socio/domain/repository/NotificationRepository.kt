package com.ecomap.socio.domain.repository

import com.ecomap.socio.data.model.Notification
import kotlinx.coroutines.flow.Flow

/**
 * Repository para notificaciones con Supabase Realtime
 * Sin Firebase - 100% Supabase
 */
interface NotificationRepository {
    /**
     * Suscribirse a notificaciones en tiempo real del usuario autenticado
     * Usa Supabase Realtime para escuchar cambios en la tabla notifications
     */
    fun subscribeToNotifications(): Flow<List<Notification>>

    /**
     * Obtener todas las notificaciones del usuario autenticado
     */
    suspend fun getNotifications(): Result<List<Notification>>

    /**
     * Obtener solo las notificaciones no leídas
     */
    suspend fun getUnreadNotifications(): Result<List<Notification>>

    /**
     * Marcar una notificación como leída
     */
    suspend fun markAsRead(notificationId: String): Result<Unit>

    /**
     * Marcar todas las notificaciones como leídas
     */
    suspend fun markAllAsRead(): Result<Unit>

    /**
     * Obtener el contador de notificaciones no leídas
     */
    suspend fun getUnreadCount(): Result<Int>

    /**
     * Eliminar una notificación
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit>

    /**
     * Cancelar suscripción a notificaciones en tiempo real
     */
    fun unsubscribeFromNotifications()
}
