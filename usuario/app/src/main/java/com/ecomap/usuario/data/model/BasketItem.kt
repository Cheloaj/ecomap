package com.ecomap.usuario.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BasketItem(
    @SerialName("id")
    val id: String = "",

    @SerialName("user_id")
    val userId: String = "",

    @SerialName("product_id")
    val productId: String = "",

    @SerialName("quantity")
    val quantity: Int = 1,

    @SerialName("created_at")
    val createdAt: String = "",

    @SerialName("updated_at")
    val updatedAt: String = "",

    // Campos del producto (joined)
    val product: Product? = null
)

// Para insertar
@Serializable
data class BasketItemInsert(
    @SerialName("user_id")
    val userId: String,

    @SerialName("product_id")
    val productId: String,

    @SerialName("quantity")
    val quantity: Int = 1
)

// Para actualizar cantidad
@Serializable
data class BasketItemUpdate(
    @SerialName("quantity")
    val quantity: Int
)
