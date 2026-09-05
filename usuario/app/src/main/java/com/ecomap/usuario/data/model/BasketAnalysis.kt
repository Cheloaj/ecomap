package com.ecomap.usuario.data.model

import kotlinx.serialization.Serializable

/**
 * Métodos de transporte disponibles para cálculo de costos
 */
enum class TransportMethod(
    val displayName: String,
    val costPerKm: Double, // Costo por kilómetro
    val fixedCost: Double = 0.0 // Costo fijo por viaje (para transporte público)
) {
    CAR("Carro", costPerKm = 10.0),
    PUBLIC_TRANSPORT("Transporte público", costPerKm = 0.0, fixedCost = 20.0),
    BIKE("Bicicleta", costPerKm = 0.0),
    WALKING("A pie", costPerKm = 0.0);

    fun calculateCost(distanceKm: Double): Double {
        return if (fixedCost > 0) {
            fixedCost // Transporte público: costo fijo por viaje
        } else {
            distanceKm * costPerKm
        }
    }
}

/**
 * Tipos de sugerencias inteligentes
 */
enum class SuggestionType {
    HIGH_TRANSPORT_COST,     // Costo de transporte muy alto
    CONCENTRATED_PURCHASE,   // Compra concentrada en una tienda
    MULTIPLE_STORES,         // Compra dispersa en muchas tiendas
    SINGLE_STORE,            // Toda la compra en una sola tienda (óptimo)
    NO_SUGGESTION            // Sin sugerencia específica
}

/**
 * Agrupación de items por negocio
 */
data class BusinessGroup(
    val business: Business,
    val items: List<BasketItem>,
    val distanceKm: Double,
    val subtotal: Double,
    val transportCost: Double,
    val totalWithTransport: Double,
    val percentageOfTotal: Int // Porcentaje del total de la compra
)

/**
 * Sugerencia inteligente basada en el análisis
 */
data class SmartSuggestion(
    val type: SuggestionType,
    val title: String,
    val message: String,
    val potentialSavings: Double? = null,
    val mainBusiness: Business? = null,
    val concentrationPercentage: Int? = null
)

/**
 * Análisis completo de la canasta
 */
data class BasketAnalysis(
    val businessGroups: List<BusinessGroup>,
    val totalProductsCost: Double,
    val totalTransportCost: Double,
    val grandTotal: Double,
    val transportMethod: TransportMethod,
    val suggestion: SmartSuggestion?
) {
    val numberOfStores: Int
        get() = businessGroups.size

    val transportPercentage: Int
        get() = if (grandTotal > 0) {
            ((totalTransportCost / grandTotal) * 100).toInt()
        } else {
            0
        }

    val mainBusiness: BusinessGroup?
        get() = businessGroups.maxByOrNull { it.subtotal }
}
