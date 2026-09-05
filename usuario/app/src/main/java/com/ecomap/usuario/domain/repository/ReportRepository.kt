package com.ecomap.usuario.domain.repository

import com.ecomap.usuario.data.model.ProductReport

interface ReportRepository {
    suspend fun submitProductReport(report: ProductReport): Result<Unit>
}
