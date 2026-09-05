
package com.ecomap.socio.data.model

import kotlinx.serialization.Serializable

@Serializable // <-- Anotación importante para la serialización
enum class ScheduledOfferStatus(val displayName: String) {
    PENDING("Pendiente"),
    PUBLISHED("Publicada"),
    CANCELLED("Cancelada"),
    FAILED("Fallida");

    companion object {
        fun fromString(value: String): ScheduledOfferStatus {
            return entries.find { it.name.lowercase() == value.lowercase() } ?: PENDING
        }
    }
}

