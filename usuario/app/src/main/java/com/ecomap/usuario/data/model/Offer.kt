package com.ecomap.usuario.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Offer(
    @SerialName("id")
    val id: String = "",

    @SerialName("business_id")
    val businessId: String = "",

    @SerialName("owner_id")
    val ownerId: String = "",

    @SerialName("product_name")
    val productName: String = "",

    @SerialName("price")
    val price: Double = 0.0,

    @SerialName("unit")
    val unit: String = "",

    @SerialName("validity_type")
    val validityType: String = "today",

    @SerialName("valid_until")
    val validUntil: String? = null,

    @SerialName("is_active")
    val isActive: Boolean = true,

    @SerialName("is_out_of_stock")
    val isOutOfStock: Boolean = false,

    @SerialName("created_at")
    val createdAt: String = "",

    @SerialName("updated_at")
    val updatedAt: String = "",

    @SerialName("view_count")
    val viewCount: Int = 0,

    @SerialName("report_count")
    val reportCount: Int = 0,

    @SerialName("confirmation_count")
    val confirmationCount: Int = 0,

    // Campos de promociones
    @SerialName("title")
    val title: String? = null,

    @SerialName("description")
    val description: String? = null,

    @SerialName("discount_percentage")
    val discountPercentage: Int? = null,

    @SerialName("original_price")
    val originalPrice: Double? = null,

    @SerialName("discounted_price")
    val discountedPrice: Double? = null,

    @SerialName("start_date")
    val startDate: String? = null,

    @SerialName("end_date")
    val endDate: String? = null,

    @SerialName("image_url")
    val imageUrl: String? = null,

    // Relación con el negocio
    @SerialName("business")
    val business: Business? = null
) {
    /**
     * Convierte una Oferta a un Producto para mostrar en ProductDetailScreen
     */
    fun toProduct(): Product {
        return Product(
            id = this.id,
            businessId = this.businessId,
            userId = this.ownerId,
            name = this.productName,
            description = this.description,
            price = this.discountedPrice ?: this.price,
            imageUrl = this.imageUrl,
            category = null,
            unit = this.unit,
            isAvailable = this.isActive && !this.isOutOfStock,
            stock = null,
            latitude = null,
            longitude = null,
            locationAddress = null,
            categoryName = null,
            ownerName = null,
            ownerPhone = null,
            moderationStatus = "approved",
            approvedAt = null,
            approvedBy = null,
            expiresAt = this.endDate,
            publicationStatus = "published",
            scheduledDate = null,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            // Campos de oferta
            isOnOffer = true,
            originalPrice = this.originalPrice,
            offerType = this.title,
            offerDescription = this.description,
            discountPercentage = this.discountPercentage,
            offerValidUntil = this.endDate
        )
    }
}
