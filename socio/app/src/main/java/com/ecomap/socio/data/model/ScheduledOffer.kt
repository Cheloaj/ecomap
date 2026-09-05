package com.ecomap.socio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScheduledOffer(
    @SerialName("id") val id: String = "",
    @SerialName("business_id") val businessId: String = "",
    @SerialName("product_name") val productName: String = "",
    @SerialName("price") val price: Double = 0.0,
    @SerialName("unit") val unit: String = "kilogramo",
    @SerialName("validity_type") val validityType: String = "today",
    @SerialName("scheduled_date") val scheduledDate: String = "", // ISO 8601 date
    @SerialName("scheduled_time") val scheduledTime: String = "08:00", // HH:mm format
    @SerialName("status") val status: ScheduledOfferStatus = ScheduledOfferStatus.PENDING,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("published_offer_id") val publishedOfferId: String? = null
)

