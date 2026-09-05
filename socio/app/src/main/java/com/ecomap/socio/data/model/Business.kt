package com.ecomap.socio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Business(
    @SerialName("id")
    val id: String = "",

    @SerialName("user_id")
    val userId: String = "",

    @SerialName("business_name")
    val businessName: String = "",

    @SerialName("business_type")
    val businessType: String = "", // 'abarrotes', 'carniceria', 'fruteria', etc.

    @SerialName("phone")
    val phone: String? = null,

    @SerialName("latitude")
    val latitude: Double = 0.0,

    @SerialName("longitude")
    val longitude: Double = 0.0,

    @SerialName("address")
    val address: String = "",

    @SerialName("operating_hours")
    val operatingHours: String = "", // JSON stored as string

    @SerialName("verification_status")
    val verificationStatus: String = "pending", // 'pending', 'approved', 'rejected'

    @SerialName("document_ine_url")
    val documentIneUrl: String? = null,

    @SerialName("document_proof_url")
    val documentProofUrl: String? = null,

    @SerialName("subscription_plan")
    val subscriptionPlan: String = "basic", // 'basic', 'pro'

    @SerialName("created_at")
    val createdAt: String = "",

    @SerialName("updated_at")
    val updatedAt: String = "",

    @SerialName("avatar_url")
    val avatarUrl: String? = null,

    @SerialName("approved_at")
    val approvedAt: String? = null, // Timestamp de cuándo fue aprobado

    @SerialName("approval_seen")
    val approvalSeen: Boolean = false, // Si el usuario ya vio el mensaje de aprobación

    @SerialName("is_active")
    val isActive: Boolean = true, // Si el negocio está activo o desactivado

    @SerialName("deactivated_at")
    val deactivatedAt: String? = null // Timestamp de cuándo fue desactivado
)
