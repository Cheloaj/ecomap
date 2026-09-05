package com.ecomap.usuario.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.usuario.data.model.ProductComplaint
import com.ecomap.usuario.domain.repository.ProductComplaintRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ComplaintUiState {
    object Initial : ComplaintUiState()
    object Loading : ComplaintUiState()
    data class Success(val message: String) : ComplaintUiState()
    data class Error(val message: String) : ComplaintUiState()
}

@HiltViewModel
class ProductComplaintViewModel @Inject constructor(
    private val complaintRepository: ProductComplaintRepository
) : ViewModel() {

    private val _complaintState = MutableStateFlow<ComplaintUiState>(ComplaintUiState.Initial)
    val complaintState: StateFlow<ComplaintUiState> = _complaintState.asStateFlow()

    private val _userComplaints = MutableStateFlow<List<ProductComplaint>>(emptyList())
    val userComplaints: StateFlow<List<ProductComplaint>> = _userComplaints.asStateFlow()

    private val _productComplaints = MutableStateFlow<List<ProductComplaint>>(emptyList())
    val productComplaints: StateFlow<List<ProductComplaint>> = _productComplaints.asStateFlow()

    fun createComplaint(
        productId: String,
        reason: String,
        description: String?,
        imageUri: Uri?,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _complaintState.value = ComplaintUiState.Loading

            complaintRepository.createComplaint(productId, reason, description, imageUri)
                .onSuccess {
                    _complaintState.value = ComplaintUiState.Success("Reporte enviado exitosamente")
                    onResult(true, "Reporte enviado exitosamente")
                }
                .onFailure { error ->
                    val errorMessage = error.message ?: "Error al enviar el reporte"
                    _complaintState.value = ComplaintUiState.Error(errorMessage)
                    onResult(false, errorMessage)
                }
        }
    }

    fun updateComplaint(
        complaintId: String,
        reason: String,
        description: String?,
        imageUri: Uri?,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _complaintState.value = ComplaintUiState.Loading

            complaintRepository.updateComplaint(complaintId, reason, description, imageUri)
                .onSuccess {
                    _complaintState.value = ComplaintUiState.Success("Reporte actualizado")
                    onResult(true, "Reporte actualizado exitosamente")
                }
                .onFailure { error ->
                    val errorMessage = error.message ?: "Error al actualizar el reporte"
                    _complaintState.value = ComplaintUiState.Error(errorMessage)
                    onResult(false, errorMessage)
                }
        }
    }

    fun deleteComplaint(complaintId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _complaintState.value = ComplaintUiState.Loading

            complaintRepository.deleteComplaint(complaintId)
                .onSuccess {
                    _complaintState.value = ComplaintUiState.Success("Reporte eliminado")
                    onResult(true, "Reporte eliminado exitosamente")
                }
                .onFailure { error ->
                    val errorMessage = error.message ?: "Error al eliminar el reporte"
                    _complaintState.value = ComplaintUiState.Error(errorMessage)
                    onResult(false, errorMessage)
                }
        }
    }

    fun loadUserComplaints(userId: String) {
        viewModelScope.launch {
            complaintRepository.getUserComplaints(userId)
                .onSuccess { complaints ->
                    _userComplaints.value = complaints
                }
                .onFailure { error ->
                    android.util.Log.e("ComplaintVM", "Error al cargar quejas del usuario: ${error.message}")
                }
        }
    }

    fun loadProductComplaints(productId: String) {
        viewModelScope.launch {
            complaintRepository.getProductComplaints(productId)
                .onSuccess { complaints ->
                    _productComplaints.value = complaints
                }
                .onFailure { error ->
                    android.util.Log.e("ComplaintVM", "Error al cargar quejas del producto: ${error.message}")
                }
        }
    }

    fun resetState() {
        _complaintState.value = ComplaintUiState.Initial
    }
}
