package com.ecomap.socio.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.socio.data.model.ProductRating
import com.ecomap.socio.domain.repository.RatingRepository
import com.ecomap.socio.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RatingViewModel @Inject constructor(
    private val ratingRepository: RatingRepository
) : ViewModel() {

    private val _productRatings = MutableStateFlow<List<ProductRating>>(emptyList())
    val productRatings: StateFlow<List<ProductRating>> = _productRatings.asStateFlow()

    private val _ratingsState = MutableStateFlow<UiState<List<ProductRating>>>(UiState.Idle)
    val ratingsState: StateFlow<UiState<List<ProductRating>>> = _ratingsState.asStateFlow()

    private val _vendorResponseState = MutableStateFlow<UiState<ProductRating>>(UiState.Idle)
    val vendorResponseState: StateFlow<UiState<ProductRating>> = _vendorResponseState.asStateFlow()

    private val _averageRating = MutableStateFlow(0.0)
    val averageRating: StateFlow<Double> = _averageRating.asStateFlow()

    private val _totalRatings = MutableStateFlow(0)
    val totalRatings: StateFlow<Int> = _totalRatings.asStateFlow()

    /**
     * Load all ratings for a specific product
     */
    fun loadProductRatings(productId: String) {
        viewModelScope.launch {
            _ratingsState.value = UiState.Loading
            println("⭐ Cargando reseñas del producto: $productId")

            val result = ratingRepository.getProductRatings(productId)
            result.fold(
                onSuccess = { ratings ->
                    _productRatings.value = ratings
                    _ratingsState.value = UiState.Success(ratings)
                    println("✅ ${ratings.size} reseñas cargadas")
                },
                onFailure = { error ->
                    println("❌ Error al cargar reseñas: ${error.message}")
                    _ratingsState.value = UiState.Error(error.message ?: "Error al cargar reseñas")
                }
            )
        }
    }

    /**
     * Load rating statistics for a product
     */
    fun loadRatingStats(productId: String) {
        viewModelScope.launch {
            // Load average rating
            ratingRepository.getAverageRating(productId).fold(
                onSuccess = { average ->
                    _averageRating.value = average
                    println("✅ Promedio de calificación: $average")
                },
                onFailure = { error ->
                    println("❌ Error al cargar promedio: ${error.message}")
                }
            )

            // Load total ratings
            ratingRepository.getTotalRatings(productId).fold(
                onSuccess = { total ->
                    _totalRatings.value = total
                    println("✅ Total de calificaciones: $total")
                },
                onFailure = { error ->
                    println("❌ Error al cargar total: ${error.message}")
                }
            )
        }
    }

    /**
     * Add vendor response to a rating
     */
    fun addVendorResponse(ratingId: String, response: String, productId: String) {
        viewModelScope.launch {
            _vendorResponseState.value = UiState.Loading
            println("💬 Agregando respuesta del vendedor...")

            val result = ratingRepository.addVendorResponse(ratingId, response)
            result.fold(
                onSuccess = { updatedRating ->
                    _vendorResponseState.value = UiState.Success(updatedRating)
                    println("✅ Respuesta agregada exitosamente")
                    // Reload ratings to reflect changes
                    loadProductRatings(productId)
                },
                onFailure = { error ->
                    println("❌ Error al agregar respuesta: ${error.message}")
                    _vendorResponseState.value = UiState.Error(error.message ?: "Error al agregar respuesta")
                }
            )
        }
    }

    /**
     * Update existing vendor response
     */
    fun updateVendorResponse(ratingId: String, response: String, productId: String) {
        viewModelScope.launch {
            _vendorResponseState.value = UiState.Loading
            println("💬 Actualizando respuesta del vendedor...")

            val result = ratingRepository.updateVendorResponse(ratingId, response)
            result.fold(
                onSuccess = { updatedRating ->
                    _vendorResponseState.value = UiState.Success(updatedRating)
                    println("✅ Respuesta actualizada exitosamente")
                    // Reload ratings to reflect changes
                    loadProductRatings(productId)
                },
                onFailure = { error ->
                    println("❌ Error al actualizar respuesta: ${error.message}")
                    _vendorResponseState.value = UiState.Error(error.message ?: "Error al actualizar respuesta")
                }
            )
        }
    }

    /**
     * Delete vendor response
     */
    fun deleteVendorResponse(ratingId: String, productId: String) {
        viewModelScope.launch {
            _vendorResponseState.value = UiState.Loading
            println("🗑️ Eliminando respuesta del vendedor...")

            val result = ratingRepository.deleteVendorResponse(ratingId)
            result.fold(
                onSuccess = { updatedRating ->
                    _vendorResponseState.value = UiState.Success(updatedRating)
                    println("✅ Respuesta eliminada exitosamente")
                    // Reload ratings to reflect changes
                    loadProductRatings(productId)
                },
                onFailure = { error ->
                    println("❌ Error al eliminar respuesta: ${error.message}")
                    _vendorResponseState.value = UiState.Error(error.message ?: "Error al eliminar respuesta")
                }
            )
        }
    }

    /**
     * Reset vendor response state
     */
    fun resetVendorResponseState() {
        _vendorResponseState.value = UiState.Idle
    }

    /**
     * Reset ratings state
     */
    fun resetRatingsState() {
        _ratingsState.value = UiState.Idle
    }
}
