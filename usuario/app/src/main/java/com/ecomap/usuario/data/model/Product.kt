package com.ecomap.usuario.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    @SerialName("id")
    val id: String = "",

    // IDs - businessId puede ser null para productos comunitarios
    @SerialName("business_id")
    val businessId: String? = null,

    @SerialName("user_id")
    val userId: String? = null, // Usuario que subió el producto (si no tiene negocio)

    @SerialName("name")
    val name: String = "",

    @SerialName("description")
    val description: String? = null,

    @SerialName("price")
    val price: Double = 0.0,

    @SerialName("image_url")
    val imageUrl: String? = null,

    @SerialName("category")
    val category: String? = null,

    @SerialName("unit")
    val unit: String? = null, // Unidad de medida: kg, litro, pieza, etc.

    @SerialName("is_available")
    val isAvailable: Boolean = true, // true = Disponible, false = Agotado

    @SerialName("stock")
    val stock: Int? = null,

    // Ubicación del producto (para productos comunitarios)
    @SerialName("latitude")
    val latitude: Double? = null,

    @SerialName("longitude")
    val longitude: Double? = null,

    // --- CAMPOS FALTANTES AGREGADOS ---
    // Estos campos arreglan los errores "Unresolved reference"

    @SerialName("location_address")
    val locationAddress: String? = null,

    @SerialName("category_name")
    val categoryName: String? = null,

    @SerialName("owner_name")
    val ownerName: String? = null,

    @SerialName("owner_phone")
    val ownerPhone: String? = null,
    // ----------------------------------

    // Campos de moderación
    @SerialName("moderation_status")
    val moderationStatus: String = "approved", // pending, approved, rejected

    @SerialName("approved_at")
    val approvedAt: String? = null,

    @SerialName("approved_by")
    val approvedBy: String? = null,

    // Campo de expiración (solo para productos comunitarios)
    @SerialName("expires_at")
    val expiresAt: String? = null,

    // Campos para programación de publicación (Pro feature)
    @SerialName("publication_status")
    val publicationStatus: String = "published", // "scheduled" | "published"

    @SerialName("scheduled_date")
    val scheduledDate: String? = null, // Formato: "YYYY-MM-DD"

    @SerialName("created_at")
    val createdAt: String = "",

    @SerialName("updated_at")
    val updatedAt: String = "",

    // ===== CAMPOS DE OFERTA =====
    @SerialName("is_on_offer")
    val isOnOffer: Boolean = false,

    @SerialName("original_price")
    val originalPrice: Double? = null,

    @SerialName("offer_type")
    val offerType: String? = null, // "2x1", "3x2", "discount", "special_price", "custom"

    @SerialName("discount_percentage")
    val discountPercentage: Int? = null,

    @SerialName("offer_description")
    val offerDescription: String? = null, // "2x1", "30% OFF", "Lleva 4 paga 3", etc.

    @SerialName("offer_valid_until")
    val offerValidUntil: String? = null // ISO date string
) {
    // Helper para saber si es un producto comunitario o de negocio
    val isCommunityProduct: Boolean
        get() = businessId == null && userId != null

    val isBusinessProduct: Boolean
        get() = businessId != null

    // Helper para saber si el producto está aprobado
    val isApproved: Boolean
        get() = moderationStatus == "approved"

    // Helper para saber si el producto está pendiente de moderación
    val isPending: Boolean
        get() = moderationStatus == "pending"

    // Helper para saber si el producto fue rechazado
    val isRejected: Boolean
        get() = moderationStatus == "rejected"

    // Helper para calcular precio con descuento
    val effectivePrice: Double
        get() = if (isOnOffer && discountPercentage != null) {
            price * (1 - discountPercentage / 100.0)
        } else {
            price
        }
}