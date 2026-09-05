package com.ecomap.socio.domain.repository

import com.ecomap.socio.data.model.ScheduledOffer
import kotlinx.coroutines.flow.Flow

interface ScheduledOfferRepository {
    suspend fun createScheduledOffer(scheduledOffer: ScheduledOffer): Result<ScheduledOffer>
    suspend fun getScheduledOffersByBusinessId(businessId: String): Result<List<ScheduledOffer>>
    fun getScheduledOffersRealtimeByBusinessId(businessId: String): Flow<List<ScheduledOffer>>
    suspend fun updateScheduledOffer(scheduledOffer: ScheduledOffer): Result<ScheduledOffer>
    suspend fun deleteScheduledOffer(scheduledOfferId: String): Result<Unit>
    suspend fun cancelScheduledOffer(scheduledOfferId: String): Result<Unit>
    suspend fun getScheduledOffersCount(businessId: String): Result<Int>
}
