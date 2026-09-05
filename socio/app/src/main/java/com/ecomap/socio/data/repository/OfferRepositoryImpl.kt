package com.ecomap.socio.data.repository

import com.ecomap.socio.data.model.Offer
import com.ecomap.socio.domain.repository.OfferRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order // CORREGIDO: Import necesario para el ordenamiento
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import javax.inject.Inject

class OfferRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : OfferRepository {

    override suspend fun createOffer(offer: Offer): Result<Offer> {
        return try {
            val newOffer = offer.copy(
                createdAt = Clock.System.now().toString(),
                updatedAt = Clock.System.now().toString()
            )

            supabase.from("offers").insert(newOffer)

            Result.success(newOffer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOffersByBusinessId(businessId: String): Result<List<Offer>> {
        return try {
            val offers = supabase.from("offers")
                .select {
                    filter {
                        eq("business_id", businessId)
                        eq("is_active", true)
                    }
                    // CORREGIDO: Se cambió 'ascending = false' por el nuevo parámetro 'order'
                    order("created_at", order = Order.DESCENDING)
                }.decodeList<Offer>()

            Result.success(offers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getOffersRealtimeByBusinessId(businessId: String): Flow<List<Offer>> {
        val channel = supabase.channel("offers-$businessId")

        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "offers"
            filter = "business_id=eq.$businessId"
        }

        return changeFlow.map {
            // Fetch updated list
            val offers = supabase.from("offers")
                .select {
                    filter {
                        eq("business_id", businessId)
                        eq("is_active", true)
                    }
                    // CORREGIDO: Se cambió 'ascending = false' por el nuevo parámetro 'order'
                    order("created_at", order = Order.DESCENDING)
                }.decodeList<Offer>()
            offers
        }
    }

    override suspend fun updateOffer(offer: Offer): Result<Offer> {
        return try {
            val updatedOffer = offer.copy(
                updatedAt = Clock.System.now().toString()
            )

            supabase.from("offers").update(updatedOffer) {
                filter {
                    eq("id", offer.id)
                }
            }

            Result.success(updatedOffer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteOffer(offerId: String): Result<Unit> {
        return try {
            supabase.from("offers").delete {
                filter {
                    eq("id", offerId)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleOfferStock(offerId: String, isOutOfStock: Boolean): Result<Unit> {
        return try {
            supabase.from("offers").update(
                {
                    set("is_out_of_stock", isOutOfStock)
                    set("updated_at", Clock.System.now().toString())
                }
            ) {
                filter {
                    eq("id", offerId)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActiveOffersCount(businessId: String): Result<Int> {
        return try {
            val offers = supabase.from("offers")
                .select {
                    filter {
                        eq("business_id", businessId)
                        eq("is_active", true)
                    }
                }.decodeList<Offer>()

            Result.success(offers.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}