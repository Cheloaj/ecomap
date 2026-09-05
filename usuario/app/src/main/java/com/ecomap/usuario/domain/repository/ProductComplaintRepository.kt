package com.ecomap.usuario.domain.repository

import android.net.Uri
import com.ecomap.usuario.data.model.ProductComplaint

interface ProductComplaintRepository {
    /**
     * Crear un nuevo reporte/queja de producto
     */
    suspend fun createComplaint(
        productId: String,
        reason: String,
        description: String?,
        imageUri: Uri?
    ): Result<ProductComplaint>

    /**
     * Obtener todas las quejas de un usuario
     */
    suspend fun getUserComplaints(userId: String): Result<List<ProductComplaint>>

    /**
     * Obtener una queja específica
     */
    suspend fun getComplaintById(complaintId: String): Result<ProductComplaint>

    /**
     * Actualizar una queja existente (solo si está pending)
     */
    suspend fun updateComplaint(
        complaintId: String,
        reason: String,
        description: String?,
        imageUri: Uri?
    ): Result<ProductComplaint>

    /**
     * Eliminar una queja (solo el usuario que la creó)
     */
    suspend fun deleteComplaint(complaintId: String): Result<Unit>

    /**
     * Obtener quejas de un producto específico (para el vendedor)
     */
    suspend fun getProductComplaints(productId: String): Result<List<ProductComplaint>>
}
