package com.ecomap.socio.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.socio.data.model.Business
import com.ecomap.socio.data.model.ScheduledOffer
import com.ecomap.socio.domain.repository.AuthRepository
import com.ecomap.socio.domain.repository.BusinessRepository
import com.ecomap.socio.domain.repository.ScheduledOfferRepository
import com.ecomap.socio.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduledOfferViewModel @Inject constructor(
    private val scheduledOfferRepository: ScheduledOfferRepository,
    private val authRepository: AuthRepository,
    private val businessRepository: BusinessRepository
) : ViewModel() {

    private val _scheduledOffers = MutableStateFlow<List<ScheduledOffer>>(emptyList())
    val scheduledOffers: StateFlow<List<ScheduledOffer>> = _scheduledOffers.asStateFlow()

    private val _scheduledOffersState = MutableStateFlow<UiState<List<ScheduledOffer>>>(UiState.Idle)
    val scheduledOffersState: StateFlow<UiState<List<ScheduledOffer>>> = _scheduledOffersState.asStateFlow()

    private val _createScheduledOfferState = MutableStateFlow<UiState<ScheduledOffer>>(UiState.Idle)
    val createScheduledOfferState: StateFlow<UiState<ScheduledOffer>> = _createScheduledOfferState.asStateFlow()

    private val _updateScheduledOfferState = MutableStateFlow<UiState<ScheduledOffer>>(UiState.Idle)
    val updateScheduledOfferState: StateFlow<UiState<ScheduledOffer>> = _updateScheduledOfferState.asStateFlow()

    private val _deleteScheduledOfferState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val deleteScheduledOfferState: StateFlow<UiState<Unit>> = _deleteScheduledOfferState.asStateFlow()

    private val _scheduledOffersCount = MutableStateFlow(0)
    val scheduledOffersCount: StateFlow<Int> = _scheduledOffersCount.asStateFlow()

    private val _userBusiness = MutableStateFlow<List<Business>>(emptyList())
    val userBusiness: StateFlow<List<Business>> = _userBusiness.asStateFlow()

    fun loadScheduledOffers() {
        viewModelScope.launch {
            println("🚀 ========== INICIO CARGA SCHEDULED OFFERS ==========")
            _scheduledOffersState.value = UiState.Loading

            // PASO 1: Obtener usuario actual
            println("📝 Paso 1: Obteniendo usuario actual...")
            val userResult = authRepository.getCurrentUser()
            println("   Result Success: ${userResult.isSuccess}")

            val user = userResult.getOrNull()
            if (user == null) {
                println("❌ ERROR: Usuario no encontrado")
                _scheduledOffersState.value = UiState.Error("Usuario no encontrado")
                return@launch
            }

            println("✅ Usuario encontrado:")
            println("   ID: ${user.id}")
            println("   Email: ${user.email}")
            println("   Nombre: ${user.fullName}")
            println("   ⭐ ES PRO: ${user.isPro}")

            // PASO 2: Validar Plan Pro del USUARIO (no del negocio)
            println("\n📝 Paso 2: Validación de Plan Pro")
            println("   ¿Usuario tiene Plan Pro?: ${user.isPro}")

            if (!user.isPro) {
                println("\n❌ BLOQUEADO: Usuario no tiene Plan Pro")
                println("   user.isPro = ${user.isPro}")
                _scheduledOffersState.value = UiState.Error("Esta función requiere Plan Pro")
                return@launch
            }

            println("✅ Usuario tiene Plan Pro - Acceso concedido")

            // PASO 3: Obtener TODOS los negocios del usuario
            println("\n📝 Paso 3: Obteniendo negocios del usuario...")
            val businessesResult = businessRepository.getAllBusinessesByOwnerId(user.id)
            val businesses = businessesResult.getOrNull() ?: emptyList()
            println("   Negocios encontrados: ${businesses.size}")

            if (businesses.isEmpty()) {
                println("⚠️ Usuario Pro sin negocios registrados")
                _scheduledOffersState.value = UiState.Success(emptyList())
                return@launch
            }

            // PASO 4: Cargar ofertas programadas de TODOS los negocios
            println("\n📝 Paso 4: Cargando ofertas programadas...")
            val allScheduledOffers = mutableListOf<ScheduledOffer>()
            businesses.forEach { business ->
                println("   Cargando ofertas de: ${business.businessName}")
                val result = scheduledOfferRepository.getScheduledOffersByBusinessId(business.id)
                result.onSuccess { offers ->
                    println("      ✅ ${offers.size} ofertas programadas")
                    allScheduledOffers.addAll(offers)
                }
                result.onFailure { error ->
                    println("      ❌ Error: ${error.message}")
                }
            }

            println("\n✅ ========== FIN CARGA EXITOSA ==========")
            println("   Total ofertas programadas: ${allScheduledOffers.size}")

            _scheduledOffers.value = allScheduledOffers
            _scheduledOffersState.value = UiState.Success(allScheduledOffers)
        }
    }

    fun loadUserBusiness() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser().getOrNull()
            if (user != null) {
                val result = businessRepository.getAllBusinessesByOwnerId(user.id)
                _userBusiness.value = result.getOrNull() ?: emptyList()
            }
        }
    }

    fun createScheduledOffer(scheduledOffer: ScheduledOffer) {
        viewModelScope.launch {
            _createScheduledOfferState.value = UiState.Loading

            val user = authRepository.getCurrentUser().getOrNull()
            if (user == null) {
                _createScheduledOfferState.value = UiState.Error("Usuario no encontrado")
                return@launch
            }

            // Check if USER has Pro plan (not business)
            if (!user.isPro) {
                _createScheduledOfferState.value = UiState.Error("Esta función requiere Plan Pro")
                return@launch
            }

            val result = scheduledOfferRepository.createScheduledOffer(scheduledOffer)
            _createScheduledOfferState.value = result.fold(
                onSuccess = { created ->
                    loadScheduledOffers()
                    UiState.Success(created)
                },
                onFailure = { error ->
                    UiState.Error(error.message ?: "Error al programar oferta")
                }
            )
        }
    }

    fun updateScheduledOffer(scheduledOffer: ScheduledOffer) {
        viewModelScope.launch {
            _updateScheduledOfferState.value = UiState.Loading

            val result = scheduledOfferRepository.updateScheduledOffer(scheduledOffer)
            _updateScheduledOfferState.value = result.fold(
                onSuccess = { updated ->
                    loadScheduledOffers()
                    UiState.Success(updated)
                },
                onFailure = { error ->
                    UiState.Error(error.message ?: "Error al actualizar oferta programada")
                }
            )
        }
    }

    fun deleteScheduledOffer(scheduledOfferId: String) {
        viewModelScope.launch {
            _deleteScheduledOfferState.value = UiState.Loading

            val result = scheduledOfferRepository.deleteScheduledOffer(scheduledOfferId)
            _deleteScheduledOfferState.value = result.fold(
                onSuccess = {
                    loadScheduledOffers()
                    UiState.Success(Unit)
                },
                onFailure = { error ->
                    UiState.Error(error.message ?: "Error al eliminar oferta programada")
                }
            )
        }
    }

    fun cancelScheduledOffer(scheduledOfferId: String) {
        viewModelScope.launch {
            val result = scheduledOfferRepository.cancelScheduledOffer(scheduledOfferId)
            result.fold(
                onSuccess = {
                    loadScheduledOffers()
                },
                onFailure = {
                    // Handle error
                }
            )
        }
    }

    private fun updateScheduledOffersCount(businessId: String) {
        viewModelScope.launch {
            val result = scheduledOfferRepository.getScheduledOffersCount(businessId)
            _scheduledOffersCount.value = result.getOrDefault(0)
        }
    }

    fun resetCreateScheduledOfferState() {
        _createScheduledOfferState.value = UiState.Idle
    }

    fun resetUpdateScheduledOfferState() {
        _updateScheduledOfferState.value = UiState.Idle
    }

    fun resetDeleteScheduledOfferState() {
        _deleteScheduledOfferState.value = UiState.Idle
    }
}
