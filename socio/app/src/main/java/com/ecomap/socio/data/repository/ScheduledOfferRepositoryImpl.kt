package com.ecomap.socio.data.repository

import com.ecomap.socio.data.model.ScheduledOffer
import com.ecomap.socio.data.model.ScheduledOfferStatus
import com.ecomap.socio.domain.repository.ScheduledOfferRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order // CORREGIDO: Import necesario para el ordenamiento
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ScheduledOfferRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : ScheduledOfferRepository {

    override suspend fun createScheduledOffer(scheduledOffer: ScheduledOffer): Result<ScheduledOffer> {
        return try {
            val created = supabase.from("scheduled_offers")
                .insert(scheduledOffer) {
                    select()
                }
                .decodeSingle<ScheduledOffer>()

            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getScheduledOffersByBusinessId(businessId: String): Result<List<ScheduledOffer>> {
        return try {
            val scheduledOffers = supabase.from("scheduled_offers")
                .select {
                    filter {
                        eq("business_id", businessId)
                    }
                    // CORREGIDO: Se cambió 'ascending = true' por el nuevo parámetro 'order'
                    order("scheduled_date", order = Order.ASCENDING)
                    order("scheduled_time", order = Order.ASCENDING)
                }
                .decodeList<ScheduledOffer>()

            Result.success(scheduledOffers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getScheduledOffersRealtimeByBusinessId(businessId: String): Flow<List<ScheduledOffer>> {
        val channel = supabase.channel("scheduled-offers-$businessId")

        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "scheduled_offers"
            filter = "business_id=eq.$businessId"
        }

        return changeFlow.map {
            supabase.from("scheduled_offers")
                .select {
                    filter {
                        eq("business_id", businessId)
                    }
                    // CORREGIDO: Se cambió 'ascending = true' por el nuevo parámetro 'order'
                    order("scheduled_date", order = Order.ASCENDING)
                    order("scheduled_time", order = Order.ASCENDING)
                }
                .decodeList<ScheduledOffer>()
        }
    }

    override suspend fun updateScheduledOffer(scheduledOffer: ScheduledOffer): Result<ScheduledOffer> {
        return try {
            val updated = supabase.from("scheduled_offers")
                .update(
                    {
                        set("product_name", scheduledOffer.productName)
                        set("price", scheduledOffer.price)
                        set("unit", scheduledOffer.unit)
                        set("validity_type", scheduledOffer.validityType)
                        set("scheduled_date", scheduledOffer.scheduledDate)
                        set("scheduled_time", scheduledOffer.scheduledTime)
                    }
                ) {
                    filter {
                        eq("id", scheduledOffer.id)
                    }
                    select()
                }
                .decodeSingle<ScheduledOffer>()

            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteScheduledOffer(scheduledOfferId: String): Result<Unit> {
        return try {
            supabase.from("scheduled_offers")
                .delete {
                    filter {
                        eq("id", scheduledOfferId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelScheduledOffer(scheduledOfferId: String): Result<Unit> {
        return try {
            supabase.from("scheduled_offers")
                .update(
                    {
                        set("status", ScheduledOfferStatus.CANCELLED.name.lowercase())
                    }
                ) {
                    filter {
                        eq("id", scheduledOfferId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getScheduledOffersCount(businessId: String): Result<Int> {
        return try {
            val offers = supabase.from("scheduled_offers")
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("business_id", businessId)
                        eq("status", ScheduledOfferStatus.PENDING.name.lowercase())
                    }
                }
                .decodeList<ScheduledOffer>()

            Result.success(offers.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}