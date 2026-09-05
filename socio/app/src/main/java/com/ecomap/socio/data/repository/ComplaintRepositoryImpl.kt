package com.ecomap.socio.data.repository

import com.ecomap.socio.data.model.ProductComplaint
import com.ecomap.socio.domain.repository.ComplaintRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import javax.inject.Inject

@Serializable
data class ComplaintResponse(
    @SerialName("vendor_response")
    val vendorResponse: String,

    @SerialName("vendor_response_at")
    val vendorResponseAt: String,

    @SerialName("vendor_user_id")
    val vendorUserId: String,

    @SerialName("status")
    val status: String = "resolved"
)

class ComplaintRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : ComplaintRepository {

    override suspend fun getVendorComplaints(): Result<List<ProductComplaint>> {
        return try {
            val startTime = System.currentTimeMillis()
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Usuario no autenticado"))

            android.util.Log.d("ComplaintRepo", "🔍 Obteniendo reportes para vendedor: $userId")

            // Primero obtener los productos del vendedor
            val productsData = supabase.from("products")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<JsonObject>()

            val productIds = productsData.mapNotNull {
                it["id"]?.jsonPrimitive?.content
            }

            android.util.Log.d("ComplaintRepo", "📦 Productos del vendedor: ${productIds.size}")

            if (productIds.isEmpty()) {
                android.util.Log.d("ComplaintRepo", "⚠️ Vendedor sin productos, retornando lista vacía")
                return Result.success(emptyList())
            }

            // Obtener quejas de esos productos que estén aprobadas por admin
            val complaintsData = supabase.from("product_complaints")
                .select {
                    filter {
                        isIn("product_id", productIds)
                        eq("admin_approved", true) // ✅ SOLO reportes aprobados por admin
                    }
                    order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<JsonObject>()

            android.util.Log.d("ComplaintRepo", "📋 Quejas encontradas: ${complaintsData.size}")

            // Extraer IDs únicos de usuarios y productos
            val userIds = complaintsData.mapNotNull {
                it["user_id"]?.jsonPrimitive?.content
            }.distinct()

            // Obtener información de usuarios
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

            // Obtener información de productos
            val productsMap = productsData.associate { product ->
                val id = product["id"]?.jsonPrimitive?.content ?: ""
                val name = product["name"]?.jsonPrimitive?.contentOrNull
                val imageUrl = product["image_url"]?.jsonPrimitive?.contentOrNull
                id to (name to imageUrl)
            }

            // Mapear quejas con información completa
            val complaints = complaintsData.map { json ->
                val userId = json["user_id"]?.jsonPrimitive?.content ?: ""
                val productId = json["product_id"]?.jsonPrimitive?.content ?: ""
                val (productName, productImageUrl) = productsMap[productId] ?: (null to null)

                ProductComplaint(
                    id = json["id"]?.jsonPrimitive?.content ?: "",
                    userId = userId,
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
                    userName = usersMap[userId],
                    productName = productName,
                    productImageUrl = productImageUrl
                )
            }

            val elapsed = System.currentTimeMillis() - startTime
            android.util.Log.d("ComplaintRepo", "✅ Quejas cargadas: ${complaints.size} en ${elapsed}ms")

            Result.success(complaints)
        } catch (e: Exception) {
            android.util.Log.e("ComplaintRepo", "Error al obtener quejas del vendedor: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Error al obtener las quejas"))
        }
    }

    override suspend fun respondToComplaint(complaintId: String, response: String): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val responseData = ComplaintResponse(
                vendorResponse = response,
                vendorResponseAt = getCurrentTimestamp(),
                vendorUserId = userId,
                status = "resolved"
            )

            supabase.from("product_complaints")
                .update(responseData) {
                    filter { eq("id", complaintId) }
                }

            android.util.Log.d("ComplaintRepo", "✅ Respuesta enviada a queja: $complaintId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ComplaintRepo", "Error al responder queja: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Error al enviar respuesta"))
        }
    }

    override suspend fun getProductComplaints(productId: String): Result<List<ProductComplaint>> {
        return try {
            val complaintsData = supabase.from("product_complaints")
                .select {
                    filter { eq("product_id", productId) }
                    order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<JsonObject>()

            val complaints = complaintsData.map { json ->
                ProductComplaint(
                    id = json["id"]?.jsonPrimitive?.content ?: "",
                    userId = json["user_id"]?.jsonPrimitive?.content ?: "",
                    productId = json["product_id"]?.jsonPrimitive?.content ?: "",
                    reason = json["reason"]?.jsonPrimitive?.content ?: "",
                    description = json["description"]?.jsonPrimitive?.contentOrNull,
                    imageUrl = json["image_url"]?.jsonPrimitive?.contentOrNull,
                    status = json["status"]?.jsonPrimitive?.content ?: "pending",
                    createdAt = json["created_at"]?.jsonPrimitive?.content ?: "",
                    updatedAt = json["updated_at"]?.jsonPrimitive?.content ?: "",
                    vendorResponse = json["vendor_response"]?.jsonPrimitive?.contentOrNull,
                    vendorResponseAt = json["vendor_response_at"]?.jsonPrimitive?.contentOrNull,
                    vendorUserId = json["vendor_user_id"]?.jsonPrimitive?.contentOrNull
                )
            }

            Result.success(complaints)
        } catch (e: Exception) {
            android.util.Log.e("ComplaintRepo", "Error al obtener quejas del producto: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Error al obtener las quejas"))
        }
    }

    private fun getCurrentTimestamp(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return dateFormat.format(java.util.Date())
    }
}
