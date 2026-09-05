package com.ecomap.socio.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.socio.data.model.Offer
import com.ecomap.socio.domain.repository.AuthRepository
import com.ecomap.socio.domain.repository.OfferRepository
import com.ecomap.socio.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class OfferStatistics(
    val totalOffers: Int = 0,
    val activeOffers: Int = 0,
    val expiredOffers: Int = 0,
    val averagePrice: Double = 0.0,
    val mostExpensiveOffer: Offer? = null,
    val mostCheapOffer: Offer? = null,
    val offersByCategory: Map<String, Int> = emptyMap(),
    val dailyOffers: Map<String, Int> = emptyMap() // Last 7 days
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val offerRepository: OfferRepository,
    private val authRepository: AuthRepository
    // Suponiendo que BusinessRepository se inyectará si es necesario
) : ViewModel() {

    private val _statistics = MutableStateFlow(OfferStatistics())
    val statistics: StateFlow<OfferStatistics> = _statistics.asStateFlow()

    private val _statisticsState = MutableStateFlow<UiState<OfferStatistics>>(UiState.Idle)
    val statisticsState: StateFlow<UiState<OfferStatistics>> = _statisticsState.asStateFlow()

    fun loadStatistics() {
        viewModelScope.launch {
            _statisticsState.value = UiState.Loading

            val userResult = authRepository.getCurrentUser()
            if (userResult.isFailure || userResult.getOrNull() == null) {
                _statisticsState.value = UiState.Error("Usuario no autenticado")
                return@launch
            }
            val user = userResult.getOrThrow()!!

            // Aquí iría la lógica para verificar el plan del negocio (ej. 'pro')
            // val business = businessRepository.getBusinessByOwnerId(user.id).getOrNull()
            // if (business?.subscriptionPlan != "pro") {
            //     _statisticsState.value = UiState.Error("Esta función requiere Plan Pro")
            //     return@launch
            // }

            val result = offerRepository.getOffersByBusinessId(user.id)
            _statisticsState.value = result.fold(
                onSuccess = { offersList ->
                    val stats = calculateStatistics(offersList)
                    _statistics.value = stats
                    UiState.Success(stats)
                },
                onFailure = { error ->
                    UiState.Error(error.message ?: "Error al cargar estadísticas")
                }
            )
        }
    }

    private fun calculateStatistics(offers: List<Offer>): OfferStatistics {
        if (offers.isEmpty()) {
            return OfferStatistics()
        }

        val activeOffers = offers.filter { it.isActive }
        val averagePrice = if (offers.isNotEmpty()) offers.map { it.price }.average() else 0.0
        val mostExpensive = offers.maxByOrNull { it.price }
        val mostCheap = offers.minByOrNull { it.price }

        val offersByCategory = offers.groupBy { it.productName }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .toMap()

        val dailyOffers = mutableMapOf<String, Int>()
        val timeZone = TimeZone.currentSystemDefault()
        for (i in 0..6) {
            val date = Clock.System.now().minus(i.toLong(), DateTimeUnit.DAY, timeZone)
            val dateString = date.toLocalDateTime(timeZone).date.toString()

            val count = offers.count { offer ->
                try {
                    val offerDateString = Instant.parse(offer.createdAt).toLocalDateTime(timeZone).date.toString()
                    offerDateString == dateString
                } catch (e: Exception) {
                    false
                }
            }
            dailyOffers[dateString] = count
        }

        return OfferStatistics(
            totalOffers = offers.size,
            activeOffers = activeOffers.size,
            expiredOffers = offers.size - activeOffers.size,
            averagePrice = averagePrice,
            mostExpensiveOffer = mostExpensive,
            mostCheapOffer = mostCheap,
            offersByCategory = offersByCategory,
            dailyOffers = dailyOffers
        )
    }
}
