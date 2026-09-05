package com.ecomap.usuario.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.usuario.data.model.ProductRating
import com.ecomap.usuario.data.model.ProductRatingStats
import com.ecomap.usuario.domain.repository.RatingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RatingUiState {
    object Loading : RatingUiState()
    data class Success(val ratings: List<ProductRating>, val userRating: ProductRating?) : RatingUiState()
    data class Error(val message: String) : RatingUiState()
}

@HiltViewModel
class RatingViewModel @Inject constructor(
    private val ratingRepository: RatingRepository
) : ViewModel() {

    private val _ratingsState = MutableStateFlow<RatingUiState>(RatingUiState.Loading)
    val ratingsState: StateFlow<RatingUiState> = _ratingsState.asStateFlow()

    private val _ratingStats = MutableStateFlow<ProductRatingStats?>(null)
    val ratingStats: StateFlow<ProductRatingStats?> = _ratingStats.asStateFlow()

    fun loadProductRatings(productId: String) {
        viewModelScope.launch {
            _ratingsState.value = RatingUiState.Loading

            // Cargar calificaciones
            ratingRepository.getProductRatings(productId)
                .onSuccess { ratings ->
                    // Cargar calificación del usuario actual
                    ratingRepository.getUserRatingForProduct(productId)
                        .onSuccess { userRating ->
                            _ratingsState.value = RatingUiState.Success(ratings, userRating)
                        }
                        .onFailure {
                            _ratingsState.value = RatingUiState.Success(ratings, null)
                        }
                }
                .onFailure { error ->
                    _ratingsState.value = RatingUiState.Error(error.message ?: "Error al cargar calificaciones")
                }

            // Cargar estadísticas
            ratingRepository.getProductRatingStats(productId)
                .onSuccess { stats ->
                    _ratingStats.value = stats
                }
        }
    }

    fun rateProduct(productId: String, rating: Int, comment: String? = null, imageUri: Uri? = null, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            ratingRepository.rateProduct(productId, rating, comment, imageUri)
                .onSuccess {
                    loadProductRatings(productId) // Recargar calificaciones
                    onResult(true, "Producto calificado exitosamente")
                }
                .onFailure { error ->
                    onResult(false, error.message ?: "Error al calificar")
                }
        }
    }

    fun updateRating(ratingId: String, productId: String, newRating: Int, comment: String? = null, imageUri: Uri? = null, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            ratingRepository.updateRating(ratingId, newRating, comment, imageUri)
                .onSuccess {
                    loadProductRatings(productId) // Recargar calificaciones
                    onResult(true, "Calificación actualizada")
                }
                .onFailure { error ->
                    onResult(false, error.message ?: "Error al actualizar")
                }
        }
    }

    fun deleteRating(ratingId: String, productId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            ratingRepository.deleteRating(ratingId)
                .onSuccess {
                    loadProductRatings(productId) // Recargar calificaciones
                    onResult(true, "Calificación eliminada")
                }
                .onFailure { error ->
                    onResult(false, error.message ?: "Error al eliminar")
                }
        }
    }
}
