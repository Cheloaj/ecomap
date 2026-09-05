package com.ecomap.socio.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.socio.data.model.Business
import com.ecomap.socio.data.model.User
import com.ecomap.socio.domain.repository.AuthRepository
import com.ecomap.socio.domain.repository.BusinessRepository
import com.ecomap.socio.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val businessRepository: BusinessRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _business = MutableStateFlow<Business?>(null)
    val business: StateFlow<Business?> = _business.asStateFlow()

    private val _userState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val userState: StateFlow<UiState<User>> = _userState.asStateFlow()

    private val _businessState = MutableStateFlow<UiState<Business>>(UiState.Idle)
    val businessState: StateFlow<UiState<Business>> = _businessState.asStateFlow()

    private val _updateProfileState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val updateProfileState: StateFlow<UiState<Unit>> = _updateProfileState.asStateFlow()

    private val _uploadAvatarState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val uploadAvatarState: StateFlow<UiState<String>> = _uploadAvatarState.asStateFlow()

    private val _signOutState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val signOutState: StateFlow<UiState<Unit>> = _signOutState.asStateFlow()

    private val _deleteAccountState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val deleteAccountState: StateFlow<UiState<Unit>> = _deleteAccountState.asStateFlow()

    private val _changePasswordState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val changePasswordState: StateFlow<UiState<Unit>> = _changePasswordState.asStateFlow()

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                _userState.value = UiState.Loading
                println("📝 Cargando perfil de usuario...")

                val result = authRepository.getCurrentUser()
                result.fold(
                    onSuccess = { user ->
                        if (user != null) {
                            println("✅ Usuario cargado: ${user.email}")
                            _user.value = user
                            _userState.value = UiState.Success(user)
                            loadBusiness(user.id)
                        } else {
                            println("❌ getCurrentUser() retornó null")
                            _userState.value = UiState.Error("No se pudo obtener información del usuario")
                        }
                    },
                    onFailure = { error ->
                        println("❌ Error al cargar usuario: ${error.message}")
                        error.printStackTrace()
                        _userState.value = UiState.Error(error.message ?: "Error al cargar perfil")
                    }
                )
            } catch (e: Exception) {
                println("❌ Exception en loadUserProfile: ${e.message}")
                e.printStackTrace()
                _userState.value = UiState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    private fun loadBusiness(ownerId: String) {
        viewModelScope.launch {
            try {
                _businessState.value = UiState.Loading
                println("🏢 Cargando negocio para ownerId: $ownerId")

                val result = businessRepository.getBusinessByOwnerId(ownerId)
                result.fold(
                    onSuccess = { business ->
                        if (business != null) {
                            println("✅ Negocio cargado: ${business.businessName}")
                            _business.value = business
                            _businessState.value = UiState.Success(business)
                        } else {
                            println("⚠️ Usuario no tiene negocio registrado")
                            _business.value = null
                            _businessState.value = UiState.Idle  // Sin negocio no es error
                        }
                    },
                    onFailure = { error ->
                        println("❌ Error al cargar negocio: ${error.message}")
                        error.printStackTrace()
                        _business.value = null
                        _businessState.value = UiState.Error(error.message ?: "Error al cargar negocio")
                    }
                )
            } catch (e: Exception) {
                println("❌ Exception en loadBusiness: ${e.message}")
                e.printStackTrace()
                _businessState.value = UiState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    fun updateUserProfile(fullName: String) {
        viewModelScope.launch {
            try {
                _updateProfileState.value = UiState.Loading

                val currentUser = _user.value
                if (currentUser == null) {
                    println("❌ No hay usuario actual para actualizar")
                    _updateProfileState.value = UiState.Error("Usuario no encontrado")
                    return@launch
                }

                println("📝 Actualizando perfil de: ${currentUser.email}")

                val result = authRepository.updateUserProfile(currentUser.id, fullName)
                _updateProfileState.value = result.fold(
                    onSuccess = { updatedUser ->
                        println("✅ Perfil actualizado correctamente")
                        _user.value = updatedUser
                        UiState.Success(Unit)
                    },
                    onFailure = { error ->
                        println("❌ Error al actualizar perfil: ${error.message}")
                        UiState.Error(error.message ?: "Error al actualizar perfil")
                    }
                )
            } catch (e: Exception) {
                println("❌ Exception en updateUserProfile: ${e.message}")
                e.printStackTrace()
                _updateProfileState.value = UiState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    fun updateBusiness(business: Business) {
        viewModelScope.launch {
            try {
                _businessState.value = UiState.Loading
                println("🏢 Actualizando negocio: ${business.businessName}")

                val result = businessRepository.updateBusiness(business)
                _businessState.value = result.fold(
                    onSuccess = { updatedBusiness ->
                        println("✅ Negocio actualizado correctamente")
                        _business.value = updatedBusiness
                        UiState.Success(updatedBusiness)
                    },
                    onFailure = { error ->
                        println("❌ Error al actualizar negocio: ${error.message}")
                        UiState.Error(error.message ?: "Error al actualizar negocio")
                    }
                )
            } catch (e: Exception) {
                println("❌ Exception en updateBusiness: ${e.message}")
                e.printStackTrace()
                _businessState.value = UiState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    fun uploadAvatar(file: File) {
        viewModelScope.launch {
            try {
                _uploadAvatarState.value = UiState.Loading
                println("📸 Subiendo avatar...")

                val business = _business.value
                if (business == null) {
                    println("❌ No hay negocio para subir avatar")
                    _uploadAvatarState.value = UiState.Error("Negocio no encontrado")
                    return@launch
                }

                val result = businessRepository.updateBusinessAvatar(file, business.id)
                _uploadAvatarState.value = result.fold(
                    onSuccess = { avatarUrl ->
                        println("✅ Avatar subido correctamente: $avatarUrl")
                        // Actualizar el estado local del negocio con el nuevo avatar
                        val updatedBusiness = business.copy(avatarUrl = avatarUrl)
                        _business.value = updatedBusiness

                        // Recargar el perfil para obtener datos frescos de la BD
                        println("🔄 Recargando perfil para reflejar cambios...")
                        loadUserProfile()

                        UiState.Success(avatarUrl)
                    },
                    onFailure = { error ->
                        println("❌ Error al subir avatar: ${error.message}")
                        UiState.Error(error.message ?: "Error al subir avatar")
                    }
                )
            } catch (e: Exception) {
                println("❌ Exception en uploadAvatar: ${e.message}")
                e.printStackTrace()
                _uploadAvatarState.value = UiState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                _signOutState.value = UiState.Loading
                println("🚪 Cerrando sesión...")

                val result = authRepository.signOut()
                _signOutState.value = result.fold(
                    onSuccess = {
                        println("✅ Sesión cerrada correctamente")
                        _user.value = null
                        _business.value = null
                        UiState.Success(Unit)
                    },
                    onFailure = { error ->
                        println("❌ Error al cerrar sesión: ${error.message}")
                        UiState.Error(error.message ?: "Error al cerrar sesión")
                    }
                )
            } catch (e: Exception) {
                println("❌ Exception en signOut: ${e.message}")
                e.printStackTrace()
                _signOutState.value = UiState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            try {
                _changePasswordState.value = UiState.Loading
                println("🔑 Cambiando contraseña...")

                val result = authRepository.changePassword(currentPassword, newPassword)
                _changePasswordState.value = result.fold(
                    onSuccess = {
                        println("✅ Contraseña cambiada correctamente")
                        UiState.Success(Unit)
                    },
                    onFailure = { error ->
                        println("❌ Error al cambiar contraseña: ${error.message}")
                        UiState.Error(error.message ?: "Error al cambiar contraseña")
                    }
                )
            } catch (e: Exception) {
                println("❌ Exception en changePassword: ${e.message}")
                e.printStackTrace()
                _changePasswordState.value = UiState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            try {
                _deleteAccountState.value = UiState.Loading
                println("🗑️ Eliminando cuenta...")

                val currentUser = _user.value
                if (currentUser == null) {
                    println("❌ No hay usuario actual para eliminar")
                    _deleteAccountState.value = UiState.Error("Usuario no encontrado")
                    return@launch
                }

                val result = authRepository.deleteUserAccount(currentUser.id)
                _deleteAccountState.value = result.fold(
                    onSuccess = {
                        println("✅ Cuenta eliminada correctamente")
                        _user.value = null
                        _business.value = null
                        UiState.Success(Unit)
                    },
                    onFailure = { error ->
                        println("❌ Error al eliminar cuenta: ${error.message}")
                        UiState.Error(error.message ?: "Error al eliminar cuenta")
                    }
                )
            } catch (e: Exception) {
                println("❌ Exception en deleteAccount: ${e.message}")
                e.printStackTrace()
                _deleteAccountState.value = UiState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    fun resetUpdateProfileState() {
        _updateProfileState.value = UiState.Idle
    }

    fun resetUploadAvatarState() {
        _uploadAvatarState.value = UiState.Idle
    }

    fun resetSignOutState() {
        _signOutState.value = UiState.Idle
    }

    fun resetDeleteAccountState() {
        _deleteAccountState.value = UiState.Idle
    }

    fun resetChangePasswordState() {
        _changePasswordState.value = UiState.Idle
    }
}