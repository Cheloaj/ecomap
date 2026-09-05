package com.ecomap.usuario.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.ecomap.usuario.utils.ConnectivityObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel para gestionar el estado global de conectividad
 * Controla cuándo mostrar/ocultar la pantalla de "Sin Internet"
 */
class ConnectivityViewModel : ViewModel() {

    private val _showNoInternetScreen = MutableStateFlow(false)
    val showNoInternetScreen: StateFlow<Boolean> = _showNoInternetScreen.asStateFlow()

    private val _currentNetworkStatus = MutableStateFlow<ConnectivityObserver.Status>(
        ConnectivityObserver.Status.Available
    )
    val currentNetworkStatus: StateFlow<ConnectivityObserver.Status> = _currentNetworkStatus.asStateFlow()

    /**
     * Actualizar el estado de la red
     */
    fun updateNetworkStatus(status: ConnectivityObserver.Status) {
        _currentNetworkStatus.value = status
    }

    /**
     * Verificar si hay conexión antes de ejecutar una acción
     * Si no hay conexión, muestra la pantalla de "Sin Internet"
     * @return true si hay conexión, false si no hay
     */
    fun checkConnectionBeforeAction(): Boolean {
        val hasConnection = _currentNetworkStatus.value == ConnectivityObserver.Status.Available

        if (!hasConnection) {
            // Mostrar pantalla de "Sin Internet"
            _showNoInternetScreen.value = true
        }

        return hasConnection
    }

    /**
     * Intentar reintentar la conexión
     * Si ahora hay internet, cierra la pantalla modal
     */
    fun retryConnection() {
        val hasConnection = _currentNetworkStatus.value == ConnectivityObserver.Status.Available

        if (hasConnection) {
            // Hay internet, cerrar pantalla
            dismissNoInternetScreen()
        }
        // Si no hay internet, la pantalla permanece abierta
    }

    /**
     * Cerrar la pantalla de "Sin Internet" manualmente
     */
    fun dismissNoInternetScreen() {
        _showNoInternetScreen.value = false
    }
}
