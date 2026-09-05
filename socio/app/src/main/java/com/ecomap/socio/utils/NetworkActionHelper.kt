package com.ecomap.socio.utils

import com.ecomap.socio.presentation.viewmodel.ConnectivityViewModel

/**
 * Helper para ejecutar acciones que requieren conexión a Internet
 * Verifica automáticamente la conexión antes de ejecutar la acción
 */
object NetworkActionHelper {

    /**
     * Ejecuta una acción solo si hay conexión a Internet
     * Si no hay conexión, muestra automáticamente la pantalla de "Sin Internet"
     *
     * @param connectivityViewModel El ViewModel de conectividad global
     * @param action La acción a ejecutar si hay conexión
     */
    fun executeWithNetwork(
        connectivityViewModel: ConnectivityViewModel,
        action: () -> Unit
    ) {
        if (connectivityViewModel.checkConnectionBeforeAction()) {
            // Hay conexión, ejecutar acción
            action()
        }
        // Si no hay conexión, checkConnectionBeforeAction() ya mostró la pantalla modal
    }

    /**
     * Ejecuta una acción suspendida solo si hay conexión a Internet
     *
     * @param connectivityViewModel El ViewModel de conectividad global
     * @param action La acción suspendida a ejecutar si hay conexión
     */
    suspend fun executeWithNetworkSuspend(
        connectivityViewModel: ConnectivityViewModel,
        action: suspend () -> Unit
    ) {
        if (connectivityViewModel.checkConnectionBeforeAction()) {
            // Hay conexión, ejecutar acción
            action()
        }
        // Si no hay conexión, checkConnectionBeforeAction() ya mostró la pantalla modal
    }
}
