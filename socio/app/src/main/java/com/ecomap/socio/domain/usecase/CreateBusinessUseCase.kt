package com.ecomap.socio.domain.usecase

import com.ecomap.socio.data.model.Business
import com.ecomap.socio.domain.repository.BusinessRepository
import javax.inject.Inject

class CreateBusinessUseCase @Inject constructor(
    private val businessRepository: BusinessRepository
) {
    suspend operator fun invoke(business: Business): Result<Business> {
        // Validation
        if (business.businessName.length < 3) {
            return Result.failure(Exception("El nombre del negocio debe tener al menos 3 caracteres"))
        }

        if (business.businessType.isBlank()) {
            return Result.failure(Exception("Debe seleccionar un tipo de negocio"))
        }

        if (business.latitude == 0.0 || business.longitude == 0.0) {
            return Result.failure(Exception("Debe seleccionar una ubicación válida"))
        }

        return businessRepository.createBusiness(business)
    }
}
