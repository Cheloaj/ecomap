package com.ecomap.usuario.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    @SerialName("user_id")
    val userId: String = "",

    @SerialName("display_name")
    val displayName: String? = null,

    @SerialName("avatar_url")
    val avatarUrl: String? = null,

    @SerialName("default_latitude")
    val defaultLatitude: Double = 18.6465,

    @SerialName("default_longitude")
    val defaultLongitude: Double = -91.8323,

    @SerialName("notifications_enabled")
    val notificationsEnabled: Boolean = true,

    @SerialName("updated_at")
    val updatedAt: String = ""
)
