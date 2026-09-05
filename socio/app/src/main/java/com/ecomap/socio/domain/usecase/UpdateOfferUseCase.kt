package com.ecomap.socio.domain.usecase

import com.ecomap.socio.data.model.Offer
import com.ecomap.socio.domain.repository.OfferRepository
import javax.inject.Inject

class UpdateOfferUseCase @Inject constructor(
    private val offerRepository: OfferRepository
) {
    suspend operator fun invoke(offer: Offer): Result<Offer> {
        // Validations
        if (offer.productName.isBlank()) {
            return Result.failure(Exception("El nombre del producto no puede estar vacío"))
        }

        if (offer.price <= 0) {
            return Result.failure(Exception("El precio debe ser mayor a 0"))
        }

        return offerRepository.updateOffer(offer)
    }
}
