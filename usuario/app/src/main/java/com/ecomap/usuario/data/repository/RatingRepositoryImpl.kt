package com.ecomap.usuario.data.repository

import android.content.Context
import android.net.Uri
import com.ecomap.usuario.data.model.ProductRating
import com.ecomap.usuario.data.model.ProductRatingStats
import com.ecomap.usuario.domain.repository.RatingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.util.UUID
import javax.inject.Inject

@Serializable
data class RatingInsert(
    @SerialName("user_id")
    val userId: String,

    @SerialName("product_id")
    val productId: String,

    @SerialName("rating")
    val rating: Int,

    @SerialName("comment")
    val comment: String? = null,

    @SerialName("image_url")
    val imageUrl: String? = null
)

@Serializable
data class RatingUpdate(
    @SerialName("rating")
    val rating: Int,

    @SerialName("comment")
    val comment: String? = null,

    @SerialName("image_url")
    val imageUrl: String? = null
)

class RatingRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    @ApplicationContext private val context: Context
) : RatingRepository {

    private suspend fun uploadImage(imageUri: Uri): Result<String> {
        return try {
            // Leer bytes de la imagen
            val bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                ?: return Result.failure(Exception("No se pudo leer la imagen"))

            // Generar nombre único para la imagen
            val fileName = "rating_${UUID.randomUUID()}.jpg"

            // Subir a Supabase Storage
            val bucket = supabase.storage.from("rating-images")
            bucket.upload(fileName, bytes)

            // Obtener URL pública
            val publicUrl = bucket.publicUrl(fileName)

            android.util.Log.d("RatingRepo", "📸 Imagen subida exitosamente: $publicUrl")
            Result.success(publicUrl)
        } catch (e: Exception) {
            android.util.Log.e("RatingRepo", "❌ Error al subir imagen: ${e.message}", e)
            Result.failure(Exception("Error al subir la imagen: ${e.message}"))
        }
    }

    override suspend fun rateProduct(
        productId: String,
        rating: Int,
        comment: String?,
        imageUri: Uri?
    ): Result<ProductRating> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // 📸 Subir imagen si existe
            var imageUrl: String? = null
            if (imageUri != null) {
                android.util.Log.d("RatingRepo", "📤 Subiendo imagen...")
                val uploadResult = uploadImage(imageUri)
                if (uploadResult.isSuccess) {
                    imageUrl = uploadResult.getOrNull()
                    android.util.Log.d("RatingRepo", "✅ Imagen subida: $imageUrl")
                } else {
                    android.util.Log.e("RatingRepo", "⚠️ Error al subir imagen: ${uploadResult.exceptionOrNull()?.message}")
                    // Continuar sin imagen si falla la subida
                }
            }

            val newRating = RatingInsert(
                userId = userId,
                productId = productId,
                rating = rating,
                comment = if (comment.isNullOrBlank()) null else comment,
                imageUrl = imageUrl
            )

            val response = supabase.from("product_ratings")
                .insert(newRating) {
                    select()
                }

            val insertedRating = response.decodeSingle<ProductRating>()

            Result.success(insertedRating)
        } catch (e: Exception) {
            android.util.Log.e("RatingRepo", "Error al calificar: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("duplicate key") == true -> "Ya has calificado este producto"
                else -> e.message ?: "Error al calificar el producto"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    override suspend fun updateRating(
        ratingId: String,
        rating: Int,
        comment: String?,
        imageUri: Uri?
    ): Result<ProductRating> {
        return try {
            // 📸 Subir imagen si existe
            var imageUrl: String? = null
            if (imageUri != null) {
                android.util.Log.d("RatingRepo", "📤 Subiendo imagen...")
                val uploadResult = uploadImage(imageUri)
                if (uploadResult.isSuccess) {
                    imageUrl = uploadResult.getOrNull()
                    android.util.Log.d("RatingRepo", "✅ Imagen subida: $imageUrl")
                } else {
                    android.util.Log.e("RatingRepo", "⚠️ Error al subir imagen: ${uploadResult.exceptionOrNull()?.message}")
                    // Continuar sin imagen si falla la subida
                }
            }

            val updateData = RatingUpdate(
                rating = rating,
                comment = if (comment.isNullOrBlank()) null else comment,
                imageUrl = imageUrl
            )

            val response = supabase.from("product_ratings")
                .update(updateData) {
                    filter { eq("id", ratingId) }
                    select()
                }

            val updatedRating = response.decodeSingle<ProductRating>()

            Result.success(updatedRating)
        } catch (e: Exception) {
            android.util.Log.e("RatingRepo", "Error al actualizar: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Error al actualizar la calificación"))
        }
    }

    override suspend fun deleteRating(ratingId: String): Result<Unit> {
        return try {
            supabase.from("product_ratings")
                .delete {
                    filter { eq("id", ratingId) }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Error al eliminar la calificación"))
        }
    }

    override suspend fun getUserRatingForProduct(productId: String): Result<ProductRating?> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.success(null)

            val rating = supabase.from("product_ratings")
                .select {
                    filter {
                        eq("product_id", productId)
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<ProductRating>()

            Result.success(rating)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Error al obtener la calificación"))
        }
    }

    override suspend fun getProductRatings(productId: String): Result<List<ProductRating>> {
        return try {
            // ⚡ OPTIMIZACIÓN: Batch loading de ratings
            val startTime = System.currentTimeMillis()

            // Obtener ratings
            val ratingsData = supabase.from("product_ratings")
                .select {
                    filter { eq("product_id", productId) }
                    order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
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
                    userName = usersMap[userId],
                    imageUrl = json["image_url"]?.jsonPrimitive?.contentOrNull
                )
            }

            val elapsed = System.currentTimeMillis() - startTime
            android.util.Log.d("RatingRepo", "✅ Ratings cargados con batch loading: ${ratings.size} ratings, ${userIds.size} usuarios únicos en ${elapsed}ms")

            Result.success(ratings)
        } catch (e: Exception) {
            android.util.Log.e("RatingRepo", "Error al obtener ratings: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Error al obtener las calificaciones"))
        }
    }

    override suspend fun getProductRatingStats(productId: String): Result<ProductRatingStats?> {
        return try {
            val stats = supabase.from("product_rating_stats")
                .select {
                    filter { eq("product_id", productId) }
                }
                .decodeSingleOrNull<ProductRatingStats>()

            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Error al obtener las estadísticas"))
        }
    }
}
