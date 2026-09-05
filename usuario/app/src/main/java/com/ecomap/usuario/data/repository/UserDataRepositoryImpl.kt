package com.ecomap.usuario.data.repository

import com.ecomap.usuario.data.model.*
import com.ecomap.usuario.domain.repository.UserDataRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject // <--- IMPORTANTE
import kotlinx.serialization.json.put            // <--- IMPORTANTE
import java.util.UUID
import javax.inject.Inject

class UserDataRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : UserDataRepository {

    private fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }

    // ===== FAVORITOS =====
    override suspend fun addFavorite(businessId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuario no autenticado"))

            // CORREGIDO: Usar buildJsonObject en lugar de mapOf
            val favorite = buildJsonObject {
                put("user_id", userId)
                put("business_id", businessId)
            }

            supabase.from("favorites").insert(favorite)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFavorite(businessId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuario no autenticado"))

            supabase.from("favorites")
                .delete {
                    filter {
                        eq("user_id", userId)
                        eq("business_id", businessId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isFavorite(businessId: String): Result<Boolean> {
        return try {
            val userId = getCurrentUserId() ?: return Result.success(false)

            val result = supabase.from("favorites")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("business_id", businessId)
                    }
                }

            Result.success(result.data.isNotEmpty())
        } catch (e: Exception) {
            Result.success(false)
        }
    }

    override fun getFavorites(): Flow<List<Business>> = flow {
        try {
            val userId = getCurrentUserId() ?: run {
                emit(emptyList())
                return@flow
            }

            val favorites = supabase.from("favorites")
                .select(columns = Columns.list("business_id")) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<Favorite>()

            val businessIds = favorites.map { it.businessId }

            if (businessIds.isEmpty()) {
                emit(emptyList())
                return@flow
            }

            val businesses = supabase.from("businesses")
                .select {
                    filter {
                        isIn("id", businessIds)
                    }
                }
                .decodeList<Business>()

            emit(businesses)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    // ===== HISTORIAL =====
    override suspend fun addToHistory(businessId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuario no autenticado"))

            // CORREGIDO: Usar buildJsonObject
            val history = buildJsonObject {
                put("user_id", userId)
                put("business_id", businessId)
            }

            supabase.from("user_history").insert(history)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getHistory(): Flow<List<Business>> = flow {
        try {
            val userId = getCurrentUserId() ?: run {
                emit(emptyList())
                return@flow
            }

            val history = supabase.from("user_history")
                .select(columns = Columns.list("business_id", "visited_at")) {
                    filter {
                        eq("user_id", userId)
                    }
                    limit(20)
                }
                .decodeList<UserHistory>()

            val businessIds = history.map { it.businessId }.distinct()

            if (businessIds.isEmpty()) {
                emit(emptyList())
                return@flow
            }

            val businesses = supabase.from("businesses")
                .select {
                    filter {
                        isIn("id", businessIds)
                    }
                }
                .decodeList<Business>()

            // Mantener el orden del historial
            val orderedBusinesses = businessIds.mapNotNull { id ->
                businesses.find { it.id == id }
            }

            emit(orderedBusinesses)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun clearHistory(): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuario no autenticado"))

            supabase.from("user_history")
                .delete {
                    filter {
                        eq("user_id", userId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== NOTIFICACIONES =====
    override fun getNotifications(): Flow<List<Notification>> = flow {
        try {
            val userId = getCurrentUserId() ?: run {
                emit(emptyList())
                return@flow
            }

            val notifications = supabase.from("notifications")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<Notification>()

            emit(notifications)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            supabase.from("notifications")
                .update({
                    set("read", true)
                }) {
                    filter {
                        eq("id", notificationId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            supabase.from("notifications")
                .delete {
                    filter {
                        eq("id", notificationId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getUnreadCount(): Flow<Int> = flow {
        try {
            val userId = getCurrentUserId() ?: run {
                emit(0)
                return@flow
            }

            val notifications = supabase.from("notifications")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("read", false)
                    }
                }
                .decodeList<Notification>()

            emit(notifications.size)
        } catch (e: Exception) {
            emit(0)
        }
    }

    // ===== PREFERENCIAS =====
    override suspend fun getUserPreferences(): Result<UserPreferences?> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuario no autenticado"))

            val prefs = supabase.from("user_preferences")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    limit(1)
                }
                .decodeSingleOrNull<UserPreferences>()

            Result.success(prefs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDisplayName(displayName: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuario no autenticado"))
            android.util.Log.d("UserDataRepo", "Actualizando nombre para userId: $userId a: $displayName")

            // Intentar actualizar primero
            val existing = supabase.from("user_preferences")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    limit(1)
                }
                .decodeSingleOrNull<UserPreferences>()

            android.util.Log.d("UserDataRepo", "Registro existente: $existing")

            if (existing == null) {
                // Crear nuevo registro
                android.util.Log.d("UserDataRepo", "Creando nuevo registro de preferencias")

                // CORREGIDO: Usar buildJsonObject en lugar de mapOf
                val newPrefs = buildJsonObject {
                    put("user_id", userId)
                    put("display_name", displayName)
                    put("notifications_enabled", true)
                }

                supabase.from("user_preferences").insert(newPrefs)
            } else {
                // Actualizar existente
                android.util.Log.d("UserDataRepo", "Actualizando registro existente")
                supabase.from("user_preferences")
                    .update({
                        set("display_name", displayName)
                    }) {
                        filter {
                            eq("user_id", userId)
                        }
                    }
            }

            android.util.Log.d("UserDataRepo", "Actualización exitosa")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("UserDataRepo", "Error al actualizar: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun uploadAvatar(imageBytes: ByteArray): Result<String> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuario no autenticado"))
            android.util.Log.d("UserDataRepo", "Subiendo avatar para userId: $userId")

            // Generar nombre único para el archivo
            val fileName = "${userId}_${UUID.randomUUID()}.jpg"

            // Subir imagen a Supabase Storage
            val bucket = supabase.storage.from("avatars")

            // Eliminar avatar anterior si existe
            try {
                val existing = supabase.from("user_preferences")
                    .select {
                        filter { eq("user_id", userId) }
                        limit(1)
                    }
                    .decodeSingleOrNull<UserPreferences>()

                existing?.avatarUrl?.let { oldUrl ->
                    val oldFileName = oldUrl.substringAfterLast("/")
                    try {
                        bucket.delete(oldFileName)
                        android.util.Log.d("UserDataRepo", "Avatar anterior eliminado: $oldFileName")
                    } catch (e: Exception) {
                        android.util.Log.w("UserDataRepo", "No se pudo eliminar avatar anterior: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("UserDataRepo", "Error verificando avatar anterior: ${e.message}")
            }

            // Subir nueva imagen
            bucket.upload(fileName, imageBytes)
            android.util.Log.d("UserDataRepo", "Avatar subido: $fileName")

            // Obtener URL pública
            val avatarUrl = bucket.publicUrl(fileName)
            android.util.Log.d("UserDataRepo", "URL pública generada: $avatarUrl")

            // Actualizar user_preferences con la nueva URL
            val existingPrefs = supabase.from("user_preferences")
                .select {
                    filter { eq("user_id", userId) }
                    limit(1)
                }
                .decodeSingleOrNull<UserPreferences>()

            if (existingPrefs == null) {
                // Crear nuevo registro
                android.util.Log.d("UserDataRepo", "Creando nuevo registro de preferencias con avatar")

                // CORREGIDO: Usar buildJsonObject en lugar de mapOf
                val newPrefs = buildJsonObject {
                    put("user_id", userId)
                    put("avatar_url", avatarUrl)
                    put("notifications_enabled", true)
                }

                supabase.from("user_preferences").insert(newPrefs)
            } else {
                // Actualizar existente
                android.util.Log.d("UserDataRepo", "Actualizando avatar_url")
                supabase.from("user_preferences")
                    .update({
                        set("avatar_url", avatarUrl)
                    }) {
                        filter { eq("user_id", userId) }
                    }
            }

            android.util.Log.d("UserDataRepo", "Avatar actualizado exitosamente")
            Result.success(avatarUrl)
        } catch (e: Exception) {
            android.util.Log.e("UserDataRepo", "Error al subir avatar: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateDefaultLocation(latitude: Double, longitude: Double): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuario no autenticado"))

            supabase.from("user_preferences")
                .update({
                    set("default_latitude", latitude)
                    set("default_longitude", longitude)
                }) {
                    filter {
                        eq("user_id", userId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateNotificationsEnabled(enabled: Boolean): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuario no autenticado"))

            supabase.from("user_preferences")
                .update({
                    set("notifications_enabled", enabled)
                }) {
                    filter {
                        eq("user_id", userId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}