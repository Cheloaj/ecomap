package com.ecomap.socio.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.socio.data.local.UserSession
import com.ecomap.socio.data.model.Business
import com.ecomap.socio.domain.repository.AuthRepository
import com.ecomap.socio.domain.repository.BusinessRepository
import com.ecomap.socio.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BusinessViewModel @Inject constructor(
    private val businessRepository: BusinessRepository,
    private val authRepository: AuthRepository,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _business = MutableStateFlow<Business?>(null)
    val business: StateFlow<Business?> = _business.asStateFlow()

    private val _businessState = MutableStateFlow<UiState<Business?>>(UiState.Idle)
    val businessState: StateFlow<UiState<Business?>> = _businessState.asStateFlow()

    // ✅ Lista de negocios
    private val _businesses = MutableStateFlow<List<Business>>(emptyList())
    val businesses: StateFlow<List<Business>> = _businesses.asStateFlow()

    private val _businessesState = MutableStateFlow<UiState<List<Business>>>(UiState.Idle)
    val businessesState: StateFlow<UiState<List<Business>>> = _businessesState.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    // ✅ Estado para Pro user
    private val _isProUser = MutableStateFlow(false)
    val isProUser: StateFlow<Boolean> = _isProUser.asStateFlow()

    // ✅ Estado para saber si ya se verificó el estado Pro
    private val _isProStatusChecked = MutableStateFlow(false)
    val isProStatusChecked: StateFlow<Boolean> = _isProStatusChecked.asStateFlow()

    // ✅ Realtime channel para escuchar cambios
    private var realtimeChannel: RealtimeChannel? = null

    fun loadUserBusiness() {
        viewModelScope.launch {
            _businessState.value = UiState.Loading
            println("🏢 Cargando negocio del usuario...")

            val userResult = authRepository.getCurrentUser()
            userResult.fold(
                onSuccess = { user ->
                    if (user == null) {
                        println("❌ No hay usuario autenticado")
                        _businessState.value = UiState.Error("No hay usuario autenticado")
                        return@launch
                    }

                    // 💾 Cachear información del usuario
                    _currentUserId.value = user.id
                    UserSession.setCurrentBusinessId(user.id)
                    UserSession.setProStatus(user.isPro)

                    val result = businessRepository.getBusinessByOwnerId(user.id)
                    result.fold(
                        onSuccess = { business ->
                            _business.value = business
                            _businessState.value = UiState.Success(business)
                            if (business != null) {
                                println("✅ Negocio cargado: ${business.businessName}")
                            } else {
                                println("ℹ️ Usuario no tiene negocio registrado")
                            }
                        },
                        onFailure = { error ->
                            println("❌ Error al cargar negocio: ${error.message}")
                            _businessState.value = UiState.Error(error.message ?: "Error al cargar negocio")
                        }
                    )
                },
                onFailure = { error ->
                    println("❌ Error al obtener usuario: ${error.message}")
                    _businessState.value = UiState.Error(error.message ?: "Error al obtener usuario")
                }
            )
        }
    }

    // ✅ Cargar TODOS los negocios del usuario
    fun loadAllUserBusinesses() {
        viewModelScope.launch {
            _businessesState.value = UiState.Loading
            println("🏢 Cargando todos los negocios del usuario...")

            val userResult = authRepository.getCurrentUser()
            userResult.fold(
                onSuccess = { user ->
                    if (user == null) {
                        println("❌ No hay usuario autenticado")
                        _businessesState.value = UiState.Error("No hay usuario autenticado")
                        return@launch
                    }

                    // 💾 Cachear información del usuario
                    _currentUserId.value = user.id
                    UserSession.setCurrentBusinessId(user.id)
                    UserSession.setProStatus(user.isPro)

                    val result = businessRepository.getAllBusinessesByOwnerId(user.id)
                    result.fold(
                        onSuccess = { businesses ->
                            _businesses.value = businesses
                            _businessesState.value = UiState.Success(businesses)
                            println("✅ ${businesses.size} negocios cargados")
                        },
                        onFailure = { error ->
                            println("❌ Error al cargar negocios: ${error.message}")
                            _businessesState.value = UiState.Error(error.message ?: "Error al cargar negocios")
                        }
                    )
                },
                onFailure = { error ->
                    println("❌ Error al obtener usuario: ${error.message}")
                    _businessesState.value = UiState.Error(error.message ?: "Error al obtener usuario")
                }
            )
        }
    }

    fun isPro(): Boolean {
        return _isProUser.value
    }

    fun checkProStatus() {
        viewModelScope.launch {
            // 🚀 FASE 1: Verificar caché primero (0ms de delay)
            val cachedStatus = UserSession.getProStatus()
            if (cachedStatus != null) {
                _isProUser.value = cachedStatus
                _isProStatusChecked.value = true
                println("⚡ Estado Pro obtenido desde caché: $cachedStatus (0ms)")
                return@launch
            }

            // 📡 FASE 2: Si no hay caché válido, consultar BD
            _isProStatusChecked.value = false
            println("🔍 Caché no disponible, consultando BD...")
            val userResult = authRepository.getCurrentUser()
            userResult.fold(
                onSuccess = { user ->
                    val isPro = user?.isPro ?: false
                    _isProUser.value = isPro
                    _isProStatusChecked.value = true

                    // 💾 Actualizar caché para próximas consultas
                    UserSession.setProStatus(isPro)
                    user?.id?.let { UserSession.setCurrentBusinessId(it) }

                    println("✅ Estado Pro cargado desde BD: $isPro (300-500ms)")
                },
                onFailure = { error ->
                    println("❌ Error al verificar estado Pro: ${error.message}")
                    _isProUser.value = false
                    _isProStatusChecked.value = true
                }
            )
        }
    }

    fun markApprovalAsSeen(businessId: String) {
        viewModelScope.launch {
            println("👁️ Marcando aprobación como vista para negocio: $businessId")

            // ✅ Actualizar estado local INMEDIATAMENTE para evitar que aparezca de nuevo
            _businesses.value = _businesses.value.map { business ->
                if (business.id == businessId) {
                    business.copy(approvalSeen = true)
                } else {
                    business
                }
            }
            println("✅ Estado local actualizado (approvalSeen = true)")

            // ✅ Actualizar en BD
            val result = businessRepository.markApprovalAsSeen(businessId)
            result.fold(
                onSuccess = {
                    println("✅ Aprobación marcada como vista en BD")
                },
                onFailure = { error ->
                    println("❌ Error al marcar aprobación como vista: ${error.message}")
                    // ⚠️ Si falla BD, revertir estado local
                    _businesses.value = _businesses.value.map { business ->
                        if (business.id == businessId) {
                            business.copy(approvalSeen = false)
                        } else {
                            business
                        }
                    }
                }
            )
        }
    }

    // ✅ Suscribirse a cambios en tiempo real de negocios
    fun subscribeToBusinessChanges() {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: run {
                println("❌ No se puede suscribir: userId es null")
                return@launch
            }

            try {
                println("🔔 Iniciando suscripción Realtime para negocios del usuario: $userId")

                realtimeChannel = supabase.channel("businesses-$userId")

                val changeFlow = realtimeChannel?.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "businesses"
                    filter = "user_id=eq.$userId"
                }

                realtimeChannel?.subscribe()

                changeFlow?.collect { action ->
                    println("🔔 Evento Realtime recibido: ${action::class.simpleName}")
                    when (action) {
                        is PostgresAction.Update -> {
                            println("🔄 Negocio actualizado en Realtime, recargando...")
                            // ✅ Forzar recarga para actualizar UI inmediatamente
                            delay(100) // Pequeño delay para asegurar que BD está actualizada
                            loadAllUserBusinesses()
                        }
                        is PostgresAction.Insert -> {
                            println("🆕 Nuevo negocio insertado en Realtime, recargando...")
                            delay(100)
                            loadAllUserBusinesses()
                        }
                        is PostgresAction.Delete -> {
                            println("🗑️ Negocio eliminado en Realtime, recargando...")
                            delay(100)
                            loadAllUserBusinesses()
                        }
                        else -> {
                            println("ℹ️ Acción Realtime no manejada: $action")
                        }
                    }
                }

                println("✅ Suscripción Realtime activa")
            } catch (e: Exception) {
                println("❌ Error al suscribirse a Realtime: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // ✅ Desactivar negocio
    fun deactivateBusiness(businessId: String) {
        viewModelScope.launch {
            println("🗑️ Desactivando negocio: $businessId")
            val result = businessRepository.deactivateBusiness(businessId)
            result.fold(
                onSuccess = {
                    println("✅ Negocio desactivado exitosamente")
                    // ✅ Actualizar estado local INMEDIATAMENTE para UI instantánea
                    _businesses.value = _businesses.value.map { business ->
                        if (business.id == businessId) {
                            business.copy(isActive = false)
                        } else {
                            business
                        }
                    }
                    println("✅ UI actualizada localmente (isActive = false)")
                },
                onFailure = { error ->
                    println("❌ Error al desactivar negocio: ${error.message}")
                }
            )
        }
    }

    // ✅ Reactivar negocio
    fun reactivateBusiness(businessId: String) {
        viewModelScope.launch {
            println("🔄 Reactivando negocio: $businessId")
            val result = businessRepository.reactivateBusiness(businessId)
            result.fold(
                onSuccess = {
                    println("✅ Negocio reactivado exitosamente")
                    // ✅ Actualizar estado local INMEDIATAMENTE para UI instantánea
                    _businesses.value = _businesses.value.map { business ->
                        if (business.id == businessId) {
                            business.copy(isActive = true)
                        } else {
                            business
                        }
                    }
                    println("✅ UI actualizada localmente (isActive = true)")
                },
                onFailure = { error ->
                    println("❌ Error al reactivar negocio: ${error.message}")
                }
            )
        }
    }

    // ✅ Subir avatar del negocio
    fun uploadBusinessAvatar(file: java.io.File, businessId: String) {
        viewModelScope.launch {
            println("📸 Subiendo avatar para negocio: $businessId")
            val result = businessRepository.updateBusinessAvatar(file, businessId)
            result.fold(
                onSuccess = { avatarUrl ->
                    println("✅ Avatar de negocio actualizado: $avatarUrl")
                    // ✅ Actualizar estado local INMEDIATAMENTE
                    _businesses.value = _businesses.value.map { business ->
                        if (business.id == businessId) {
                            business.copy(avatarUrl = avatarUrl)
                        } else {
                            business
                        }
                    }
                    println("✅ UI actualizada con nuevo avatar")
                },
                onFailure = { error ->
                    println("❌ Error al subir avatar: ${error.message}")
                }
            )
        }
    }

    // ✅ Limpiar suscripción al destruir el ViewModel
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                realtimeChannel?.unsubscribe()
                println("✅ Suscripción Realtime limpiada")
            } catch (e: Exception) {
                println("❌ Error al limpiar suscripción: ${e.message}")
            }
        }
    }
}
