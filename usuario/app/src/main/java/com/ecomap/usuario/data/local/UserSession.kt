package com.ecomap.usuario.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 🎯 Singleton para cachear información del usuario en memoria
 *
 * Beneficios:
 * - Elimina consultas repetidas a Supabase
 * - Respuesta instantánea (0ms vs 300-500ms)
 * - Reduce uso de red y batería
 *
 * Se actualiza:
 * - Al iniciar sesión
 * - Al cambiar de plan (gratuito ↔ PRO)
 * - Cada 5 minutos en segundo plano (opcional)
 */
object UserSession {
    // Estado PRO del usuario
    private val _isProUser = MutableStateFlow<Boolean?>(null)
    val isProUser: StateFlow<Boolean?> = _isProUser.asStateFlow()

    // ID del usuario actual
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    // Timestamp de última verificación
    private var lastProCheck: Long = 0
    private const val CACHE_VALIDITY_MS = 5 * 60 * 1000L // 5 minutos

    /**
     * Actualizar estado PRO
     */
    fun setProStatus(isPro: Boolean) {
        _isProUser.value = isPro
        lastProCheck = System.currentTimeMillis()
    }

    /**
     * Actualizar ID de usuario actual
     */
    fun setCurrentUserId(userId: String?) {
        _currentUserId.value = userId
    }

    /**
     * Verificar si el caché es válido
     */
    fun isCacheValid(): Boolean {
        return (System.currentTimeMillis() - lastProCheck) < CACHE_VALIDITY_MS
    }

    /**
     * Limpiar sesión (logout)
     */
    fun clear() {
        _isProUser.value = null
        _currentUserId.value = null
        lastProCheck = 0
    }

    /**
     * Obtener estado PRO (con validación de caché)
     */
    fun getProStatus(): Boolean? {
        return if (isCacheValid()) _isProUser.value else null
    }
}
