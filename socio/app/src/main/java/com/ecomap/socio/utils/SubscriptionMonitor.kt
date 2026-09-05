package com.ecomap.socio.utils

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitor de suscripciones Pro en TIEMPO REAL
 *
 * Funcionalidad:
 * - Monitorea cambios en `users.is_pro` en tiempo real
 * - Bloquea negocios adicionales automáticamente cuando isPro cambia a false
 * - Desbloquea todos los negocios cuando isPro cambia a true
 * - Solo permite 1 negocio activo en plan FREE (el más antiguo)
 */
@Singleton
class SubscriptionMonitor @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val _isProStatus = MutableStateFlow(false)
    val isProStatus: StateFlow<Boolean> = _isProStatus.asStateFlow()

    private var currentUserId: String? = null
    private var monitoringJob: kotlinx.coroutines.Job? = null

    /**
     * Inicia el monitoreo de suscripción para un usuario
     * @param userId ID del usuario a monitorear
     * @param scope CoroutineScope donde ejecutar el monitoreo
     */
    fun startMonitoring(userId: String, scope: CoroutineScope) {
        if (currentUserId == userId && monitoringJob?.isActive == true) {
            Log.d("SubscriptionMonitor", "Ya está monitoreando al usuario $userId")
            return
        }

        stopMonitoring()
        currentUserId = userId

        Log.d("SubscriptionMonitor", "🔴 Iniciando monitoreo de suscripción para socio: $userId")

        monitoringJob = scope.launch {
            try {
                // Obtener estado inicial
                fetchCurrentStatus(userId)

                // Configurar canal de Realtime
                val channel = supabase.channel("user_subscription_$userId")

                // Escuchar cambios en la tabla users
                val changes = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                    table = "users"
                    filter = "id=eq.$userId"
                }

                // Suscribir al canal
                channel.subscribe()

                // Procesar cambios
                changes.collect { change ->
                    when (change) {
                        is PostgresAction.Update -> {
                            val newIsPro = change.record["is_pro"] as? Boolean ?: false
                            Log.d("SubscriptionMonitor", "✅ CAMBIO DETECTADO: is_pro = $newIsPro")

                            if (_isProStatus.value != newIsPro) {
                                val wasProBefore = _isProStatus.value
                                _isProStatus.value = newIsPro

                                if (wasProBefore && !newIsPro) {
                                    // PRO → FREE: Bloquear negocios adicionales
                                    Log.d("SubscriptionMonitor", "⚠️ PRO desactivado - Bloqueando negocios adicionales")
                                    blockAdditionalBusinesses(userId)
                                } else if (!wasProBefore && newIsPro) {
                                    // FREE → PRO: Activar todos los negocios
                                    Log.d("SubscriptionMonitor", "🎉 PRO activado - Desbloqueando todos los negocios")
                                    unblockAllBusinesses(userId)
                                }
                            }
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e("SubscriptionMonitor", "Error al monitorear suscripción: ${e.message}", e)
            }
        }
    }

    /**
     * Obtiene el estado inicial de suscripción desde la base de datos
     */
    private suspend fun fetchCurrentStatus(userId: String) {
        try {
            val user = supabase.from("users")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingle<JsonObject>()

            val isPro = user["is_pro"]?.jsonPrimitive?.boolean ?: false
            _isProStatus.value = isPro
            Log.d("SubscriptionMonitor", "📊 Estado inicial de suscripción: is_pro = $isPro")
        } catch (e: Exception) {
            Log.e("SubscriptionMonitor", "Error al obtener estado inicial: ${e.message}")
        }
    }

    /**
     * Bloquea todos los negocios excepto el primero (más antiguo)
     * Solo el primer negocio creado permanece activo en plan FREE
     */
    private suspend fun blockAdditionalBusinesses(userId: String) {
        try {
            // Obtener todos los negocios ordenados por fecha de creación
            val businesses = supabase.from("businesses")
                .select {
                    filter { eq("user_id", userId) }
                    order(column = "created_at", order = Order.ASCENDING)
                }
                .decodeList<JsonObject>()

            if (businesses.size <= 1) {
                Log.d("SubscriptionMonitor", "Solo tiene 1 negocio, no hay nada que bloquear")
                return
            }

            // Desactivar todos excepto el primero
            val businessesToBlock = businesses.drop(1) // Saltar el primero

            businessesToBlock.forEach { business ->
                val businessId = business["id"]?.jsonPrimitive?.content ?: return@forEach

                supabase.from("businesses")
                    .update(mapOf("is_active" to false)) {
                        filter { eq("id", businessId) }
                    }

                Log.d("SubscriptionMonitor", "🔒 Negocio bloqueado: $businessId")
            }

            Log.d("SubscriptionMonitor", "✅ ${businessesToBlock.size} negocios bloqueados")
        } catch (e: Exception) {
            Log.e("SubscriptionMonitor", "Error al bloquear negocios: ${e.message}", e)
        }
    }

    /**
     * Desbloquea todos los negocios del usuario
     * Se ejecuta cuando el usuario obtiene plan PRO
     */
    private suspend fun unblockAllBusinesses(userId: String) {
        try {
            supabase.from("businesses")
                .update(mapOf("is_active" to true)) {
                    filter { eq("user_id", userId) }
                }

            Log.d("SubscriptionMonitor", "✅ Todos los negocios desbloqueados")
        } catch (e: Exception) {
            Log.e("SubscriptionMonitor", "Error al desbloquear negocios: ${e.message}", e)
        }
    }

    /**
     * Detiene el monitoreo de suscripción
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        currentUserId = null
        Log.d("SubscriptionMonitor", "Monitoreo de suscripción detenido")
    }

    /**
     * Verifica si el usuario tiene acceso Pro
     * @return true si el usuario tiene plan Pro activo
     */
    fun hasProAccess(): Boolean = _isProStatus.value
}
