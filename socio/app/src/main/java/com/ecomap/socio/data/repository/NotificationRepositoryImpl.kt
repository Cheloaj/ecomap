package com.ecomap.socio.data.repository

import com.ecomap.socio.data.model.Notification
import com.ecomap.socio.domain.repository.NotificationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Repository de notificaciones con Supabase REALTIME
 * Sin Firebase - 100% Supabase
 * Usa Realtime para notificaciones instantáneas
 */
class NotificationRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : NotificationRepository {

    private var realtimeChannel: RealtimeChannel? = null

    /**
     * Suscribirse a notificaciones usando REALTIME (instantáneo)
     * Se activa cuando se INSERTA una nueva notificación en la BD
     */
    override fun subscribeToNotifications(): Flow<List<Notification>> {
        return flow {
            val userId = supabase.auth.currentUserOrNull()?.id
            if (userId == null) {
                println("⚠️ NotificationRepository: No hay usuario autenticado")
                return@flow
            }

            println("🔔 NotificationRepository: Iniciando Realtime para usuario: $userId")

            try {
                // Crear canal de Realtime
                realtimeChannel = supabase.channel("notifications-$userId")

                // Escuchar cambios en la tabla notifications
                val changeFlow = realtimeChannel?.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "notifications"
                    filter = "user_id=eq.$userId"
                }

                realtimeChannel?.subscribe()

                // Cargar notificaciones iniciales
                emit(getNotificationsFromDb(userId))

                // Escuchar eventos de Realtime
                changeFlow?.collect { action ->
                    println("🔔 Realtime: Evento recibido: ${action::class.simpleName}")
                    when (action) {
                        is PostgresAction.Insert -> {
                            println("🆕 Nueva notificación insertada, recargando...")
                            delay(100)
                            emit(getNotificationsFromDb(userId))
                        }
                        is PostgresAction.Update -> {
                            println("🔄 Notificación actualizada, recargando...")
                            delay(100)
                            emit(getNotificationsFromDb(userId))
                        }
                        is PostgresAction.Delete -> {
                            println("🗑️ Notificación eliminada, recargando...")
                            delay(100)
                            emit(getNotificationsFromDb(userId))
                        }
                        else -> {
                            println("ℹ️ Acción Realtime no manejada: $action")
                        }
                    }
                }

                println("✅ Suscripción Realtime activa para notificaciones")
            } catch (e: Exception) {
                println("❌ Error al suscribirse a Realtime: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Obtener todas las notificaciones del usuario autenticado
     */
    override suspend fun getNotifications(): Result<List<Notification>> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val notifications = getNotificationsFromDb(userId)
            Result.success(notifications)
        } catch (e: Exception) {
            println("❌ NotificationRepository: Error al obtener notificaciones: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Obtener solo notificaciones no leídas
     */
    override suspend fun getUnreadNotifications(): Result<List<Notification>> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val notifications = supabase.from("notifications")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("read", false)
                    }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<Notification>()

            Result.success(notifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Marcar una notificación como leída
     */
    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            supabase.from("notifications")
                .update(
                    {
                        set("read", true)
                    }
                ) {
                    filter {
                        eq("id", notificationId)
                    }
                }

            println("✅ NotificationRepository: Notificación marcada como leída: $notificationId")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ NotificationRepository: Error al marcar como leída: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Marcar todas las notificaciones como leídas
     */
    override suspend fun markAllAsRead(): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Usuario no autenticado"))

            supabase.from("notifications")
                .update(
                    {
                        set("read", true)
                    }
                ) {
                    filter {
                        eq("user_id", userId)
                        eq("read", false)
                    }
                }

            println("✅ NotificationRepository: Todas las notificaciones marcadas como leídas")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ NotificationRepository: Error al marcar todas como leídas: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Obtener contador de notificaciones no leídas
     */
    override suspend fun getUnreadCount(): Result<Int> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val notifications = supabase.from("notifications")
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("user_id", userId)
                        eq("read", false)
                    }
                }
                .decodeList<Notification>()

            Result.success(notifications.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Eliminar una notificación
     */
    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            supabase.from("notifications")
                .delete {
                    filter {
                        eq("id", notificationId)
                    }
                }

            println("✅ NotificationRepository: Notificación eliminada: $notificationId")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ NotificationRepository: Error al eliminar: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Cancelar suscripción a notificaciones (no hace nada con polling)
     * El Flow se cancela automáticamente cuando se destruye el ViewModel
     */
    override fun unsubscribeFromNotifications() {
        println("✅ NotificationRepository: Suscripción de polling se cancelará con el scope")
    }

    /**
     * Helper: Obtener notificaciones desde la base de datos
     */
    private suspend fun getNotificationsFromDb(userId: String): List<Notification> {
        return supabase.from("notifications")
            .select {
                filter {
                    eq("user_id", userId)
                }
                order("created_at", order = Order.DESCENDING)
            }
            .decodeList<Notification>()
    }
}
