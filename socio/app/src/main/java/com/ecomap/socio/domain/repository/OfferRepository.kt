package com.ecomap.socio.domain.repository

import com.ecomap.socio.data.model.Offer
import kotlinx.coroutines.flow.Flow

interface OfferRepository {
    suspend fun createOffer(offer: Offer): Result<Offer>
    suspend fun getOffersByBusinessId(businessId: String): Result<List<Offer>>
    fun getOffersRealtimeByBusinessId(businessId: String): Flow<List<Offer>>
    suspend fun updateOffer(offer: Offer): Result<Offer>
    suspend fun deleteOffer(offerId: String): Result<Unit>
    suspend fun toggleOfferStock(offerId: String, isOutOfStock: Boolean): Result<Unit>
    suspend fun getActiveOffersCount(businessId: String): Result<Int>
}
