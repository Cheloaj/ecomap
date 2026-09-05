package com.ecomap.usuario.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("id")
    val id: String = "",

    @SerialName("email")
    val email: String = "",

    @SerialName("full_name")
    val fullName: String = "",

    @SerialName("created_at")
    val createdAt: String = "",

    @SerialName("email_verified")
    val emailVerified: Boolean = false,

    @SerialName("verification_code")
    val verificationCode: String? = null,

    @SerialName("verification_code_expiry")
    val verificationCodeExpiry: String? = null,

    @SerialName("account_status")
    val accountStatus: String = "pending_verification", // 'pending_verification', 'active', 'suspended'

    @SerialName("onboarding_step")
    val onboardingStep: String = "email_verification", // 'email_verification', 'business_setup', 'document_upload', 'completed'

    @SerialName("onboarding_completed_at")
    val onboardingCompletedAt: String? = null,

    @SerialName("fcm_token")
    val fcmToken: String? = null,

    @SerialName("is_pro")
    val isPro: Boolean = false, // ✅ Plan Pro del usuario

    @SerialName("user_type")
    val userType: String = "cliente" // 'socio' (vendedor) o 'cliente' (usuario)
)
