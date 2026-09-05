package com.ecomap.usuario.data.repository

import com.ecomap.usuario.data.model.ProductReport
import com.ecomap.usuario.domain.repository.ReportRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

class ReportRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : ReportRepository {

    override suspend fun submitProductReport(report: ProductReport): Result<Unit> {
        return try {
            supabase.from("product_reports").insert(report)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
