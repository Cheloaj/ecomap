package com.ecomap.socio.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.socio.data.model.Offer
import com.ecomap.socio.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OfferViewModel @Inject constructor(
    // Aquí irían tus repositorios, por ejemplo:
    // private val offerRepository: OfferRepository
) : ViewModel() {

    private val _offers = MutableStateFlow<List<Offer>>(emptyList())
    val offers: StateFlow<List<Offer>> = _offers.asStateFlow()

    private val _offersState = MutableStateFlow<UiState<List<Offer>>>(UiState.Idle)
    val offersState: StateFlow<UiState<List<Offer>>> = _offersState.asStateFlow()

    private val _activeOffersCount = MutableStateFlow(0)
    val activeOffersCount: StateFlow<Int> = _activeOffersCount.asStateFlow()

    fun loadOffers(businessId: String) {
        viewModelScope.launch {
            _offersState.value = UiState.Loading
            // Lógica para cargar ofertas desde el repositorio
            // Por ahora, simularemos una carga exitosa con datos de ejemplo
            val exampleOffers = listOf(
                Offer(id = "1", productName = "Tomates Frescos", price = 25.50, unit = "kg"),
                Offer(id = "2", productName = "Cebolla Blanca", price = 15.0, unit = "kg", isOutOfStock = true)
            )
            _offers.value = exampleOffers
            _activeOffersCount.value = exampleOffers.size
            _offersState.value = UiState.Success(exampleOffers)
        }
    }

    fun subscribeToRealtimeOffers(businessId: String) {
        // Lógica para suscripción en tiempo real con Supabase
    }

    fun createOffer(offer: Offer, subscriptionPlan: String) {
        // Lógica para crear oferta
    }

    fun updateOffer(offer: Offer) {
        // Lógica para actualizar oferta
    }

    fun deleteOffer(offerId: String, businessId: String) {
        // Lógica para eliminar oferta
    }

    fun toggleOfferStock(offerId: String, isOutOfStock: Boolean, businessId: String) {
        // Lógica para cambiar estado de stock
    }
}
