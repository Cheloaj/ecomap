package com.ecomap.usuario.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.usuario.data.model.BasketItem
import com.ecomap.usuario.domain.repository.BasketRepository
import com.ecomap.usuario.utils.SubscriptionMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BasketUiState {
    object Loading : BasketUiState()
    data class Success(val items: List<BasketItem>) : BasketUiState()
    data class Error(val message: String) : BasketUiState()
}

@HiltViewModel
class BasketViewModel @Inject constructor(
    private val basketRepository: BasketRepository,
    private val subscriptionMonitor: SubscriptionMonitor
) : ViewModel() {

    companion object {
        private const val FREE_PLAN_MAX_PRODUCTS = 5
    }

    private val _basketState = MutableStateFlow<BasketUiState>(BasketUiState.Loading)
    val basketState: StateFlow<BasketUiState> = _basketState.asStateFlow()

    private val _basketCount = MutableStateFlow(0)
    val basketCount: StateFlow<Int> = _basketCount.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _showUpgradeDialog = MutableStateFlow(false)
    val showUpgradeDialog: StateFlow<Boolean> = _showUpgradeDialog.asStateFlow()

    init {
        loadBasket()
    }

    fun loadBasket() {
        viewModelScope.launch {
            _basketState.value = BasketUiState.Loading
            basketRepository.getBasketItems()
                .onSuccess { items ->
                    _basketState.value = BasketUiState.Success(items)
                    updateBasketCount()
                }
                .onFailure { error ->
                    _basketState.value = BasketUiState.Error(error.message ?: "Error desconocido")
                }
        }
    }

    fun addToBasket(productId: String, quantity: Int = 1) {
        viewModelScope.launch {
            // Verificar límite de productos para usuarios FREE
            val currentState = _basketState.value
            if (currentState is BasketUiState.Success) {
                val currentProducts = currentState.items.size
                val hasPro = subscriptionMonitor.hasProAccess()

                // Si es FREE y ya tiene 5 productos, bloquear
                if (!hasPro && currentProducts >= FREE_PLAN_MAX_PRODUCTS) {
                    _showUpgradeDialog.value = true
                    _message.value = "Límite alcanzado. Actualiza a PRO para productos ilimitados."
                    return@launch
                }
            }

            // Si pasa la validación, agregar el producto
            basketRepository.addToBasket(productId, quantity)
                .onSuccess {
                    _message.value = "Producto añadido a la canasta"
                    loadBasket()
                }
                .onFailure { error ->
                    _message.value = error.message ?: "Error al añadir"
                }
        }
    }

    fun showUpgradeDialog() {
        _showUpgradeDialog.value = true
    }

    fun dismissUpgradeDialog() {
        _showUpgradeDialog.value = false
    }

    fun updateQuantity(basketItemId: String, quantity: Int) {
        viewModelScope.launch {
            basketRepository.updateQuantity(basketItemId, quantity)
                .onSuccess {
                    loadBasket()
                }
                .onFailure { error ->
                    _message.value = error.message ?: "Error al actualizar"
                }
        }
    }

    fun removeItem(basketItemId: String) {
        viewModelScope.launch {
            basketRepository.removeFromBasket(basketItemId)
                .onSuccess {
                    _message.value = "Producto eliminado"
                    loadBasket()
                }
                .onFailure { error ->
                    _message.value = error.message ?: "Error al eliminar"
                }
        }
    }

    fun clearBasket() {
        viewModelScope.launch {
            basketRepository.clearBasket()
                .onSuccess {
                    _message.value = "Canasta vaciada"
                    loadBasket()
                }
                .onFailure { error ->
                    _message.value = error.message ?: "Error al vaciar"
                }
        }
    }

    private fun updateBasketCount() {
        viewModelScope.launch {
            basketRepository.getBasketCount()
                .onSuccess { count ->
                    _basketCount.value = count
                }
                .onFailure {
                    _basketCount.value = 0
                }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
