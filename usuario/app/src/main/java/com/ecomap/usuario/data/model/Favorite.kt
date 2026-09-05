package com.ecomap.usuario.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Favorite(
    @SerialName("id")
    val id: String = "",

    @SerialName("user_id")
    val userId: String = "",

    @SerialName("business_id")
    val businessId: String = "",

    @SerialName("created_at")
    val createdAt: String = ""
)
