package com.ecomap.usuario.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductReport(
    @SerialName("id")
    val id: String = "",

    @SerialName("user_id")
    val userId: String = "",

    @SerialName("business_id")
    val businessId: String = "",

    @SerialName("product_name")
    val productName: String = "",

    @SerialName("description")
    val description: String = "",

    @SerialName("estimated_price")
    val estimatedPrice: Double? = null,

    @SerialName("image_url")
    val imageUrl: String? = null,

    @SerialName("created_at")
    val createdAt: String = "",

    @SerialName("expires_at")
    val expiresAt: String = "", // created_at + 8 horas

    @SerialName("is_active")
    val isActive: Boolean = true
)
