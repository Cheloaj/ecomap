package com.ecomap.usuario.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Notification(
    @SerialName("id") val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("body") val body: String = "",
    @SerialName("type") val type: String = "general",
    @SerialName("data") val data: JsonObject? = null, // Datos adicionales como JSON
    @SerialName("read") val read: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

// Tipos de notificaciones
object NotificationType {
    const val ACCOUNT_APPROVED = "account_approved"
    const val ACCOUNT_REJECTED = "account_rejected"
    const val GENERAL = "general"
    const val TEST = "test"
}
