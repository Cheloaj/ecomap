package com.ecomap.socio.data.model

/**
 * Tipos de planes de suscripción para Socios
 */
enum class SubscriptionPlan(
    val displayName: String,
    val price: Double,
    val subtotal: Double, // Precio sin IVA
    val iva: Double, // IVA 16%
    val features: List<String>
) {
    FREE(
        displayName = "Gratis",
        price = 0.0,
        subtotal = 0.0,
        iva = 0.0,
        features = listOf(
            "Hasta 10 productos",
            "Funciones básicas",
            "Visibilidad estándar"
        )
    ),
    PRO(
        displayName = "PRO",
        price = 49.99,
        subtotal = 43.09,
        iva = 6.90, // 16% de 43.09
        features = listOf(
            "Productos ilimitados",
            "Estadísticas avanzadas de ventas",
            "Reportes detallados",
            "Prioridad en búsquedas",
            "Destacado en el mapa",
            "Soporte premium"
        )
    )
}

/**
 * Estado de una suscripción
 */
data class Subscription(
    val plan: SubscriptionPlan,
    val isActive: Boolean,
    val startDate: Long, // timestamp
    val endDate: Long? = null, // timestamp
    val autoRenew: Boolean = true
)

/**
 * Información de pago (ficticia)
 */
data class PaymentInfo(
    val cardNumber: String,
    val expiryMonth: String,
    val expiryYear: String,
    val cvv: String,
    val cardHolderName: String
) {
    // Validación básica (ficticia)
    fun isValid(): Boolean {
        return cardNumber.replace(" ", "").length == 16 &&
                expiryMonth.toIntOrNull() in 1..12 &&
                expiryYear.length == 2 &&
                cvv.length == 3 &&
                cardHolderName.isNotBlank()
    }

    // Últimos 4 dígitos para mostrar
    fun getLastFourDigits(): String {
        val cleaned = cardNumber.replace(" ", "")
        return if (cleaned.length >= 4) {
            cleaned.takeLast(4)
        } else {
            "****"
        }
    }

    // Tipo de tarjeta basado en primer dígito (ficticio)
    fun getCardType(): String {
        return when (cardNumber.replace(" ", "").firstOrNull()) {
            '4' -> "Visa"
            '5' -> "Mastercard"
            '3' -> "American Express"
            else -> "Tarjeta"
        }
    }
}
