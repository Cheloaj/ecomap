package com.ecomap.socio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductRating(
    @SerialName("id")
    val id: String = "",

    @SerialName("user_id")
    val userId: String = "",

    @SerialName("product_id")
    val productId: String = "",

    @SerialName("rating")
    val rating: Int = 0,

    @SerialName("comment")
    val comment: String? = null,

    @SerialName("created_at")
    val createdAt: String = "",

    @SerialName("updated_at")
    val updatedAt: String = "",

    // Vendor response fields
    @SerialName("vendor_response")
    val vendorResponse: String? = null,

    @SerialName("vendor_response_at")
    val vendorResponseAt: String? = null,

    @SerialName("vendor_user_id")
    val vendorUserId: String? = null,

    // User info for display
    @SerialName("user_name")
    val userName: String? = null
) {
    // Helper to check if vendor has responded
    val hasVendorResponse: Boolean
        get() = !vendorResponse.isNullOrBlank()

    // Helper to get display name for user
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
}

@Serializable
data class ProductRatingStats(
    @SerialName("product_id")
    val productId: String = "",

    @SerialName("total_ratings")
    val totalRatings: Int = 0,

    @SerialName("average_rating")
    val averageRating: Double = 0.0,

    @SerialName("five_stars")
    val fiveStars: Int = 0,

    @SerialName("four_stars")
    val fourStars: Int = 0,

    @SerialName("three_stars")
    val threeStars: Int = 0,

    @SerialName("two_stars")
    val twoStars: Int = 0,

    @SerialName("one_star")
    val oneStar: Int = 0
)
