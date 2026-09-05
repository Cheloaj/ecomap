package com.ecomap.socio.presentation.viewmodel

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.socio.data.local.UserSession
import com.ecomap.socio.data.model.User
import com.ecomap.socio.domain.repository.AuthRepository
import com.ecomap.socio.domain.usecase.SignInUseCase
import com.ecomap.socio.domain.usecase.SignUpUseCase
import com.ecomap.socio.domain.usecase.VerifyEmailUseCase
import com.ecomap.socio.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase,
    private val signInUseCase: SignInUseCase,
    private val verifyEmailUseCase: VerifyEmailUseCase,
    private val authRepository: AuthRepository,
    private val subscriptionMonitor: com.ecomap.socio.utils.SubscriptionMonitor,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _signUpState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val signUpState: StateFlow<UiState<User>> = _signUpState.asStateFlow()

    private val _signInState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val signInState: StateFlow<UiState<User>> = _signInState.asStateFlow()

    private val _verificationState = MutableStateFlow<UiState<Boolean>>(UiState.Idle)
    val verificationState: StateFlow<UiState<Boolean>> = _verificationState.asStateFlow()

    private val _resendCodeState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val resendCodeState: StateFlow<UiState<String>> = _resendCodeState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _emailCheckState = MutableStateFlow<UiState<Boolean>>(UiState.Idle)
    val emailCheckState: StateFlow<UiState<Boolean>> = _emailCheckState.asStateFlow()

    // 🔴 Exponer estado de suscripción en tiempo real
    val isProStatus: StateFlow<Boolean> = subscriptionMonitor.isProStatus

    // --- NUEVAS VARIABLES PARA UBICACIÓN ---
    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val _address = MutableStateFlow<String>("")
    val address: StateFlow<String> = _address.asStateFlow()
    // --- FIN DE NUEVAS VARIABLES ---

    fun signUp(email: String, password: String, confirmPassword: String, fullName: String) {
        viewModelScope.launch {
            _signUpState.value = UiState.Loading

            if (password != confirmPassword) {
                _signUpState.value = UiState.Error("Las contraseñas no coinciden")
                return@launch
            }

            val result = signUpUseCase(email, password, fullName)
            _signUpState.value = result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    UiState.Success(user)
                },
                onFailure = { error ->
                    UiState.Error(error.message ?: "Error al registrar usuario")
                }
            )
        }
    }

    fun signIn(email: String, password: String, rememberMe: Boolean) {
        viewModelScope.launch {
            println("🔐 AuthViewModel.signIn: Iniciando")
            println("   Email: $email")
            println("   RememberMe: $rememberMe")
            _signInState.value = UiState.Loading

            val result = signInUseCase(email, password)
            _signInState.value = result.fold(
                onSuccess = { user ->
                    println("✅ AuthViewModel.signIn: Login exitoso")
                    println("   User ID: ${user.id}")
                    println("   Status: ${user.accountStatus}")
                    println("   Onboarding: ${user.onboardingStep}")
                    _currentUser.value = user

                    // 💾 Cachear información del usuario inmediatamente
                    UserSession.setCurrentBusinessId(user.id)
                    UserSession.setProStatus(user.isPro)
                    println("💾 UserSession actualizado: isPro=${user.isPro}")

                    // 🔴 Iniciar monitoreo de suscripción en tiempo real
                    subscriptionMonitor.startMonitoring(user.id, viewModelScope)
                    println("🔴 Monitoreo de suscripción iniciado para: ${user.id}")

                    // TODO: Guardar estado "rememberMe" en DataStore/SharedPreferences

                    UiState.Success(user)
                },
                onFailure = { error ->
                    println("❌ AuthViewModel.signIn: Error - ${error.message}")
                    UiState.Error(error.message ?: "Error al iniciar sesión")
                }
            )
            println("🔐 AuthViewModel.signIn: Estado final = ${_signInState.value}")
        }
    }

    fun verifyEmail(email: String, code: String) {
        viewModelScope.launch {
            println("🔍 AuthViewModel: Iniciando verificación de email: $email")
            _verificationState.value = UiState.Loading

            val result = verifyEmailUseCase(email, code)
            _verificationState.value = result.fold(
                onSuccess = { isValid ->
                    if (isValid) {
                        println("✅ AuthViewModel: Email verificado correctamente")
                        // Recargar el usuario desde la base de datos para obtener los datos actualizados
                        viewModelScope.launch {
                            println("📝 AuthViewModel: Recargando usuario desde BD...")
                            authRepository.getCurrentUser().fold(
                                onSuccess = { user ->
                                    _currentUser.value = user
                                    println("✅ AuthViewModel: Usuario recargado - ID: ${user?.id}")
                                },
                                onFailure = { error ->
                                    println("❌ AuthViewModel: Error al recargar usuario: ${error.message}")
                                }
                            )
                        }
                        UiState.Success(true)
                    } else {
                        println("❌ AuthViewModel: Código inválido o expirado")
                        UiState.Error("Código inválido o expirado")
                    }
                },
                onFailure = { error ->
                    println("❌ AuthViewModel: Error al verificar email: ${error.message}")
                    UiState.Error(error.message ?: "Error al verificar código")
                }
            )
        }
    }

    fun resendVerificationCode(email: String) {
        viewModelScope.launch {
            println("🔄 AuthViewModel: Reenviando código de verificación a: $email")
            _resendCodeState.value = UiState.Loading

            val result = authRepository.sendVerificationCode(email)
            _resendCodeState.value = result.fold(
                onSuccess = { code ->
                    println("✅ AuthViewModel: Código reenviado exitosamente")
                    println("================================")
                    println("NUEVO CÓDIGO: $code")
                    println("Email: $email")
                    println("================================")
                    // Reset verification state para permitir nuevo intento
                    _verificationState.value = UiState.Idle
                    UiState.Success(code)
                },
                onFailure = { error ->
                    println("❌ AuthViewModel: Error al reenviar código: ${error.message}")
                    UiState.Error(error.message ?: "Error al reenviar código")
                }
            )
        }
    }

    // --- NUEVO MÉTODO PARA UBICACIÓN ---
    fun setLocation(latitude: Double, longitude: Double) {
        val newLocation = Location("").apply {
            this.latitude = latitude
            this.longitude = longitude
        }
        _location.value = newLocation

        // Obtener dirección usando Geocoder
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Usamos el 'context' que ya inyectaste con Hilt
                val geocoder = Geocoder(context, Locale.getDefault())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            val address = addresses[0]
                            val addressText = buildString {
                                address.thoroughfare?.let { append("$it ") }
                                address.subThoroughfare?.let { append("$it, ") }
                                address.locality?.let { append("$it, ") }
                                address.adminArea?.let { append(it) }
                            }
                            _address.value = addressText.ifBlank { "Ubicación seleccionada" }
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val addressText = buildString {
                            address.thoroughfare?.let { append("$it ") }
                            address.subThoroughfare?.let { append("$it, ") }
                            address.locality?.let { append("$it, ") }
                            address.adminArea?.let { append(it) }
                        }
                        _address.value = addressText.ifBlank { "Ubicación seleccionada" }
                    }
                }
            } catch (e: Exception) {
                _address.value = "Ubicación: $latitude, $longitude"
            }
        }
    }
    // --- FIN DE NUEVO MÉTODO ---

    fun checkUserLoggedIn() {
        viewModelScope.launch {
            val isLoggedIn = authRepository.isUserLoggedIn()
            if (isLoggedIn) {
                authRepository.getCurrentUser().fold(
                    onSuccess = { user ->
                        _currentUser.value = user
                    },
                    onFailure = {}
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _currentUser.value = null

            // 🛑 Detener monitoreo de suscripción
            subscriptionMonitor.stopMonitoring()
            println("🛑 Monitoreo de suscripción detenido")

            // 🗑️ Limpiar caché de sesión
            UserSession.clear()
            println("🗑️ UserSession limpiado en logout")

            // Resetear estados para limpiar cualquier resultado de login previo
            _signInState.value = UiState.Idle
            _signUpState.value = UiState.Idle
        }
    }

    fun resetSignUpState() {
        _signUpState.value = UiState.Idle
    }

    fun resetSignInState() {
        _signInState.value = UiState.Idle
    }

    fun resetVerificationState() {
        _verificationState.value = UiState.Idle
    }

    fun resetResendCodeState() {
        _resendCodeState.value = UiState.Idle
    }

    fun checkEmailExists(email: String) {
        viewModelScope.launch {
            println("🔍 AuthViewModel: Verificando si email existe: $email")
            _emailCheckState.value = UiState.Loading

            val result = authRepository.checkEmailExists(email)
            _emailCheckState.value = result.fold(
                onSuccess = { exists ->
                    println("✅ AuthViewModel: Email existe: $exists")
                    UiState.Success(exists)
                },
                onFailure = { error ->
                    println("❌ AuthViewModel: Error al verificar email: ${error.message}")
                    UiState.Error(error.message ?: "Error al verificar email")
                }
            )
        }
    }

    fun resetEmailCheckState() {
        _emailCheckState.value = UiState.Idle
    }

    fun resetPassword(email: String, verificationCode: String, newPassword: String) {
        viewModelScope.launch {
            println("🔐 AuthViewModel: Iniciando reset de contraseña")
            _signInState.value = UiState.Loading

            val result = authRepository.resetPassword(email, verificationCode, newPassword)
            result.fold(
                onSuccess = {
                    println("✅ AuthViewModel: Contraseña reseteada exitosamente")
                    // Hacer login automático con la nueva contraseña
                    println("🔐 Haciendo login automático con nueva contraseña...")
                    signIn(email, newPassword, true)
                },
                onFailure = { error ->
                    println("❌ AuthViewModel: Error al resetear contraseña: ${error.message}")
                    _signInState.value = UiState.Error(error.message ?: "Error al resetear contraseña")
                }
            )
        }
    }
}

