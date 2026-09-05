package com.ecomap.socio.data.repository

import com.ecomap.socio.data.model.ProductRating
import com.ecomap.socio.domain.repository.RatingRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject

@Serializable
data class VendorResponseUpdate(
    @SerialName("vendor_response") val vendorResponse: String,
    @SerialName("vendor_response_at") val vendorResponseAt: String,
    @SerialName("vendor_user_id") val vendorUserId: String
)

@Serializable
data class VendorResponseDelete(
    @SerialName("vendor_response") val vendorResponse: String? = null,
    @SerialName("vendor_response_at") val vendorResponseAt: String? = null,
    @SerialName("vendor_user_id") val vendorUserId: String? = null
)

class RatingRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : RatingRepository {

    override suspend fun getProductRatings(productId: String): Result<List<ProductRating>> {
        return try {
            // ⚡ OPTIMIZACIÓN: Batch loading de ratings
            val startTime = System.currentTimeMillis()

            // Obtener ratings
            val ratingsData = supabase.from("product_ratings")
                .select {
                    filter {
                        eq("product_id", productId)
                    }
                    order(column = "created_at", order = Order.DESCENDING)
                }
                .decodeList<JsonObject>()

            // 🚀 Extraer todos los user IDs únicos (evita queries duplicadas)
            val userIds = ratingsData.mapNotNull {
                it["user_id"]?.jsonPrimitive?.content
            }.distinct()

            // 🔥 UNA SOLA query para obtener TODOS los usuarios
            val usersMap = if (userIds.isNotEmpty()) {
                try {
                    val users = supabase.from("users")
                        .select {
                            filter {
                                isIn("id", userIds)
                            }
                        }
                        .decodeList<JsonObject>()

                    // Crear mapa userId -> userName para lookup O(1)
                    users.associate { user ->
                        val id = user["id"]?.jsonPrimitive?.content ?: ""
                        val name = user["full_name"]?.jsonPrimitive?.contentOrNull
                        id to name
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RatingRepo", "Error al cargar usuarios: ${e.message}")
                    emptyMap()
                }
            } else {
                emptyMap()
            }

            // ⚡ Mapear ratings con nombres (sin queries adicionales)
            val ratings = ratingsData.map { json ->
                val userId = json["user_id"]?.jsonPrimitive?.content ?: ""

                ProductRating(
                    id = json["id"]?.jsonPrimitive?.content ?: "",
                    userId = userId,
                    productId = json["product_id"]?.jsonPrimitive?.content ?: "",
                    rating = json["rating"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                    comment = json["comment"]?.jsonPrimitive?.contentOrNull,
                    createdAt = json["created_at"]?.jsonPrimitive?.content ?: "",
                    updatedAt = json["updated_at"]?.jsonPrimitive?.content ?: "",
                    vendorResponse = json["vendor_response"]?.jsonPrimitive?.contentOrNull,
                    vendorResponseAt = json["vendor_response_at"]?.jsonPrimitive?.contentOrNull,
                    vendorUserId = json["vendor_user_id"]?.jsonPrimitive?.contentOrNull,
                    userName = usersMap[userId]
                )
            }

            val elapsed = System.currentTimeMillis() - startTime
            android.util.Log.d("RatingRepo", "✅ Ratings cargados con batch loading: ${ratings.size} ratings, ${userIds.size} usuarios únicos en ${elapsed}ms")

            Result.success(ratings)
        } catch (e: Exception) {
            android.util.Log.e("RatingRepo", "Error al obtener ratings: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getAverageRating(productId: String): Result<Double> {
        return try {
            val ratings = getProductRatings(productId).getOrThrow()

            if (ratings.isEmpty()) {
                Result.success(0.0)
            } else {
                val average = ratings.map { it.rating }.average()
                Result.success(average)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTotalRatings(productId: String): Result<Int> {
        return try {
            val ratings = getProductRatings(productId).getOrThrow()
            Result.success(ratings.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addVendorResponse(
        ratingId: String,
        response: String
    ): Result<ProductRating> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Vendedor no autenticado"))

            val vendorResponseUpdate = VendorResponseUpdate(
                vendorResponse = response,
                vendorResponseAt = kotlinx.datetime.Clock.System.now().toString(),
                vendorUserId = userId
            )

            val result = supabase.from("product_ratings")
                .update(vendorResponseUpdate) {
                    filter {
                        eq("id", ratingId)
                    }
                    select()
                }

            val updatedRating = result.decodeSingle<ProductRating>()
            Result.success(updatedRating)
        } catch (e: Exception) {
            android.util.Log.e("RatingRepo", "Error al agregar respuesta: ${e.message}", e)
            Result.failure(Exception("Error al agregar respuesta del vendedor"))
        }
    }

    override suspend fun updateVendorResponse(
        ratingId: String,
        response: String
    ): Result<ProductRating> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Vendedor no autenticado"))

            val vendorResponseUpdate = VendorResponseUpdate(
                vendorResponse = response,
                vendorResponseAt = kotlinx.datetime.Clock.System.now().toString(),
                vendorUserId = userId
            )

            val result = supabase.from("product_ratings")
                .update(vendorResponseUpdate) {
                    filter {
                        eq("id", ratingId)
                    }
                    select()
                }

            val updatedRating = result.decodeSingle<ProductRating>()
            Result.success(updatedRating)
        } catch (e: Exception) {
            android.util.Log.e("RatingRepo", "Error al actualizar respuesta: ${e.message}", e)
            Result.failure(Exception("Error al actualizar respuesta del vendedor"))
        }
    }

    override suspend fun deleteVendorResponse(ratingId: String): Result<ProductRating> {
        return try {
            val vendorResponseDelete = VendorResponseDelete()

            val result = supabase.from("product_ratings")
                .update(vendorResponseDelete) {
                    filter {
                        eq("id", ratingId)
                    }
                    select()
                }

            val updatedRating = result.decodeSingle<ProductRating>()
            Result.success(updatedRating)
        } catch (e: Exception) {
            android.util.Log.e("RatingRepo", "Error al eliminar respuesta: ${e.message}", e)
            Result.failure(Exception("Error al eliminar respuesta del vendedor"))
        }
    }
}
