package com.ecomap.usuario.utils

/**
 * Representa el estado de una operación UI genérica.
 * Utilizado para manejar estados de carga, éxito y error en ViewModels.
 */
sealed class UiState<out T> {
    object Initial : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
