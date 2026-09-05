package com.ecomap.usuario.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.usuario.data.model.Business
import com.ecomap.usuario.data.model.Notification
import com.ecomap.usuario.data.model.UserPreferences
import com.ecomap.usuario.domain.repository.UserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UserDataUiState {
    object Loading : UserDataUiState()
    data class Success(val data: Any) : UserDataUiState()
    data class Error(val message: String) : UserDataUiState()
}

@HiltViewModel
class UserDataViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Favoritos
    private val _favoritesState = MutableStateFlow<List<Business>>(emptyList())
    val favoritesState: StateFlow<List<Business>> = _favoritesState.asStateFlow()

    private val _isFavorite = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val isFavorite: StateFlow<Map<String, Boolean>> = _isFavorite.asStateFlow()

    // Historial
    private val _historyState = MutableStateFlow<List<Business>>(emptyList())
    val historyState: StateFlow<List<Business>> = _historyState.asStateFlow()

    // Notificaciones
    private val _notificationsState = MutableStateFlow<List<Notification>>(emptyList())
    val notificationsState: StateFlow<List<Notification>> = _notificationsState.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    // Preferencias
    private val _userPreferences = MutableStateFlow<UserPreferences?>(null)
    val userPreferences: StateFlow<UserPreferences?> = _userPreferences.asStateFlow()

    // Estado de operaciones
    private val _operationState = MutableStateFlow<UserDataUiState?>(null)
    val operationState: StateFlow<UserDataUiState?> = _operationState.asStateFlow()

    init {
        loadFavorites()
        loadHistory()
        loadNotifications()
        loadUserPreferences()
        observeUnreadCount()
    }

    // ===== FAVORITOS =====
    private fun loadFavorites() {
        viewModelScope.launch {
            userDataRepository.getFavorites().collect { businesses ->
                _favoritesState.value = businesses
            }
        }
    }

    fun toggleFavorite(businessId: String) {
        viewModelScope.launch {
            val isFav = _isFavorite.value[businessId] ?: false

            if (isFav) {
                userDataRepository.removeFavorite(businessId)
                    .onSuccess {
                        _isFavorite.value = _isFavorite.value.toMutableMap().apply {
                            this[businessId] = false
                        }
                        loadFavorites()
                    }
            } else {
                userDataRepository.addFavorite(businessId)
                    .onSuccess {
                        _isFavorite.value = _isFavorite.value.toMutableMap().apply {
                            this[businessId] = true
                        }
                        loadFavorites()
                    }
            }
        }
    }

    fun checkIfFavorite(businessId: String) {
        viewModelScope.launch {
            userDataRepository.isFavorite(businessId)
                .onSuccess { isFav ->
                    _isFavorite.value = _isFavorite.value.toMutableMap().apply {
                        this[businessId] = isFav
                    }
                }
        }
    }

    // ===== HISTORIAL =====
    private fun loadHistory() {
        viewModelScope.launch {
            userDataRepository.getHistory().collect { businesses ->
                _historyState.value = businesses
            }
        }
    }

    fun addToHistory(businessId: String) {
        viewModelScope.launch {
            userDataRepository.addToHistory(businessId)
            loadHistory()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            _operationState.value = UserDataUiState.Loading
            userDataRepository.clearHistory()
                .onSuccess {
                    _operationState.value = UserDataUiState.Success("Historial limpiado")
                    loadHistory()
                }
                .onFailure { error ->
                    _operationState.value = UserDataUiState.Error(error.message ?: "Error al limpiar historial")
                }
        }
    }

    // ===== NOTIFICACIONES =====
    private fun loadNotifications() {
        viewModelScope.launch {
            userDataRepository.getNotifications().collect { notifications ->
                _notificationsState.value = notifications
            }
        }
    }

    private fun observeUnreadCount() {
        viewModelScope.launch {
            userDataRepository.getUnreadCount().collect { count ->
                _unreadCount.value = count
            }
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            userDataRepository.markAsRead(notificationId)
            loadNotifications()
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            userDataRepository.deleteNotification(notificationId)
            loadNotifications()
        }
    }

    // ===== PREFERENCIAS =====
    fun loadUserPreferences() {
        viewModelScope.launch {
            userDataRepository.getUserPreferences()
                .onSuccess { prefs ->
                    _userPreferences.value = prefs
                }
        }
    }

    fun updateDisplayName(displayName: String) {
        viewModelScope.launch {
            _operationState.value = UserDataUiState.Loading
            userDataRepository.updateDisplayName(displayName)
                .onSuccess {
                    _operationState.value = UserDataUiState.Success("Nombre actualizado")
                    loadUserPreferences()
                }
                .onFailure { error ->
                    _operationState.value = UserDataUiState.Error(error.message ?: "Error al actualizar nombre")
                }
        }
    }

    fun updateDefaultLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            userDataRepository.updateDefaultLocation(latitude, longitude)
                .onSuccess {
                    loadUserPreferences()
                }
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userDataRepository.updateNotificationsEnabled(enabled)
                .onSuccess {
                    loadUserPreferences()
                }
        }
    }

    fun uploadAvatar(imageUri: Uri) {
        viewModelScope.launch {
            _operationState.value = UserDataUiState.Loading
            try {
                // Leer bytes del URI
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val imageBytes = inputStream?.readBytes()
                inputStream?.close()

                if (imageBytes == null) {
                    _operationState.value = UserDataUiState.Error("No se pudo leer la imagen")
                    return@launch
                }

                // Subir imagen al repositorio
                userDataRepository.uploadAvatar(imageBytes)
                    .onSuccess { avatarUrl ->
                        _operationState.value = UserDataUiState.Success("Foto actualizada")
                        loadUserPreferences()
                    }
                    .onFailure { error ->
                        _operationState.value = UserDataUiState.Error(error.message ?: "Error al subir imagen")
                    }
            } catch (e: Exception) {
                _operationState.value = UserDataUiState.Error("Error al procesar imagen: ${e.message}")
            }
        }
    }

    fun resetOperationState() {
        _operationState.value = null
    }
}
