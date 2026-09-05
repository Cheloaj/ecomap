package com.ecomap.usuario.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.usuario.data.model.Business
import com.ecomap.usuario.data.model.Product
import com.ecomap.usuario.domain.repository.BusinessRepository
import com.ecomap.usuario.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(
        val businesses: List<Business>,
        val products: List<Product>,
        val hasMoreProducts: Boolean
    ) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val businessRepository: BusinessRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _searchState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _userLatitude = MutableStateFlow<Double?>(null)
    private val _userLongitude = MutableStateFlow<Double?>(null)

    private var currentQuery = ""
    private var currentPage = 0
    private val pageSize = 20
    private val allProducts = mutableListOf<Product>()

    fun setUserLocation(latitude: Double, longitude: Double) {
        _userLatitude.value = latitude
        _userLongitude.value = longitude
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchState.value = SearchUiState.Idle
            return
        }

        currentQuery = query
        currentPage = 0
        allProducts.clear()

        viewModelScope.launch {
            try {
                _searchState.value = SearchUiState.Loading

                // 1. Buscar negocios y ordenar por rating + distancia
                val businessesResult = businessRepository.searchBusinesses(query)
                val businesses = if (businessesResult.isSuccess) {
                    val allBusinesses = businessesResult.getOrDefault(emptyList())
                    scoreAndSortBusinesses(allBusinesses).take(3) // Top 3
                } else {
                    emptyList()
                }

                // 2. Buscar productos (primera página)
                val productsResult = productRepository.searchProducts(query, offset = 0, limit = pageSize)
                val products = productsResult.getOrDefault(emptyList())
                allProducts.addAll(products)

                _searchState.value = SearchUiState.Success(
                    businesses = businesses,
                    products = allProducts.distinctBy { it.id },  // ✅ Evitar duplicados
                    hasMoreProducts = products.size >= pageSize
                )
            } catch (e: Exception) {
                _searchState.value = SearchUiState.Error(e.message ?: "Error al buscar")
            }
        }
    }

    fun loadMoreProducts() {
        val currentState = _searchState.value
        if (currentState !is SearchUiState.Success || !currentState.hasMoreProducts) {
            return
        }

        currentPage++

        viewModelScope.launch {
            try {
                val productsResult = productRepository.searchProducts(
                    query = currentQuery,
                    offset = currentPage * pageSize,
                    limit = pageSize
                )

                val newProducts = productsResult.getOrDefault(emptyList())
                allProducts.addAll(newProducts)

                _searchState.value = SearchUiState.Success(
                    businesses = currentState.businesses,
                    products = allProducts.distinctBy { it.id },  // ✅ Evitar duplicados
                    hasMoreProducts = newProducts.size >= pageSize
                )
            } catch (e: Exception) {
                // Mantener estado actual en caso de error
                android.util.Log.e("SearchViewModel", "Error loading more: ${e.message}")
            }
        }
    }

    /**
     * Calcula un score combinado: 60% rating + 40% proximidad
     */
    private fun scoreAndSortBusinesses(businesses: List<Business>): List<Business> {
        val userLat = _userLatitude.value
        val userLon = _userLongitude.value

        // Si no tenemos ubicación del usuario, ordenar por nombre
        if (userLat == null || userLon == null) {
            return businesses.sortedBy { it.businessName }
        }

        // Calcular distancias
        val businessesWithDistance = businesses.map { business ->
            val distance = calculateDistance(
                userLat, userLon,
                business.latitude, business.longitude
            )
            business to distance
        }

        // Normalizar distancias (0-1, donde 0 = más lejano, 1 = más cercano)
        val maxDistance = businessesWithDistance.maxOfOrNull { it.second } ?: 1.0
        val normalizedBusinesses = businessesWithDistance.map { (business, distance) ->
            val normalizedDistance = if (maxDistance > 0) {
                1.0 - (distance / maxDistance)
            } else {
                1.0
            }

            // Score: 100% proximidad (no tenemos rating en Business model)
            val score = normalizedDistance

            business to score
        }

        return normalizedBusinesses
            .sortedByDescending { it.second }
            .map { it.first }
    }

    /**
     * Calcula la distancia en km usando la fórmula de Haversine
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Radio de la Tierra en km

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    fun clearSearch() {
        currentQuery = ""
        currentPage = 0
        allProducts.clear()
        _searchState.value = SearchUiState.Idle
    }
}
