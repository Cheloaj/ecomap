package com.ecomap.usuario.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.usuario.data.model.Business
import com.ecomap.usuario.data.model.Offer
import com.ecomap.usuario.data.model.Product
import com.ecomap.usuario.data.model.ProductReport
import com.ecomap.usuario.domain.repository.BusinessRepository
import com.ecomap.usuario.domain.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BusinessUiState {
    object Loading : BusinessUiState()
    data class Success(val businesses: List<Business>) : BusinessUiState()
    data class Error(val message: String) : BusinessUiState()
}

sealed class OffersUiState {
    object Loading : OffersUiState()
    data class Success(val offers: List<Offer>) : OffersUiState()
    data class Error(val message: String) : OffersUiState()
}

sealed class ProductsUiState {
    object Loading : ProductsUiState()
    data class Success(val products: List<Product>) : ProductsUiState()
    data class Error(val message: String) : ProductsUiState()
}

sealed class ReportUiState {
    object Idle : ReportUiState()
    object Loading : ReportUiState()
    object Success : ReportUiState()
    data class Error(val message: String) : ReportUiState()
}

@HiltViewModel
class BusinessViewModel @Inject constructor(
    private val businessRepository: BusinessRepository,
    private val reportRepository: ReportRepository,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _businessState = MutableStateFlow<BusinessUiState>(BusinessUiState.Loading)
    val businessState: StateFlow<BusinessUiState> = _businessState.asStateFlow()

    private val _offersState = MutableStateFlow<OffersUiState>(OffersUiState.Loading)
    val offersState: StateFlow<OffersUiState> = _offersState.asStateFlow()

    private val _productsState = MutableStateFlow<ProductsUiState>(ProductsUiState.Loading)
    val productsState: StateFlow<ProductsUiState> = _productsState.asStateFlow()

    private val _selectedBusiness = MutableStateFlow<Business?>(null)
    val selectedBusiness: StateFlow<Business?> = _selectedBusiness.asStateFlow()

    init {
        viewModelScope.launch {
            // Supabase restaura la sesión desde el almacenamiento de forma
            // asíncrona al arrancar. Si se consultaba de inmediato, la petición
            // salía como "anon" y el servidor la rechazaba con
            // "permission denied", dejando un error en pantalla que ya no se
            // recuperaba aunque la sesión llegara medio segundo después.
            esperarSesion()
            loadAllBusinesses()
            loadAllOffers()
            subscribeToBusinessChanges()
            subscribeToProductChanges()
        }
    }

    /**
     * Espera a que la sesión quede disponible, con un tope de 5 segundos.
     * Si al agotarse no hay sesión, se continúa igualmente: la pantalla mostrará
     * el estado vacío en vez de quedarse cargando para siempre.
     */
    private suspend fun esperarSesion(timeoutMs: Long = 5_000) {
        var esperado = 0L
        while (supabase.auth.currentUserOrNull() == null && esperado < timeoutMs) {
            delay(150)
            esperado += 150
        }
    }

    private fun subscribeToBusinessChanges() {
        viewModelScope.launch {
            try {
                val channel = supabase.realtime.channel("businesses-changes")

                channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "businesses"
                }.onEach { action ->
                    android.util.Log.d("BusinessViewModel", "📡 Cambio en businesses: ${action.javaClass.simpleName}")
                    // Recargar negocios cuando haya cualquier cambio
                    loadAllBusinesses()
                }.launchIn(viewModelScope)

                channel.subscribe()
                android.util.Log.d("BusinessViewModel", "✅ Suscrito a cambios de businesses")
            } catch (e: Exception) {
                android.util.Log.e("BusinessViewModel", "❌ Error al suscribirse a businesses: ${e.message}")
            }
        }
    }

    private fun subscribeToProductChanges() {
        viewModelScope.launch {
            try {
                val channel = supabase.realtime.channel("products-changes")

                channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "products"
                }.onEach { action ->
                    android.util.Log.d("BusinessViewModel", "📡 Cambio en products: ${action.javaClass.simpleName}")
                    // Recargar productos del negocio seleccionado
                    _selectedBusiness.value?.let { business ->
                        loadProductsByBusiness(business.id)
                    }
                }.launchIn(viewModelScope)

                channel.subscribe()
                android.util.Log.d("BusinessViewModel", "✅ Suscrito a cambios de products")
            } catch (e: Exception) {
                android.util.Log.e("BusinessViewModel", "❌ Error al suscribirse a products: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                supabase.realtime.removeChannel(supabase.realtime.channel("businesses-changes"))
                supabase.realtime.removeChannel(supabase.realtime.channel("products-changes"))
                android.util.Log.d("BusinessViewModel", "🔌 Desconectado de Realtime")
            } catch (e: Exception) {
                android.util.Log.e("BusinessViewModel", "❌ Error al desconectar: ${e.message}")
            }
        }
    }

    fun loadAllBusinesses() {
        viewModelScope.launch {
            _businessState.value = BusinessUiState.Loading
            businessRepository.getAllBusinesses()
                .onSuccess { businesses ->
                    _businessState.value = BusinessUiState.Success(businesses)
                }
                .onFailure { error ->
                    _businessState.value = BusinessUiState.Error(mensajeAmable(error))
                }
        }
    }

    fun loadAllOffers() {
        viewModelScope.launch {
            _offersState.value = OffersUiState.Loading
            businessRepository.getAllActiveOffers()
                .onSuccess { offers ->
                    _offersState.value = OffersUiState.Success(offers)
                }
                .onFailure { error ->
                    _offersState.value = OffersUiState.Error(mensajeAmable(error))
                }
        }
    }

    fun selectBusiness(business: Business) {
        _selectedBusiness.value = business
        loadProductsByBusiness(business.id)
        loadOffersByBusiness(business.id)
    }

    private fun loadProductsByBusiness(businessId: String) {
        viewModelScope.launch {
            _productsState.value = ProductsUiState.Loading
            businessRepository.getProductsByBusiness(businessId)
                .onSuccess { allProducts ->
                    android.util.Log.d("BusinessViewModel", "📦 Total productos obtenidos: ${allProducts.size}")
                    android.util.Log.d("BusinessViewModel", "🔍 Productos con oferta: ${allProducts.count { it.isOnOffer }}")
                    android.util.Log.d("BusinessViewModel", "🔍 Productos SIN oferta: ${allProducts.count { !it.isOnOffer }}")

                    // Filtrar solo productos SIN ofertas
                    val productsWithoutOffers = allProducts.filter { !it.isOnOffer }
                    android.util.Log.d("BusinessViewModel", "✅ Productos filtrados (sin oferta): ${productsWithoutOffers.size}")

                    _productsState.value = ProductsUiState.Success(productsWithoutOffers)
                }
                .onFailure { error ->
                    _productsState.value = ProductsUiState.Error(error.message ?: "Error al cargar productos")
                }
        }
    }

    private fun loadOffersByBusiness(businessId: String) {
        viewModelScope.launch {
            businessRepository.getOffersByBusiness(businessId)
                .onSuccess { offers ->
                    _offersState.value = OffersUiState.Success(offers)
                }
                .onFailure { error ->
                    _offersState.value = OffersUiState.Error(mensajeAmable(error))
                }
        }
    }

    fun searchBusinesses(query: String) {
        viewModelScope.launch {
            try {
                if (query.isBlank()) {
                    android.util.Log.d("BusinessViewModel", "⚠️ Query vacío, cargando todos los negocios")
                    loadAllBusinesses()
                    return@launch
                }

                android.util.Log.d("BusinessViewModel", "🔍 Iniciando búsqueda: '$query'")
                _businessState.value = BusinessUiState.Loading

                businessRepository.searchBusinesses(query)
                    .onSuccess { businesses ->
                        android.util.Log.d("BusinessViewModel", "✅ Búsqueda exitosa: ${businesses.size} resultados")
                        _businessState.value = BusinessUiState.Success(businesses)
                    }
                    .onFailure { error ->
                        android.util.Log.e("BusinessViewModel", "❌ Error en búsqueda: ${error.message}")
                        error.printStackTrace()
                        _businessState.value = BusinessUiState.Error(error.message ?: "Error al buscar")
                    }
            } catch (e: Exception) {
                android.util.Log.e("BusinessViewModel", "❌ Excepción en searchBusinesses: ${e.message}")
                e.printStackTrace()
                _businessState.value = BusinessUiState.Error(e.message ?: "Error inesperado")
            }
        }
    }

    fun getAllBusinesses() {
        loadAllBusinesses()
    }

    // Estados y funciones para reportes
    private val _reportState = MutableStateFlow<ReportUiState>(ReportUiState.Idle)
    val reportState: StateFlow<ReportUiState> = _reportState.asStateFlow()

    fun submitProductReport(report: ProductReport) {
        viewModelScope.launch {
            _reportState.value = ReportUiState.Loading
            reportRepository.submitProductReport(report)
                .onSuccess {
                    _reportState.value = ReportUiState.Success
                }
                .onFailure { error ->
                    _reportState.value = ReportUiState.Error(error.message ?: "Error al enviar reporte")
                }
        }
    }

    fun resetReportState() {
        _reportState.value = ReportUiState.Idle
    }

    /**
     * Traduce cualquier fallo a un mensaje que una persona pueda entender.
     *
     * Antes se pintaba `error.message` tal cual, y eso volcaba en pantalla la
     * URL completa, las cabeceras y la API key del proyecto: ilegible para el
     * usuario y una fuga de información innecesaria.
     */
    private fun mensajeAmable(error: Throwable): String {
        val texto = error.message.orEmpty()
        return when {
            texto.contains("permission denied", true) ||
            texto.contains("JWT", true) -> "Tu sesión expiró. Vuelve a iniciar sesión."
            texto.contains("Unable to resolve host", true) ||
            texto.contains("timeout", true) ||
            texto.contains("failed to connect", true) -> "Sin conexión a internet. Revisa tu red."
            else -> "No se pudieron cargar los datos. Inténtalo de nuevo."
        }
    }
}
