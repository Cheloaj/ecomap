package com.ecomap.usuario.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.usuario.data.local.SecurePreferences
import com.ecomap.usuario.data.local.UserSession
import com.ecomap.usuario.data.model.User
import com.ecomap.usuario.domain.repository.AuthRepository
import com.ecomap.usuario.utils.BiometricHelper
import com.ecomap.usuario.utils.SubscriptionMonitor
import com.ecomap.usuario.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val securePreferences: SecurePreferences,
    private val application: Application,
    val subscriptionMonitor: SubscriptionMonitor
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Estados adicionales para flujo de autenticación
    private val _emailCheckState = MutableStateFlow<UiState<Boolean>>(UiState.Initial)
    val emailCheckState: StateFlow<UiState<Boolean>> = _emailCheckState.asStateFlow()

    private val _signInState = MutableStateFlow<UiState<User>>(UiState.Initial)
    val signInState: StateFlow<UiState<User>> = _signInState.asStateFlow()

    private val _verificationState = MutableStateFlow<UiState<Unit>>(UiState.Initial)
    val verificationState: StateFlow<UiState<Unit>> = _verificationState.asStateFlow()

    private val _resendCodeState = MutableStateFlow<UiState<Unit>>(UiState.Initial)
    val resendCodeState: StateFlow<UiState<Unit>> = _resendCodeState.asStateFlow()

    init {
        // NO cargar sesión automáticamente
        // La sesión NO debe persistir, siempre debe iniciar en login
        _isSignedIn.value = false
        _currentUser.value = null
        _authState.value = AuthUiState.Initial
    }

    fun signUp(email: String, password: String, fullName: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            authRepository.signUp(email, password, fullName)
                .onSuccess { user ->
                    _currentUser.value = user
                    _authState.value = AuthUiState.Success(user)
                    // NO marcar como signedIn hasta verificar el código OTP
                    _isSignedIn.value = false
                }
                .onFailure { error ->
                    _authState.value = AuthUiState.Error(error.message ?: "Error al registrarse")
                }
        }
    }

    fun verifyCode(email: String, code: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            authRepository.verifyCode(email, code)
                .onSuccess { user ->
                    _currentUser.value = user
                    _authState.value = AuthUiState.Success(user)
                    _isSignedIn.value = true

                    // Cachear información del usuario
                    UserSession.setCurrentUserId(user.id)
                    UserSession.setProStatus(user.isPro)

                    // Iniciar monitoreo de suscripción en tiempo real
                    subscriptionMonitor.startMonitoring(user.id, viewModelScope)

                    Log.d("AuthViewModel", "Email verificado exitosamente")
                }
                .onFailure { error ->
                    _authState.value = AuthUiState.Error(error.message ?: "Código inválido")
                }
        }
    }

    fun signIn(email: String, password: String, saveBiometric: Boolean = false) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "signIn - Email: $email, SaveBiometric: $saveBiometric")
            _signInState.value = UiState.Loading
            authRepository.signIn(email, password)
                .onSuccess { user ->
                    Log.d("AuthViewModel", "Login exitoso")
                    _currentUser.value = user
                    _signInState.value = UiState.Success(user)
                    _isSignedIn.value = true

                    // 💾 Cachear información del usuario inmediatamente
                    UserSession.setCurrentUserId(user.id)
                    UserSession.setProStatus(user.isPro)
                    Log.d("AuthViewModel", "UserSession actualizado: isPro=${user.isPro}")

                    // 🔴 Iniciar monitoreo de suscripción en tiempo real
                    subscriptionMonitor.startMonitoring(user.id, viewModelScope)
                    Log.d("AuthViewModel", "SubscriptionMonitor iniciado")

                    // Solo GUARDAR credenciales si saveBiometric = true
                    // NO borrar si saveBiometric = false (mantener las existentes)
                    if (saveBiometric) {
                        Log.d("AuthViewModel", "Guardando credenciales para biométrico")
                        securePreferences.saveCredentials(email, password)
                    } else {
                        Log.d("AuthViewModel", "Manteniendo credenciales existentes (no se modifican)")
                    }
                }
                .onFailure { error ->
                    Log.e("AuthViewModel", "Error en login: ${error.message}")
                    _signInState.value = UiState.Error(error.message ?: "Error al iniciar sesión")
                }
        }
    }

    fun getSavedCredentials(): Pair<String?, String?>? {
        val email = securePreferences.getEmail()
        val password = securePreferences.getPassword()
        return if (email != null && password != null) {
            Pair(email, password)
        } else {
            null
        }
    }

    fun isBiometricEnabled(): Boolean {
        return securePreferences.isBiometricEnabled()
    }

    fun saveBiometricCredentials(email: String, password: String) {
        securePreferences.saveCredentials(email, password)
    }

    fun clearBiometricCredentials() {
        securePreferences.clearCredentials()
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()

            // Limpiar credenciales biométricas (NO borra el email/password guardado, solo marca biometric_enabled = false)
            BiometricHelper.clearBiometricCredentials(application)

            // ⏹️ Detener monitoreo de suscripción
            subscriptionMonitor.stopMonitoring()
            Log.d("AuthViewModel", "SubscriptionMonitor detenido")

            // 🗑️ Limpiar caché de sesión
            UserSession.clear()
            Log.d("AuthViewModel", "UserSession limpiado en logout")

            _currentUser.value = null
            _authState.value = AuthUiState.Initial
            _isSignedIn.value = false
        }
    }

    fun clearError() {
        _authState.value = AuthUiState.Initial
    }

    fun resetPassword(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            authRepository.resetPassword(email)
                .onSuccess {
                    onResult(true, "Se ha enviado un correo de recuperación a $email")
                }
                .onFailure { error ->
                    onResult(false, error.message ?: "Error al enviar el correo")
                }
        }
    }

    fun updatePassword(newPassword: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            authRepository.updatePassword(newPassword)
                .onSuccess {
                    // Cerrar sesión después de actualizar la contraseña
                    authRepository.signOut()
                    _currentUser.value = null
                    _authState.value = AuthUiState.Initial
                    _isSignedIn.value = false
                    onResult(true, "Contraseña actualizada exitosamente")
                }
                .onFailure { error ->
                    onResult(false, error.message ?: "Error al actualizar la contraseña")
                }
        }
    }

    /**
     * Verifica si hay una sesión activa en Supabase.
     * Esta función se llama desde SplashScreen para decidir la navegación inicial.
     */
    fun checkUserLoggedIn() {
        viewModelScope.launch {
            Log.d("AuthViewModel", "🔍 checkUserLoggedIn - Verificando sesión...")
            authRepository.getCurrentUser()
                .onSuccess { user ->
                    if (user != null) {
                        Log.d("AuthViewModel", "✅ Sesión activa encontrada: ${user.email}")
                        _currentUser.value = user
                        _isSignedIn.value = true
                        UserSession.setCurrentUserId(user.id)
                        UserSession.setProStatus(user.isPro)

                        // 🔴 Iniciar monitoreo de suscripción para sesión restaurada
                        subscriptionMonitor.startMonitoring(user.id, viewModelScope)
                        Log.d("AuthViewModel", "SubscriptionMonitor iniciado para sesión restaurada")
                    } else {
                        Log.d("AuthViewModel", "❌ No hay sesión activa")
                        _currentUser.value = null
                        _isSignedIn.value = false
                    }
                }
                .onFailure { error ->
                    Log.e("AuthViewModel", "❌ Error al verificar sesión: ${error.message}")
                    _currentUser.value = null
                    _isSignedIn.value = false
                }
        }
    }

    /**
     * Resetea el estado de autenticación después de un error.
     * Se usa en QuickLoginScreen para limpiar errores.
     */
    fun resetSignInState() {
        _signInState.value = UiState.Initial
    }

    /**
     * Verifica si un email existe en la base de datos.
     * Usado en LoginEmailScreen para validar antes de avanzar al paso de contraseña.
     */
    fun checkEmailExists(email: String) {
        viewModelScope.launch {
            _emailCheckState.value = UiState.Loading
            try {
                authRepository.checkEmailExists(email)
                    .onSuccess { exists ->
                        _emailCheckState.value = UiState.Success(exists)
                    }
                    .onFailure { error ->
                        _emailCheckState.value = UiState.Error(error.message ?: "Error al verificar email")
                    }
            } catch (e: Exception) {
                _emailCheckState.value = UiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Resetea el estado de verificación de email.
     */
    fun resetEmailCheckState() {
        _emailCheckState.value = UiState.Initial
    }

    /**
     * Verifica el email del usuario con el código OTP.
     * Usado en ForgotPasswordCodeScreen.
     */
    fun verifyEmail(email: String, code: String) {
        viewModelScope.launch {
            _verificationState.value = UiState.Loading
            try {
                authRepository.verifyCode(email, code)
                    .onSuccess {
                        _verificationState.value = UiState.Success(Unit)
                    }
                    .onFailure { error ->
                        _verificationState.value = UiState.Error(error.message ?: "Código inválido")
                    }
            } catch (e: Exception) {
                _verificationState.value = UiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Resetea el estado de verificación.
     */
    fun resetVerificationState() {
        _verificationState.value = UiState.Initial
    }

    /**
     * Reenvía el código de verificación al email del usuario.
     */
    fun resendVerificationCode(email: String) {
        viewModelScope.launch {
            _resendCodeState.value = UiState.Loading
            try {
                authRepository.resendVerificationCode(email)
                    .onSuccess {
                        _resendCodeState.value = UiState.Success(Unit)
                    }
                    .onFailure { error ->
                        _resendCodeState.value = UiState.Error(error.message ?: "Error al reenviar código")
                    }
            } catch (e: Exception) {
                _resendCodeState.value = UiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Resetea el estado de reenvío de código.
     */
    fun resetResendCodeState() {
        _resendCodeState.value = UiState.Initial
    }

    /**
     * Resetea la contraseña del usuario usando el código de verificación.
     * Automáticamente inicia sesión después de resetear la contraseña.
     */
    fun resetPassword(email: String, verificationCode: String, newPassword: String) {
        viewModelScope.launch {
            _signInState.value = UiState.Loading
            try {
                // Primero actualizar la contraseña
                authRepository.resetPasswordWithCode(email, verificationCode, newPassword)
                    .onSuccess {
                        // Luego iniciar sesión automáticamente con la nueva contraseña
                        authRepository.signIn(email, newPassword)
                            .onSuccess { user ->
                                _currentUser.value = user
                                _signInState.value = UiState.Success(user)
                                _isSignedIn.value = true

                                // Cachear información del usuario
                                UserSession.setCurrentUserId(user.id)
                                UserSession.setProStatus(user.isPro)

                                // Iniciar monitoreo de suscripción
                                subscriptionMonitor.startMonitoring(user.id, viewModelScope)
                            }
                            .onFailure { error ->
                                _signInState.value = UiState.Error(error.message ?: "Error al iniciar sesión")
                            }
                    }
                    .onFailure { error ->
                        _signInState.value = UiState.Error(error.message ?: "Error al resetear contraseña")
                    }
            } catch (e: Exception) {
                _signInState.value = UiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}
