package com.ecomap.usuario.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.usuario.data.model.*
import com.ecomap.usuario.domain.repository.BusinessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*

@HiltViewModel
class BasketOptimizerViewModel @Inject constructor(
    private val businessRepository: BusinessRepository
) : ViewModel() {

    private val _analysisState = MutableStateFlow<BasketAnalysisState>(BasketAnalysisState.Idle)
    val analysisState: StateFlow<BasketAnalysisState> = _analysisState.asStateFlow()

    private val _transportMethod = MutableStateFlow(TransportMethod.CAR)
    val transportMethod: StateFlow<TransportMethod> = _transportMethod.asStateFlow()

    /**
     * Analiza la canasta y genera el reporte completo
     */
    fun analyzeBasket(
        items: List<BasketItem>,
        userLatitude: Double,
        userLongitude: Double
    ) {
        viewModelScope.launch {
            try {
                _analysisState.value = BasketAnalysisState.Loading

                // 1. Obtener todos los negocios de los productos
                val businessIds = items.mapNotNull { it.product?.businessId }.distinct()
                val businesses = mutableMapOf<String, Business>()

                // Cargar información de cada negocio
                businessIds.forEach { businessId ->
                    businessRepository.getBusinessById(businessId).onSuccess { business ->
                        businesses[businessId] = business
                    }
                }

                // 2. Agrupar items por negocio
                val groupedItems = items.groupBy { it.product?.businessId ?: "" }

                // 3. Crear BusinessGroups con análisis
                val businessGroups = groupedItems.mapNotNull { (businessId, itemsList) ->
                    businesses[businessId]?.let { business ->
                        createBusinessGroup(
                            business = business,
                            items = itemsList,
                            userLat = userLatitude,
                            userLon = userLongitude,
                            method = _transportMethod.value
                        )
                    }
                }.sortedByDescending { it.subtotal } // Ordenar por subtotal (mayor primero)

                // 4. Calcular totales
                val totalProductsCost = businessGroups.sumOf { it.subtotal }
                val totalTransportCost = businessGroups.sumOf { it.transportCost }
                val grandTotal = totalProductsCost + totalTransportCost

                // 5. Generar sugerencia inteligente
                val suggestion = generateSmartSuggestion(
                    businessGroups = businessGroups,
                    totalProductsCost = totalProductsCost,
                    totalTransportCost = totalTransportCost
                )

                // 6. Crear análisis completo
                val analysis = BasketAnalysis(
                    businessGroups = businessGroups,
                    totalProductsCost = totalProductsCost,
                    totalTransportCost = totalTransportCost,
                    grandTotal = grandTotal,
                    transportMethod = _transportMethod.value,
                    suggestion = suggestion
                )

                _analysisState.value = BasketAnalysisState.Success(analysis)

            } catch (e: Exception) {
                _analysisState.value = BasketAnalysisState.Error(
                    e.message ?: "Error al analizar la canasta"
                )
            }
        }
    }

    /**
     * Crea un BusinessGroup con toda la información calculada
     */
    private fun createBusinessGroup(
        business: Business,
        items: List<BasketItem>,
        userLat: Double,
        userLon: Double,
        method: TransportMethod
    ): BusinessGroup {
        // Calcular subtotal de productos
        val subtotal = items.sumOf {
            (it.product?.effectivePrice ?: 0.0) * it.quantity
        }

        // Calcular distancia
        val distanceKm = calculateDistance(
            userLat, userLon,
            business.latitude, business.longitude
        )

        // Calcular costo de transporte
        val transportCost = method.calculateCost(distanceKm)

        return BusinessGroup(
            business = business,
            items = items,
            distanceKm = distanceKm,
            subtotal = subtotal,
            transportCost = transportCost,
            totalWithTransport = subtotal + transportCost,
            percentageOfTotal = 0 // Se calculará después cuando tengamos el total
        )
    }

    /**
     * Calcula la distancia entre dos puntos usando la fórmula Haversine
     * Retorna la distancia en kilómetros
     */
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusKm * c
    }

    /**
     * Genera una sugerencia inteligente basada en el análisis
     */
    private fun generateSmartSuggestion(
        businessGroups: List<BusinessGroup>,
        totalProductsCost: Double,
        totalTransportCost: Double
    ): SmartSuggestion? {
        if (businessGroups.isEmpty()) return null

        val numberOfStores = businessGroups.size
        val transportPercentage = if (totalProductsCost + totalTransportCost > 0) {
            ((totalTransportCost / (totalProductsCost + totalTransportCost)) * 100).toInt()
        } else {
            0
        }

        // Encontrar la tienda con mayor concentración
        val mainBusiness = businessGroups.maxByOrNull { it.subtotal }
        val concentration = if (totalProductsCost > 0 && mainBusiness != null) {
            ((mainBusiness.subtotal / totalProductsCost) * 100).toInt()
        } else {
            0
        }

        return when {
            // Caso 1: Una sola tienda (óptimo)
            numberOfStores == 1 -> SmartSuggestion(
                type = SuggestionType.SINGLE_STORE,
                title = "Compra Optimizada",
                message = "Tu compra está en un solo lugar. Esta es la forma más eficiente de comprar.",
                mainBusiness = mainBusiness?.business,
                concentrationPercentage = 100
            )

            // Caso 2: Costo de transporte muy alto (>20%)
            transportPercentage > 20 -> {
                val savings = totalTransportCost - (mainBusiness?.transportCost ?: 0.0)
                SmartSuggestion(
                    type = SuggestionType.HIGH_TRANSPORT_COST,
                    title = "Costo de Transporte Alto",
                    message = "Estás gastando $${String.format("%.0f", totalTransportCost)} en transporte (${transportPercentage}% de tu compra).\n\n" +
                            "El ${concentration}% de tu compra ($${String.format("%.0f", mainBusiness?.subtotal ?: 0.0)}) está en ${mainBusiness?.business?.businessName}.\n\n" +
                            "Considera comprar todo ahí para ahorrar ~$${String.format("%.0f", savings)} en transporte.",
                    potentialSavings = savings,
                    mainBusiness = mainBusiness?.business,
                    concentrationPercentage = concentration
                )
            }

            // Caso 3: Compra concentrada en una tienda (>60%)
            concentration >= 60 -> {
                val savings = totalTransportCost - (mainBusiness?.transportCost ?: 0.0)
                SmartSuggestion(
                    type = SuggestionType.CONCENTRATED_PURCHASE,
                    title = "Compra Concentrada",
                    message = "El ${concentration}% de tu compra ($${String.format("%.0f", mainBusiness?.subtotal ?: 0.0)}) está en ${mainBusiness?.business?.businessName}.\n\n" +
                            "Si compras todo ahí:\n" +
                            "• 1 viaje en lugar de $numberOfStores\n" +
                            "• Ahorras ~$${String.format("%.0f", savings)} en transporte\n" +
                            "• Ahorras tiempo de traslado\n\n" +
                            "Pregunta si tienen los otros productos que necesitas.",
                    potentialSavings = savings,
                    mainBusiness = mainBusiness?.business,
                    concentrationPercentage = concentration
                )
            }

            // Caso 4: Compra dispersa en muchas tiendas
            numberOfStores >= 3 -> {
                val avgTransportPerStore = totalTransportCost / numberOfStores
                SmartSuggestion(
                    type = SuggestionType.MULTIPLE_STORES,
                    title = "Compra en Múltiples Tiendas",
                    message = "Tu compra está dispersa en $numberOfStores tiendas diferentes.\n\n" +
                            "Estás gastando ~$${String.format("%.0f", avgTransportPerStore)} promedio por tienda en transporte.\n\n" +
                            "Considera agrupar tu compra en menos lugares para ahorrar tiempo y dinero.",
                    potentialSavings = totalTransportCost * 0.5, // Estimado
                    mainBusiness = mainBusiness?.business,
                    concentrationPercentage = concentration
                )
            }

            // Sin sugerencia específica
            else -> null
        }
    }

    /**
     * Cambia el método de transporte y recalcula
     */
    fun setTransportMethod(method: TransportMethod) {
        _transportMethod.value = method
        // Si ya hay un análisis, recalcular automáticamente
        val currentState = _analysisState.value
        if (currentState is BasketAnalysisState.Success) {
            // Trigger recalculo (necesitarás guardar los parámetros originales)
            // Por ahora solo actualizamos el método
        }
    }

    fun resetAnalysis() {
        _analysisState.value = BasketAnalysisState.Idle
    }
}

/**
 * Estados del análisis de canasta
 */
sealed class BasketAnalysisState {
    object Idle : BasketAnalysisState()
    object Loading : BasketAnalysisState()
    data class Success(val analysis: BasketAnalysis) : BasketAnalysisState()
    data class Error(val message: String) : BasketAnalysisState()
}
