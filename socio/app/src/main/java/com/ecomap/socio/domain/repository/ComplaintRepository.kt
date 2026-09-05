package com.ecomap.socio.domain.repository

import com.ecomap.socio.data.model.ProductComplaint

interface ComplaintRepository {
    /**
     * Obtiene todos los reportes de productos del vendedor actual
     */
    suspend fun getVendorComplaints(): Result<List<ProductComplaint>>

    /**
     * Responde a un reporte de producto
     */
    suspend fun respondToComplaint(complaintId: String, response: String): Result<Unit>

    /**
     * Obtiene los reportes de un producto específico
     */
    suspend fun getProductComplaints(productId: String): Result<List<ProductComplaint>>
}
