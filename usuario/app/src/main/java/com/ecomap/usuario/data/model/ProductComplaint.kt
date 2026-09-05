package com.ecomap.usuario.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo para reportes/quejas de productos existentes
 * (No confundir con ProductReport que es para reportar productos faltantes)
 */
@Serializable
data class ProductComplaint(
    @SerialName("id")
    val id: String = "",

    @SerialName("user_id")
    val userId: String = "",

    @SerialName("product_id")
    val productId: String = "",

    @SerialName("reason")
    val reason: String = "", // Motivo del reporte

    @SerialName("description")
    val description: String? = null, // Descripción detallada (opcional)

    @SerialName("image_url")
    val imageUrl: String? = null, // Foto de evidencia (opcional)

    @SerialName("status")
    val status: String = "pending", // pending, reviewed, resolved

    @SerialName("admin_approved")
    val adminApproved: Boolean = false, // Si admin aprobó enviarlo al vendedor

    @SerialName("created_at")
    val createdAt: String = "",

    @SerialName("updated_at")
    val updatedAt: String = "",

    // Vendor response fields (solo lectura para usuario)
    @SerialName("vendor_response")
    val vendorResponse: String? = null,

    @SerialName("vendor_response_at")
    val vendorResponseAt: String? = null,

    @SerialName("vendor_user_id")
    val vendorUserId: String? = null,

    // Información del usuario (para mostrar en la UI del vendedor)
    @SerialName("user_name")
    val userName: String? = null,

    @SerialName("user_avatar_url")
    val userAvatarUrl: String? = null,

    // Información del producto (para mostrar en listas)
    @SerialName("product_name")
    val productName: String? = null,

    @SerialName("product_image_url")
    val productImageUrl: String? = null
) {
    // Helper to check if vendor has responded
    val hasVendorResponse: Boolean
        get() = !vendorResponse.isNullOrBlank()

    // Helper para display name abreviado
    val displayName: String
        get() {
            if (userName.isNullOrBlank()) return "Usuario"

            val parts = userName.trim().split(" ")
            return when {
                parts.size >= 2 -> "${parts[0]} ${parts[1].first()}."
                parts.size == 1 -> parts[0]
                else -> "Usuario"
            }
        }

    // Helper para verificar si el usuario puede editar/eliminar
    fun canBeEditedBy(currentUserId: String): Boolean {
        return userId == currentUserId && status == "pending"
    }
}

/**
 * Tipos de motivos de reporte predefinidos
 */
enum class ComplaintReason(val displayName: String) {
    WRONG_PRICE("Precio incorrecto"),
    BAD_QUALITY("Mala calidad del producto"),
    WRONG_DESCRIPTION("Descripción incorrecta"),
    EXPIRED_PRODUCT("Producto vencido o en mal estado"),
    FALSE_ADVERTISING("Publicidad engañosa"),
    NOT_AVAILABLE("Producto no disponible"),
    OTHER("Otro motivo")
}
