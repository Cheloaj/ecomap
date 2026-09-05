package com.ecomap.usuario.domain.repository

import com.ecomap.usuario.data.model.*
import kotlinx.coroutines.flow.Flow

interface UserDataRepository {
    // Favoritos
    suspend fun addFavorite(businessId: String): Result<Unit>
    suspend fun removeFavorite(businessId: String): Result<Unit>
    suspend fun isFavorite(businessId: String): Result<Boolean>
    fun getFavorites(): Flow<List<Business>>

    // Historial
    suspend fun addToHistory(businessId: String): Result<Unit>
    fun getHistory(): Flow<List<Business>>
    suspend fun clearHistory(): Result<Unit>

    // Notificaciones
    fun getNotifications(): Flow<List<Notification>>
    suspend fun markAsRead(notificationId: String): Result<Unit>
    suspend fun deleteNotification(notificationId: String): Result<Unit>
    fun getUnreadCount(): Flow<Int>

    // Preferencias de usuario
    suspend fun getUserPreferences(): Result<UserPreferences?>
    suspend fun updateDisplayName(displayName: String): Result<Unit>
    suspend fun uploadAvatar(imageBytes: ByteArray): Result<String>
    suspend fun updateDefaultLocation(latitude: Double, longitude: Double): Result<Unit>
    suspend fun updateNotificationsEnabled(enabled: Boolean): Result<Unit>
}
