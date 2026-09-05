package com.ecomap.usuario.data.repository

import android.content.Context
import android.net.Uri
import com.ecomap.usuario.data.model.ProductComplaint
import com.ecomap.usuario.domain.repository.ProductComplaintRepository
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
data class ComplaintInsert(
    @SerialName("user_id")
    val userId: String,

    @SerialName("product_id")
    val productId: String,

    @SerialName("reason")
    val reason: String,

    @SerialName("description")
    val description: String? = null,

    @SerialName("image_url")
    val imageUrl: String? = null,

    @SerialName("status")
    val status: String = "pending"
)

@Serializable
data class ComplaintUpdate(
    @SerialName("reason")
    val reason: String,

    @SerialName("description")
    val description: String? = null,

    @SerialName("image_url")
    val imageUrl: String? = null
)

class ProductComplaintRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    @ApplicationContext private val context: Context
) : ProductComplaintRepository {

    private suspend fun uploadImage(imageUri: Uri): Result<String> {
        return try {
            // Leer bytes de la imagen
            val bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                ?: return Result.failure(Exception("No se pudo leer la imagen"))

            // Generar nombre único para la imagen
            val fileName = "complaint_${UUID.randomUUID()}.jpg"

            // Subir a Supabase Storage
            val bucket = supabase.storage.from("complaint-images")
            bucket.upload(fileName, bytes)

            // Obtener URL pública
            val publicUrl = bucket.publicUrl(fileName)

            android.util.Log.d("ComplaintRepo", "📸 Imagen subida exitosamente: $publicUrl")
            Result.success(publicUrl)
        } catch (e: Exception) {
            android.util.Log.e("ComplaintRepo", "❌ Error al subir imagen: ${e.message}", e)
            Result.failure(Exception("Error al subir la imagen: ${e.message}"))
        }
    }

    override suspend fun createComplaint(
        productId: String,
        reason: String,
        description: String?,
        imageUri: Uri?
    ): Result<ProductComplaint> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // Obtener información del usuario (nombre y avatar)
            var userName: String? = null
            var userAvatarUrl: String? = null
            try {
                val userPrefs = supabase.from("user_preferences")
                    .select {
                        filter { eq("user_id", userId) }
                        limit(1)
                    }
                    .decodeSingleOrNull<com.ecomap.usuario.data.model.UserPreferences>()

                userName = userPrefs?.displayName
                userAvatarUrl = userPrefs?.avatarUrl
                android.util.Log.d("ComplaintRepo", "👤 Usuario: $userName, Avatar: $userAvatarUrl")
            } catch (e: Exception) {
                android.util.Log.w("ComplaintRepo", "No se pudo obtener info del usuario: ${e.message}")
            }

            // 📸 Subir imagen si existe
            var imageUrl: String? = null
            if (imageUri != null) {
                android.util.Log.d("ComplaintRepo", "📤 Subiendo imagen de evidencia...")
                val uploadResult = uploadImage(imageUri)
                if (uploadResult.isSuccess) {
                    imageUrl = uploadResult.getOrNull()
                    android.util.Log.d("ComplaintRepo", "✅ Imagen subida: $imageUrl")
                } else {
                    android.util.Log.e("ComplaintRepo", "⚠️ Error al subir imagen: ${uploadResult.exceptionOrNull()?.message}")
                    // Continuar sin imagen si falla la subida
                }
            }

            val newComplaint = ComplaintInsert(
                userId = userId,
                productId = productId,
                reason = reason,
                description = if (description.isNullOrBlank()) null else description,
                imageUrl = imageUrl
            )

            val response = supabase.from("product_complaints")
                .insert(newComplaint) {
                    select()
                }

            val insertedComplaint = response.decodeSingle<ProductComplaint>()

            android.util.Log.d("ComplaintRepo", "✅ Queja creada exitosamente: ${insertedComplaint.id}")
            Result.success(insertedComplaint)
        } catch (e: Exception) {
            android.util.Log.e("ComplaintRepo", "Error al crear queja: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Error al crear la queja"))
        }
    }

    override suspend fun getUserComplaints(userId: String): Result<List<ProductComplaint>> {
        return try {
            val startTime = System.currentTimeMillis()

            // Si userId está vacío, obtener el userId del usuario autenticado actual
            val actualUserId = if (userId.isBlank()) {
                supabase.auth.currentUserOrNull()?.id
                    ?: return Result.failure(Exception("Usuario no autenticado"))
            } else {
                userId
            }

            // Obtener quejas del usuario con información del producto
            val complaintsData = supabase.from("product_complaints")
                .select {
                    filter { eq("user_id", actualUserId) }
                    order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<JsonObject>()

            // Extraer product IDs únicos
            val productIds = complaintsData.mapNotNull {
                it["product_id"]?.jsonPrimitive?.content
            }.distinct()

            // Obtener información de productos en una sola query
            val productsMap = if (productIds.isNotEmpty()) {
                try {
                    val products = supabase.from("products")
                        .select {
                            filter {
                                isIn("id", productIds)
                            }
                        }
                        .decodeList<JsonObject>()

                    products.associate { product ->
                        val id = product["id"]?.jsonPrimitive?.content ?: ""
                        val name = product["name"]?.jsonPrimitive?.contentOrNull
                        val imageUrl = product["image_url"]?.jsonPrimitive?.contentOrNull
                        id to (name to imageUrl)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ComplaintRepo", "Error al cargar productos: ${e.message}")
                    emptyMap()
                }
            } else {
                emptyMap()
            }

            // Mapear quejas con información de productos
            val complaints = complaintsData.map { json ->
                val productId = json["product_id"]?.jsonPrimitive?.content ?: ""
                val (productName, productImageUrl) = productsMap[productId] ?: (null to null)

                ProductComplaint(
                    id = json["id"]?.jsonPrimitive?.content ?: "",
                    userId = json["user_id"]?.jsonPrimitive?.content ?: "",
                    productId = productId,
                    reason = json["reason"]?.jsonPrimitive?.content ?: "",
                    description = json["description"]?.jsonPrimitive?.contentOrNull,
                    imageUrl = json["image_url"]?.jsonPrimitive?.contentOrNull,
                    status = json["status"]?.jsonPrimitive?.content ?: "pending",
                    createdAt = json["created_at"]?.jsonPrimitive?.content ?: "",
                    updatedAt = json["updated_at"]?.jsonPrimitive?.content ?: "",
                    vendorResponse = json["vendor_response"]?.jsonPrimitive?.contentOrNull,
                    vendorResponseAt = json["vendor_response_at"]?.jsonPrimitive?.contentOrNull,
                    vendorUserId = json["vendor_user_id"]?.jsonPrimitive?.contentOrNull,
                    userName = null,
                    productName = productName,
                    productImageUrl = productImageUrl
                )
            }

            val elapsed = System.currentTimeMillis() - startTime
            android.util.Log.d("ComplaintRepo", "✅ Quejas cargadas: ${complaints.size} quejas en ${elapsed}ms")

            Result.success(complaints)
        } catch (e: Exception) {
            android.util.Log.e("ComplaintRepo", "Error al obtener quejas del usuario: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Error al obtener las quejas"))
        }
    }

    override suspend fun getComplaintById(complaintId: String): Result<ProductComplaint> {
        return try {
            val complaint = supabase.from("product_complaints")
                .select {
                    filter { eq("id", complaintId) }
                }
                .decodeSingle<ProductComplaint>()

            Result.success(complaint)
        } catch (e: Exception) {
            android.util.Log.e("ComplaintRepo", "Error al obtener queja: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Error al obtener la queja"))
        }
    }

    override suspend fun updateComplaint(
        complaintId: String,
        reason: String,
        description: String?,
        imageUri: Uri?
    ): Result<ProductComplaint> {
        return try {
            // 📸 Subir imagen si existe
            var imageUrl: String? = null
            if (imageUri != null) {
                android.util.Log.d("ComplaintRepo", "📤 Subiendo nueva imagen...")
                val uploadResult = uploadImage(imageUri)
                if (uploadResult.isSuccess) {
                    imageUrl = uploadResult.getOrNull()
                    android.util.Log.d("ComplaintRepo", "✅ Imagen actualizada: $imageUrl")
                } else {
                    android.util.Log.e("ComplaintRepo", "⚠️ Error al subir imagen: ${uploadResult.exceptionOrNull()?.message}")
                    // Continuar sin imagen si falla la subida
                }
            }

            val updateData = ComplaintUpdate(
                reason = reason,
                description = if (description.isNullOrBlank()) null else description,
                imageUrl = imageUrl
            )

            val response = supabase.from("product_complaints")
                .update(updateData) {
                    filter { eq("id", complaintId) }
                    select()
                }

            val updatedComplaint = response.decodeSingle<ProductComplaint>()

            android.util.Log.d("ComplaintRepo", "✅ Queja actualizada: $complaintId")
            Result.success(updatedComplaint)
        } catch (e: Exception) {
            android.util.Log.e("ComplaintRepo", "Error al actualizar queja: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Error al actualizar la queja"))
        }
    }

    override suspend fun deleteComplaint(complaintId: String): Result<Unit> {
        return try {
            supabase.from("product_complaints")
                .delete {
                    filter { eq("id", complaintId) }
                }

            android.util.Log.d("ComplaintRepo", "✅ Queja eliminada: $complaintId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ComplaintRepo", "Error al eliminar queja: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Error al eliminar la queja"))
        }
    }

    override suspend fun getProductComplaints(productId: String): Result<List<ProductComplaint>> {
        return try {
            val startTime = System.currentTimeMillis()

            // Obtener quejas del producto
            val complaintsData = supabase.from("product_complaints")
                .select {
                    filter { eq("product_id", productId) }
                    order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<JsonObject>()

            // Extraer user IDs únicos
            val userIds = complaintsData.mapNotNull {
                it["user_id"]?.jsonPrimitive?.content
            }.distinct()

            // Obtener información de usuarios en una sola query
            val usersMap = if (userIds.isNotEmpty()) {
                try {
                    val users = supabase.from("users")
                        .select {
                            filter {
                                isIn("id", userIds)
                            }
                        }
                        .decodeList<JsonObject>()

                    users.associate { user ->
                        val id = user["id"]?.jsonPrimitive?.content ?: ""
                        val name = user["full_name"]?.jsonPrimitive?.contentOrNull
                        id to name
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ComplaintRepo", "Error al cargar usuarios: ${e.message}")
                    emptyMap()
                }
            } else {
                emptyMap()
            }

            // Mapear quejas con nombres de usuarios
            val complaints = complaintsData.map { json ->
                val userId = json["user_id"]?.jsonPrimitive?.content ?: ""

                ProductComplaint(
                    id = json["id"]?.jsonPrimitive?.content ?: "",
                    userId = userId,
                    productId = json["product_id"]?.jsonPrimitive?.content ?: "",
                    reason = json["reason"]?.jsonPrimitive?.content ?: "",
                    description = json["description"]?.jsonPrimitive?.contentOrNull,
                    imageUrl = json["image_url"]?.jsonPrimitive?.contentOrNull,
                    status = json["status"]?.jsonPrimitive?.content ?: "pending",
                    createdAt = json["created_at"]?.jsonPrimitive?.content ?: "",
                    updatedAt = json["updated_at"]?.jsonPrimitive?.content ?: "",
                    vendorResponse = json["vendor_response"]?.jsonPrimitive?.contentOrNull,
                    vendorResponseAt = json["vendor_response_at"]?.jsonPrimitive?.contentOrNull,
                    vendorUserId = json["vendor_user_id"]?.jsonPrimitive?.contentOrNull,
                    userName = usersMap[userId],
                    productName = null,
                    productImageUrl = null
                )
            }

            val elapsed = System.currentTimeMillis() - startTime
            android.util.Log.d("ComplaintRepo", "✅ Quejas del producto cargadas: ${complaints.size} quejas, ${userIds.size} usuarios únicos en ${elapsed}ms")

            Result.success(complaints)
        } catch (e: Exception) {
            android.util.Log.e("ComplaintRepo", "Error al obtener quejas del producto: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Error al obtener las quejas"))
        }
    }
}
