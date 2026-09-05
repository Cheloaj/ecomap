package com.ecomap.usuario.presentation.viewmodel

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

sealed class ComplaintsUiState {
    object Loading : ComplaintsUiState()
    data class Success(val complaints: List<ProductComplaint>) : ComplaintsUiState()
    data class Error(val message: String) : ComplaintsUiState()
}

@HiltViewModel
class ComplaintsViewModel @Inject constructor(
    private val complaintRepository: ProductComplaintRepository
) : ViewModel() {

    private val _myComplaintsState = MutableStateFlow<ComplaintsUiState>(ComplaintsUiState.Loading)
    val myComplaintsState: StateFlow<ComplaintsUiState> = _myComplaintsState.asStateFlow()

    fun loadMyComplaints() {
        viewModelScope.launch {
            try {
                _myComplaintsState.value = ComplaintsUiState.Loading
                println("📋 Cargando mis reportes...")

                // Obtener el userId del auth actual
                val result = complaintRepository.getUserComplaints("")
                _myComplaintsState.value = result.fold(
                    onSuccess = { complaints ->
                        println("✅ Reportes cargados: ${complaints.size}")
                        ComplaintsUiState.Success(complaints)
                    },
                    onFailure = { error ->
                        println("❌ Error al cargar reportes: ${error.message}")
                        ComplaintsUiState.Error(error.message ?: "Error al cargar reportes")
                    }
                )
            } catch (e: Exception) {
                println("❌ Exception en loadMyComplaints: ${e.message}")
                e.printStackTrace()
                _myComplaintsState.value = ComplaintsUiState.Error("Error inesperado: ${e.message}")
            }
        }
    }
}
