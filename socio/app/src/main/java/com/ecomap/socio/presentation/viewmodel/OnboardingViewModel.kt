package com.ecomap.socio.presentation.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.socio.data.model.Business
import com.ecomap.socio.data.model.DaySchedule
import com.ecomap.socio.data.model.OperatingHours
import com.ecomap.socio.domain.repository.BusinessRepository
import com.ecomap.socio.domain.usecase.CreateBusinessUseCase
import com.ecomap.socio.domain.usecase.GetCurrentLocationUseCase
import com.ecomap.socio.domain.usecase.ReverseGeocodeUseCase
import com.ecomap.socio.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val createBusinessUseCase: CreateBusinessUseCase,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase,
    private val reverseGeocodeUseCase: ReverseGeocodeUseCase,
    private val businessRepository: BusinessRepository,
    private val authRepository: com.ecomap.socio.domain.repository.AuthRepository,
    private val application: Application
) : ViewModel() {

    private val prefs = application.getSharedPreferences("onboarding_prefs", 0)

    private val _currentStep = MutableStateFlow(loadSavedStep())
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _businessName = MutableStateFlow(loadSavedBusinessName())
    val businessName: StateFlow<String> = _businessName.asStateFlow()

    private val _businessType = MutableStateFlow(loadSavedBusinessType())
    val businessType: StateFlow<String> = _businessType.asStateFlow()

    private val _location = MutableStateFlow<Location?>(loadSavedLocation())
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val _address = MutableStateFlow(loadSavedAddress())
    val address: StateFlow<String> = _address.asStateFlow()

    private val _operatingHours = MutableStateFlow(loadSavedOperatingHours())
    val operatingHours: StateFlow<OperatingHours> = _operatingHours.asStateFlow()

    private val _locationState = MutableStateFlow<UiState<Location>>(UiState.Idle)
    val locationState: StateFlow<UiState<Location>> = _locationState.asStateFlow()

    private val _createBusinessState = MutableStateFlow<UiState<Business>>(UiState.Idle)
    val createBusinessState: StateFlow<UiState<Business>> = _createBusinessState.asStateFlow()

    private val _uploadDocumentState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val uploadDocumentState: StateFlow<UiState<String>> = _uploadDocumentState.asStateFlow()

    // ===== FUNCIONES DE CARGA =====
    private fun loadSavedStep(): Int {
        val savedStep = prefs.getInt("current_step", 0)
        println("📂 Cargando paso guardado: $savedStep")
        return savedStep
    }

    private fun loadSavedBusinessName(): String {
        val name = prefs.getString("business_name", "") ?: ""
        println("📂 Cargando nombre del negocio: $name")
        return name
    }

    private fun loadSavedBusinessType(): String {
        val type = prefs.getString("business_type", "") ?: ""
        println("📂 Cargando tipo de negocio: $type")
        return type
    }

    private fun loadSavedLocation(): Location? {
        val lat = prefs.getFloat("location_latitude", 0f)
        val lon = prefs.getFloat("location_longitude", 0f)

        return if (lat != 0f && lon != 0f) {
            Location("").apply {
                latitude = lat.toDouble()
                longitude = lon.toDouble()
            }.also {
                println("📂 Cargando ubicación: $lat, $lon")
            }
        } else {
            println("📂 No hay ubicación guardada, usando Ciudad del Carmen por defecto")
            Location("").apply {
                latitude = 18.6367
                longitude = -91.8340
            }
        }
    }

    private fun loadSavedAddress(): String {
        val addr = prefs.getString("address", "") ?: ""
        println("📂 Cargando dirección: $addr")
        return addr
    }

    private fun loadSavedOperatingHours(): OperatingHours {
        val json = prefs.getString("operating_hours", null)
        return if (json != null) {
            try {
                Json.decodeFromString<OperatingHours>(json).also {
                    println("📂 Cargando horarios guardados")
                }
            } catch (e: Exception) {
                println("⚠️ Error al cargar horarios: ${e.message}")
                OperatingHours()
            }
        } else {
            println("📂 No hay horarios guardados")
            OperatingHours()
        }
    }

    // ===== FUNCIONES DE GUARDADO =====
    private fun saveCurrentStep(step: Int) {
        prefs.edit().putInt("current_step", step).apply()
        println("💾 Paso guardado: $step")
    }

    private fun saveBusinessName(name: String) {
        prefs.edit().putString("business_name", name).apply()
        println("💾 Nombre guardado: $name")
    }

    private fun saveBusinessType(type: String) {
        prefs.edit().putString("business_type", type).apply()
        println("💾 Tipo guardado: $type")
    }

    private fun saveLocation(location: Location) {
        prefs.edit()
            .putFloat("location_latitude", location.latitude.toFloat())
            .putFloat("location_longitude", location.longitude.toFloat())
            .apply()
        println("💾 Ubicación guardada: ${location.latitude}, ${location.longitude}")
    }

    private fun saveAddress(address: String) {
        prefs.edit().putString("address", address).apply()
        println("💾 Dirección guardada: $address")
    }

    private fun saveOperatingHours(hours: OperatingHours) {
        val json = Json.encodeToString(hours)
        prefs.edit().putString("operating_hours", json).apply()
        println("💾 Horarios guardados")
    }

    private fun clearAllOnboardingData() {
        prefs.edit()
            .remove("current_step")
            .remove("business_name")
            .remove("business_type")
            .remove("location_latitude")
            .remove("location_longitude")
            .remove("address")
            .remove("operating_hours")
            .apply()
        println("🗑️ Todos los datos de onboarding limpiados")
    }

    fun nextStep() {
        if (_currentStep.value < 3) {
            println("🚀 Avanzando al paso ${_currentStep.value + 1}")
            _currentStep.value += 1
            saveCurrentStep(_currentStep.value)
        }
    }

    fun previousStep() {
        if (_currentStep.value > 0) {
            println("⬅️ Regresando al paso ${_currentStep.value - 1}")
            _currentStep.value -= 1
            saveCurrentStep(_currentStep.value)
        }
    }

    fun updateBusinessName(name: String) {
        _businessName.value = name
        saveBusinessName(name)
        println("📝 Nombre del negocio actualizado: $name")
    }

    fun updateBusinessType(type: String) {
        _businessType.value = type
        saveBusinessType(type)
        println("📝 Tipo de negocio actualizado: $type")
    }

    fun updateLocation(latitude: Double, longitude: Double) {
        val location = Location("").apply {
            this.latitude = latitude
            this.longitude = longitude
        }
        _location.value = location
        saveLocation(location)
        println("📍 Ubicación actualizada: $latitude, $longitude")

        viewModelScope.launch {
            val result = reverseGeocodeUseCase(latitude, longitude)
            result.onSuccess { address ->
                _address.value = address
                saveAddress(address)
                println("📍 Dirección actualizada: $address")
            }.onFailure { error ->
                println("❌ Error en geocodificación inversa: ${error.message}")
            }
        }
    }

    fun getCurrentLocation() {
        viewModelScope.launch {
            _locationState.value = UiState.Loading
            println("📍 Obteniendo ubicación actual...")

            val result = getCurrentLocationUseCase()
            _locationState.value = result.fold(
                onSuccess = { location ->
                    _location.value = location
                    saveLocation(location)
                    println("📍 Ubicación actual obtenida: ${location.latitude}, ${location.longitude}")

                    reverseGeocodeUseCase(location.latitude, location.longitude).onSuccess { address ->
                        _address.value = address
                        saveAddress(address)
                        println("📍 Dirección desde geocodificación: $address")
                    }.onFailure { error ->
                        println("❌ Error en geocodificación inversa: ${error.message}")
                    }

                    UiState.Success(location)
                },
                onFailure = { error ->
                    println("❌ Error al obtener ubicación: ${error.message}")
                    UiState.Error(error.message ?: "Error al obtener ubicación")
                }
            )
        }
    }

    fun updateOperatingHours(day: String, schedule: DaySchedule) {
        _operatingHours.value = when (day.lowercase()) {
            "monday", "lunes" -> _operatingHours.value.copy(monday = schedule)
            "tuesday", "martes" -> _operatingHours.value.copy(tuesday = schedule)
            "wednesday", "miércoles" -> _operatingHours.value.copy(wednesday = schedule)
            "thursday", "jueves" -> _operatingHours.value.copy(thursday = schedule)
            "friday", "viernes" -> _operatingHours.value.copy(friday = schedule)
            "saturday", "sábado" -> _operatingHours.value.copy(saturday = schedule)
            "sunday", "domingo" -> _operatingHours.value.copy(sunday = schedule)
            else -> _operatingHours.value
        }
        saveOperatingHours(_operatingHours.value)
        println("⏰ Horarios actualizados para $day: $schedule")
    }

    fun setOperatingHours(newOperatingHours: OperatingHours) {
        _operatingHours.value = newOperatingHours
        saveOperatingHours(newOperatingHours)
        println("⏰ Horarios completos actualizados: $newOperatingHours")
    }

    fun createBusiness(userId: String, isFirstBusiness: Boolean = false) {
        viewModelScope.launch {
            _createBusinessState.value = UiState.Loading
            println("🏢 Creando negocio para usuario: $userId (primer negocio: $isFirstBusiness)")

            val location = _location.value
            if (location == null) {
                _createBusinessState.value = UiState.Error("Debe seleccionar una ubicación")
                println("❌ No se seleccionó ubicación")
                return@launch
            }

            val operatingHoursJson = Json.encodeToString(_operatingHours.value)

            val business = Business(
                id = "",
                userId = userId,
                businessName = _businessName.value,
                businessType = _businessType.value,
                latitude = location.latitude,
                longitude = location.longitude,
                address = _address.value,
                operatingHours = operatingHoursJson,
                verificationStatus = "pending" // ✅ Todos los negocios requieren aprobación del admin
            )

            println("📋 Detalles del negocio:")
            println("   - Nombre: ${_businessName.value}")
            println("   - Tipo: ${_businessType.value}")
            println("   - Ubicación: ${location.latitude}, ${location.longitude}")
            println("   - Dirección: ${_address.value}")

            val result = createBusinessUseCase(business)

            result.fold(
                onSuccess = { createdBusiness ->
                    println("✅ Negocio creado exitosamente")

                    // ✅ Solo actualizar onboarding_step si es el PRIMER negocio
                    if (isFirstBusiness) {
                        println("📝 Es primer negocio - Actualizando onboarding_step a 'document_upload'...")

                        val updateResult = authRepository.updateOnboardingStep(userId, "document_upload")

                        updateResult.fold(
                            onSuccess = {
                                println("✅ onboarding_step actualizado a 'document_upload'")
                                clearAllOnboardingData() // ✅ Limpiar TODOS los datos al completar onboarding
                                _createBusinessState.value = UiState.Success(createdBusiness)
                            },
                            onFailure = { error ->
                                println("❌ Error al actualizar onboarding_step: ${error.message}")
                                clearAllOnboardingData() // ✅ Limpiar TODOS los datos aunque falle la actualización
                                _createBusinessState.value = UiState.Success(createdBusiness)
                            }
                        )
                    } else {
                        println("📝 NO es primer negocio - NO se actualiza onboarding_step")
                        clearAllOnboardingData() // ✅ Limpiar datos de onboarding
                        _createBusinessState.value = UiState.Success(createdBusiness)
                    }
                },
                onFailure = { error ->
                    println("❌ Error al crear negocio: ${error.message}")
                    _createBusinessState.value = UiState.Error(error.message ?: "Error al crear negocio")
                }
            )
        }
    }

    fun uploadVerificationDocument(file: File, userId: String) {
        viewModelScope.launch {
            _uploadDocumentState.value = UiState.Loading
            println("📤 Iniciando subida de documento para usuario: $userId")

            val result = businessRepository.uploadVerificationDocument(file, userId)

            result.fold(
                onSuccess = { url ->
                    println("✅ Documento subido exitosamente: $url")
                    println("📝 Actualizando estado de verificación del usuario...")

                    val updateResult = authRepository.updateUserVerificationStatus(
                        userId = userId,
                        onboardingStep = "completed",  // ✅ Cambiar a "completed" (valor válido)
                        accountStatus = "pending_verification"
                    )

                    updateResult.fold(
                        onSuccess = {
                            println("✅ Estado de verificación actualizado: onboarding_step y account_status = pending_verification")
                            _uploadDocumentState.value = UiState.Success(url)
                        },
                        onFailure = { error ->
                            println("❌ Error al actualizar estado de verificación: ${error.message}")
                            _uploadDocumentState.value = UiState.Success(url)
                        }
                    )
                },
                onFailure = { error ->
                    println("❌ Error al subir documento: ${error.message}")
                    _uploadDocumentState.value = UiState.Error(error.message ?: "Error al subir documento")
                }
            )
        }
    }

    fun canProceedStep1(): Boolean {
        val canProceed = _businessName.value.length >= 3
        println("🔍 Puede avanzar paso 1: $canProceed (businessName: ${_businessName.value})")
        return canProceed
    }

    fun canProceedStep2(): Boolean {
        val canProceed = _businessType.value.isNotBlank()
        println("🔍 Puede avanzar paso 2: $canProceed (businessType: ${_businessType.value})")
        return canProceed
    }

    fun canProceedStep3(): Boolean {
        val canProceed = _location.value != null && _address.value.isNotBlank()
        println("🔍 Puede avanzar paso 3: $canProceed (location: ${_location.value}, address: ${_address.value})")
        return canProceed
    }

    fun hasOperatingHours(): Boolean {
        val hours = _operatingHours.value
        val hasAtLeastOneDay = listOf(
            hours.monday.isOpen,
            hours.tuesday.isOpen,
            hours.wednesday.isOpen,
            hours.thursday.isOpen,
            hours.friday.isOpen,
            hours.saturday.isOpen,
            hours.sunday.isOpen
        ).any { it }
        println("🔍 Tiene horarios configurados: $hasAtLeastOneDay")
        return hasAtLeastOneDay
    }

    fun resetCreateBusinessState() {
        // Resetear el estado de la operación
        _createBusinessState.value = UiState.Idle

        // Resetear el paso actual
        _currentStep.value = 0

        // Limpiar todos los campos del formulario
        _businessName.value = ""
        _businessType.value = ""
        _address.value = ""
        _location.value = Location("").apply {
            latitude = 18.6367  // Ciudad del Carmen
            longitude = -91.8340
        }
        _operatingHours.value = OperatingHours()

        // Limpiar SharedPreferences
        prefs.edit()
            .remove("current_step")
            .remove("business_name")
            .remove("business_type")
            .remove("address")
            .remove("location_latitude")
            .remove("location_longitude")
            .remove("operating_hours")
            .apply()

        println("🔄 Todos los datos del formulario y caché limpiados")
    }
}