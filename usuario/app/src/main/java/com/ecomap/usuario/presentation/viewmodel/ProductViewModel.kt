package com.ecomap.usuario.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.usuario.data.model.Product
import com.ecomap.usuario.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProductUploadUiState {
    object Idle : ProductUploadUiState()
    object Loading : ProductUploadUiState()
    data class Success(val message: String) : ProductUploadUiState()
    data class Error(val message: String) : ProductUploadUiState()
}

sealed class CommunityProductsUiState {
    object Loading : CommunityProductsUiState()
    data class Success(val products: List<Product>) : CommunityProductsUiState()
    data class Error(val message: String) : CommunityProductsUiState()
}

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uploadState = MutableStateFlow<ProductUploadUiState>(ProductUploadUiState.Idle)
    val uploadState: StateFlow<ProductUploadUiState> = _uploadState.asStateFlow()

    private val _communityProducts = MutableStateFlow<CommunityProductsUiState>(CommunityProductsUiState.Loading)
    val communityProducts: StateFlow<CommunityProductsUiState> = _communityProducts.asStateFlow()

    init {
        loadCommunityProducts()
    }

    fun uploadProduct(
        name: String,
        description: String?,
        price: Double,
        imageUri: Uri?,
        latitude: Double,
        longitude: Double,
        address: String?,
        unitType: String = "unit"
    ) {
        viewModelScope.launch {
            _uploadState.value = ProductUploadUiState.Loading

            productRepository.uploadProduct(
                name = name,
                description = description,
                price = price,
                imageUri = imageUri,
                latitude = latitude,
                longitude = longitude,
                address = address,
                unitType = unitType
            )
                .onSuccess {
                    _uploadState.value = ProductUploadUiState.Success("¡Producto publicado! Está pendiente de moderación y será visible en 8 horas.")
                    loadCommunityProducts() // Recargar lista
                }
                .onFailure { error ->
                    _uploadState.value = ProductUploadUiState.Error(
                        error.message ?: "Error al subir producto"
                    )
                }
        }
    }

    fun loadCommunityProducts() {
        viewModelScope.launch {
            _communityProducts.value = CommunityProductsUiState.Loading

            productRepository.getCommunityProducts()
                .onSuccess { products ->
                    _communityProducts.value = CommunityProductsUiState.Success(products)
                }
                .onFailure { error ->
                    _communityProducts.value = CommunityProductsUiState.Error(
                        error.message ?: "Error al cargar productos"
                    )
                }
        }
    }

    fun resetUploadState() {
        _uploadState.value = ProductUploadUiState.Idle
    }

    fun getProductById(productId: String): kotlinx.coroutines.flow.Flow<Product?> = kotlinx.coroutines.flow.flow {
        productRepository.getProductById(productId)
            .onSuccess { product ->
                emit(product)
            }
            .onFailure {
                emit(null)
            }
    }
}
