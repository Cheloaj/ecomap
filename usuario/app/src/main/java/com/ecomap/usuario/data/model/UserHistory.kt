package com.ecomap.usuario.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserHistory(
    @SerialName("id")
    val id: String = "",

    @SerialName("user_id")
    val userId: String = "",

    @SerialName("business_id")
    val businessId: String = "",

    @SerialName("visited_at")
    val visitedAt: String = ""
)
