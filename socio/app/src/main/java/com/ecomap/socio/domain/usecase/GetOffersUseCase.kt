package com.ecomap.socio.domain.usecase

import com.ecomap.socio.data.model.Offer
import com.ecomap.socio.domain.repository.OfferRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetOffersUseCase @Inject constructor(
    private val offerRepository: OfferRepository
) {
    suspend operator fun invoke(businessId: String): Result<List<Offer>> {
        return offerRepository.getOffersByBusinessId(businessId)
    }

    fun getRealtime(businessId: String): Flow<List<Offer>> {
        return offerRepository.getOffersRealtimeByBusinessId(businessId)
    }
}
