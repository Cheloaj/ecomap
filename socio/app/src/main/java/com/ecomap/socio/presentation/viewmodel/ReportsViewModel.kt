package com.ecomap.socio.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.socio.data.model.ProductComplaint
import com.ecomap.socio.domain.repository.ComplaintRepository
import com.ecomap.socio.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val complaintRepository: ComplaintRepository
) : ViewModel() {

    private val _reportsState = MutableStateFlow<UiState<List<ProductComplaint>>>(UiState.Idle)
    val reportsState: StateFlow<UiState<List<ProductComplaint>>> = _reportsState.asStateFlow()

    private val _responseState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val responseState: StateFlow<UiState<Unit>> = _responseState.asStateFlow()

    fun loadVendorReports() {
        viewModelScope.launch {
            try {
                _reportsState.value = UiState.Loading
                println("📋 Cargando reportes del vendedor...")

                val result = complaintRepository.getVendorComplaints()
                _reportsState.value = result.fold(
                    onSuccess = { complaints ->
                        println("✅ Reportes cargados: ${complaints.size}")
                        UiState.Success(complaints)
                    },
                    onFailure = { error ->
                        println("❌ Error al cargar reportes: ${error.message}")
                        UiState.Error(error.message ?: "Error al cargar reportes")
                    }
                )
            } catch (e: Exception) {
                println("❌ Exception en loadVendorReports: ${e.message}")
                e.printStackTrace()
                _reportsState.value = UiState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    fun respondToComplaint(complaintId: String, response: String) {
        viewModelScope.launch {
            try {
                _responseState.value = UiState.Loading
                println("📝 Respondiendo a reporte: $complaintId")

                val result = complaintRepository.respondToComplaint(complaintId, response)
                _responseState.value = result.fold(
                    onSuccess = {
                        println("✅ Respuesta enviada correctamente")
                        // Recargar reportes para actualizar la UI
                        loadVendorReports()
                        UiState.Success(Unit)
                    },
                    onFailure = { error ->
                        println("❌ Error al enviar respuesta: ${error.message}")
                        UiState.Error(error.message ?: "Error al enviar respuesta")
                    }
                )
            } catch (e: Exception) {
                println("❌ Exception en respondToComplaint: ${e.message}")
                e.printStackTrace()
                _responseState.value = UiState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    fun resetResponseState() {
        _responseState.value = UiState.Idle
    }
}
