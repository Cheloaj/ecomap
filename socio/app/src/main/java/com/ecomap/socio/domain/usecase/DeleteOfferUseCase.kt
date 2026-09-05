package com.ecomap.socio.domain.usecase

import com.ecomap.socio.domain.repository.OfferRepository
import javax.inject.Inject

class DeleteOfferUseCase @Inject constructor(
    private val offerRepository: OfferRepository
) {
    suspend operator fun invoke(offerId: String): Result<Unit> {
        return offerRepository.deleteOffer(offerId)
    }
}
