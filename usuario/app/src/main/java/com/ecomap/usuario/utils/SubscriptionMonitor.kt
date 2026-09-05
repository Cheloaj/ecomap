package com.ecomap.usuario.utils

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitor de suscripciones en tiempo real.
 * Escucha cambios en el campo is_pro de la tabla users y actualiza el estado inmediatamente.
 *
 * Características principales:
 * - Actualización en tiempo real cuando el admin cambia is_pro desde el panel
 * - Bloqueo automático de funciones Pro si is_pro cambia a false
 * - Activación automática de funciones Pro si is_pro cambia a true
 * - Sin necesidad de recargar la app o hacer ninguna acción del usuario
 */
@Singleton
class SubscriptionMonitor @Inject constructor(
    private val supabase: SupabaseClient
) {
    companion object {
        private const val TAG = "SubscriptionMonitor"
    }

    private val _isProStatus = MutableStateFlow(false)
    val isProStatus: StateFlow<Boolean> = _isProStatus.asStateFlow()

    private var currentUserId: String? = null
    private var monitoringJob: Job? = null
    private var channelName: String? = null

    /**
     * Inicia el monitoreo en tiempo real del estado de suscripción del usuario.
     *
     * @param userId ID del usuario a monitorear
     * @param scope CoroutineScope para las operaciones asíncronas (generalmente viewModelScope)
     */
    fun startMonitoring(userId: String, scope: CoroutineScope) {
        if (currentUserId == userId && monitoringJob?.isActive == true) {
            Log.d(TAG, "Ya está monitoreando al usuario $userId")
            return
        }

        stopMonitoring()
        currentUserId = userId
        channelName = "user_subscription_$userId"

        Log.d(TAG, "🔴 Iniciando monitoreo de suscripción para usuario: $userId")

        monitoringJob = scope.launch {
            try {
                // 1. Obtener estado inicial de is_pro
                fetchCurrentStatus(userId)

                // 2. Suscribirse a cambios en tiempo real usando Realtime
                val channel = supabase.channel(channelName!!)

                val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "users"
                    filter = "id=eq.$userId"
                }

                // 3. Suscribirse al canal antes de escuchar cambios
                channel.subscribe()
                Log.d(TAG, "✅ Canal de Realtime suscrito: $channelName")

                // 4. Escuchar todos los cambios (UPDATE principalmente)
                changes.collect { change ->
                    when (change) {
                        is PostgresAction.Update -> {
                            // Extraer el nuevo valor de is_pro del registro actualizado
                            val record = change.record
                            val newIsPro = record["is_pro"]?.jsonPrimitive?.content?.toBoolean() ?: false

                            Log.d(TAG, "🔔 CAMBIO DETECTADO en Realtime: is_pro = $newIsPro")

                            // Solo actualizar si hay un cambio real
                            if (_isProStatus.value != newIsPro) {
                                _isProStatus.value = newIsPro

                                // Actualizar también la caché local
                                com.ecomap.usuario.data.local.UserSession.setProStatus(newIsPro)

                                Log.d(TAG, if (newIsPro) {
                                    "🎉 Suscripción PRO ACTIVADA en tiempo real"
                                } else {
                                    "⚠️ Suscripción PRO DESACTIVADA en tiempo real - Funciones bloqueadas"
                                })
                            }
                        }
                        is PostgresAction.Insert -> {
                            Log.d(TAG, "INSERT detectado (no debería ocurrir para usuario existente)")
                        }
                        is PostgresAction.Delete -> {
                            Log.w(TAG, "DELETE detectado - Usuario eliminado")
                            _isProStatus.value = false
                        }
                        else -> {
                            Log.d(TAG, "Otro tipo de cambio detectado: ${change::class.simpleName}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al monitorear suscripción: ${e.message}", e)
                // En caso de error, intentar obtener el estado actual como fallback
                try {
                    fetchCurrentStatus(userId)
                } catch (fallbackError: Exception) {
                    Log.e(TAG, "Error en fallback: ${fallbackError.message}")
                }
            }
        }
    }

    /**
     * Obtiene el estado actual de la suscripción desde la base de datos.
     * Se usa para el estado inicial y como fallback en caso de error.
     */
    private suspend fun fetchCurrentStatus(userId: String) {
        try {
            val response = supabase.from("users")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<kotlinx.serialization.json.JsonObject>()

            val isPro = response["is_pro"]?.jsonPrimitive?.content?.toBoolean() ?: false
            _isProStatus.value = isPro

            // Actualizar también la caché local
            com.ecomap.usuario.data.local.UserSession.setProStatus(isPro)

            Log.d(TAG, "📊 Estado inicial de suscripción: is_pro = $isPro")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener estado inicial: ${e.message}", e)
            // En caso de error, usar el valor de la caché local como fallback
            val cachedStatus = com.ecomap.usuario.data.local.UserSession.isProUser.value ?: false
            _isProStatus.value = cachedStatus
            Log.d(TAG, "Usando estado cacheado: is_pro = $cachedStatus")
        }
    }

    /**
     * Detiene el monitoreo en tiempo real.
     * Se debe llamar cuando el usuario cierra sesión.
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null

        // Limpiar el canal de Realtime si existe
        if (channelName != null) {
            // El canal se limpiará automáticamente cuando se cancele el job
            Log.d(TAG, "Canal de Realtime limpiado: $channelName")
            channelName = null
        }

        currentUserId = null
        _isProStatus.value = false
        Log.d(TAG, "⏹️ Monitoreo de suscripción detenido")
    }

    /**
     * Verifica si el usuario tiene acceso a funciones Pro.
     * Usa esta función en todas las pantallas que requieren suscripción Pro.
     *
     * @return true si el usuario tiene suscripción Pro activa, false en caso contrario
     */
    fun hasProAccess(): Boolean {
        val hasPro = _isProStatus.value
        Log.d(TAG, "Verificación de acceso Pro: $hasPro")
        return hasPro
    }

    /**
     * Fuerza una actualización del estado de suscripción.
     * Útil para refrescar el estado después de un pago o cambio manual.
     */
    suspend fun refreshStatus() {
        currentUserId?.let { userId ->
            Log.d(TAG, "🔄 Refrescando estado de suscripción...")
            fetchCurrentStatus(userId)
        } ?: Log.w(TAG, "No se puede refrescar: usuario no inicializado")
    }
}
