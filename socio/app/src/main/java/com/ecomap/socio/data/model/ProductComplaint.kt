package com.ecomap.socio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductComplaint(
    @SerialName("id")
    val id: String = "",

    @SerialName("user_id")
    val userId: String = "",

    @SerialName("product_id")
    val productId: String = "",

    @SerialName("reason")
    val reason: String = "",

    @SerialName("description")
    val description: String? = null,

    @SerialName("image_url")
    val imageUrl: String? = null,

    @SerialName("status")
    val status: String = "pending", // pending, resolved, dismissed

    @SerialName("admin_approved")
    val adminApproved: Boolean = false, // Si admin aprobó enviarlo al vendedor

    @SerialName("created_at")
    val createdAt: String = "",

    @SerialName("updated_at")
    val updatedAt: String = "",

    @SerialName("vendor_response")
    val vendorResponse: String? = null,

    @SerialName("vendor_response_at")
    val vendorResponseAt: String? = null,

    @SerialName("vendor_user_id")
    val vendorUserId: String? = null,

    // Campos relacionados (joined data)
    val userName: String? = null,
    val productName: String? = null,
    val productImageUrl: String? = null
)
