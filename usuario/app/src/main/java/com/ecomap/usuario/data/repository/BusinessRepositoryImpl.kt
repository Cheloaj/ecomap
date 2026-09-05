package com.ecomap.usuario.data.repository

import com.ecomap.usuario.data.model.Business
import com.ecomap.usuario.data.model.Product
import com.ecomap.usuario.data.model.Offer
import com.ecomap.usuario.domain.repository.BusinessRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

class BusinessRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : BusinessRepository {

    override suspend fun getAllBusinesses(): Result<List<Business>> {
        return try {
            val businesses = supabase.from("businesses")
                .select {
                    filter {
                        eq("verification_status", "approved")
                        eq("is_active", true) // ✅ Solo negocios activos
                    }
                }
                .decodeList<Business>()

            Result.success(businesses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBusinessById(businessId: String): Result<Business> {
        return try {
            val business = supabase.from("businesses")
                .select {
                    filter { eq("id", businessId) }
                }
                .decodeSingle<Business>()

            Result.success(business)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProductsByBusiness(businessId: String): Result<List<Product>> {
        return try {
            // ✅ Productos de NEGOCIOS: NO requieren moderación
            // Los vendedores con establecimiento pueden publicar directamente
            val allProducts = supabase.from("products")
                .select {
                    filter {
                        eq("business_id", businessId)
                        eq("is_available", true)
                    }
                }
                .decodeList<Product>()

            // Solo excluir productos programados (feature Pro)
            val products = allProducts.filter { product ->
                product.publicationStatus != "scheduled"
            }

            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOffersByBusiness(businessId: String): Result<List<Offer>> {
        return try {
            // 🔥 NUEVO: Obtener ofertas desde productos con is_on_offer = true
            val products = supabase.from("products")
                .select {
                    filter {
                        eq("business_id", businessId)
                        eq("is_on_offer", true)
                        eq("is_available", true)
                    }
                }
                .decodeList<Product>()

            // Convertir productos a ofertas
            val offers = products.map { product ->
                Offer(
                    id = product.id,
                    businessId = product.businessId ?: "",
                    ownerId = "", // No disponible en Product
                    productName = product.name,
                    price = product.price,
                    unit = product.unit ?: "",
                    validityType = "until_date",
                    validUntil = product.offerValidUntil,
                    isActive = product.isAvailable,
                    isOutOfStock = !product.isAvailable,
                    createdAt = product.createdAt,
                    updatedAt = product.updatedAt,
                    viewCount = 0,
                    reportCount = 0,
                    confirmationCount = 0,
                    title = product.offerDescription,
                    description = product.description,
                    discountPercentage = product.discountPercentage,
                    originalPrice = product.originalPrice,
                    discountedPrice = if (product.discountPercentage != null && product.originalPrice != null) {
                        product.originalPrice * (1 - product.discountPercentage / 100.0)
                    } else product.price,
                    startDate = product.createdAt,
                    endDate = product.offerValidUntil,
                    imageUrl = product.imageUrl,
                    business = null
                )
            }

            Result.success(offers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllActiveOffers(): Result<List<Offer>> {
        return try {
            // 🔥 NUEVO: Obtener todas las ofertas desde productos con is_on_offer = true
            val products = supabase.from("products")
                .select(
                    columns = Columns.raw("""
                        *,
                        business:businesses(*)
                    """.trimIndent())
                ) {
                    filter {
                        eq("is_on_offer", true)
                        eq("is_available", true)
                    }
                }
                .decodeList<JsonObject>()

            // Convertir productos a ofertas
            val offers = products.map { json ->
                val product = Product(
                    id = json["id"]?.jsonPrimitive?.content ?: "",
                    businessId = json["business_id"]?.jsonPrimitive?.contentOrNull,
                    userId = json["user_id"]?.jsonPrimitive?.contentOrNull,
                    name = json["name"]?.jsonPrimitive?.content ?: "",
                    description = json["description"]?.jsonPrimitive?.contentOrNull,
                    price = json["price"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    imageUrl = json["image_url"]?.jsonPrimitive?.contentOrNull,
                    category = json["category"]?.jsonPrimitive?.contentOrNull,
                    unit = json["unit"]?.jsonPrimitive?.contentOrNull,
                    isAvailable = json["is_available"]?.jsonPrimitive?.booleanOrNull ?: true,
                    createdAt = json["created_at"]?.jsonPrimitive?.content ?: "",
                    updatedAt = json["updated_at"]?.jsonPrimitive?.content ?: "",
                    isOnOffer = json["is_on_offer"]?.jsonPrimitive?.booleanOrNull ?: false,
                    originalPrice = json["original_price"]?.jsonPrimitive?.doubleOrNull,
                    offerType = json["offer_type"]?.jsonPrimitive?.contentOrNull,
                    discountPercentage = json["discount_percentage"]?.jsonPrimitive?.intOrNull,
                    offerDescription = json["offer_description"]?.jsonPrimitive?.contentOrNull,
                    offerValidUntil = json["offer_valid_until"]?.jsonPrimitive?.contentOrNull
                )

                // Extraer business si existe
                val businessJson = json["business"] as? JsonObject
                val business = businessJson?.let {
                    Business(
                        id = it["id"]?.jsonPrimitive?.content ?: "",
                        userId = it["user_id"]?.jsonPrimitive?.content ?: "",
                        businessName = it["business_name"]?.jsonPrimitive?.content ?: "",
                        businessType = it["business_type"]?.jsonPrimitive?.content ?: "",
                        latitude = it["latitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                        longitude = it["longitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                        address = it["address"]?.jsonPrimitive?.content ?: "",
                        operatingHours = it["operating_hours"]?.jsonPrimitive?.content ?: "",
                        avatarUrl = it["avatar_url"]?.jsonPrimitive?.contentOrNull
                    )
                }

                Offer(
                    id = product.id,
                    businessId = product.businessId ?: "",
                    ownerId = "",
                    productName = product.name,
                    price = product.price,
                    unit = product.unit ?: "",
                    validityType = "until_date",
                    validUntil = product.offerValidUntil,
                    isActive = product.isAvailable,
                    isOutOfStock = !product.isAvailable,
                    createdAt = product.createdAt,
                    updatedAt = product.updatedAt,
                    viewCount = 0,
                    reportCount = 0,
                    confirmationCount = 0,
                    title = product.offerDescription,
                    description = product.description,
                    discountPercentage = product.discountPercentage,
                    originalPrice = product.originalPrice,
                    discountedPrice = if (product.discountPercentage != null && product.originalPrice != null) {
                        product.originalPrice * (1 - product.discountPercentage / 100.0)
                    } else product.price,
                    startDate = product.createdAt,
                    endDate = product.offerValidUntil,
                    imageUrl = product.imageUrl,
                    business = business
                )
            }

            Result.success(offers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchBusinesses(query: String): Result<List<Business>> {
        return try {
            // Validar query antes de buscar
            if (query.isBlank()) {
                android.util.Log.d("BusinessRepository", "⚠️ Query vacío, retornando todos los negocios")
                return getAllBusinesses()
            }

            val searchPattern = query.trim().lowercase()
            android.util.Log.d("BusinessRepository", "🔍 Buscando: '$searchPattern'")

            // 1. Buscar negocios por nombre, tipo o dirección
            val businessesByNameOrType = try {
                supabase.from("businesses")
                    .select {
                        filter {
                            eq("verification_status", "approved")
                            eq("is_active", true)
                            or {
                                ilike("business_name", "%$searchPattern%")
                                ilike("business_type", "%$searchPattern%")
                                ilike("address", "%$searchPattern%")
                            }
                        }
                    }
                    .decodeList<Business>()
            } catch (e: Exception) {
                android.util.Log.e("BusinessRepository", "Error buscando negocios: ${e.message}")
                e.printStackTrace()
                emptyList()
            }

            android.util.Log.d("BusinessRepository", "📍 Negocios por nombre/tipo/dirección: ${businessesByNameOrType.size}")

            // 2. Buscar productos que coincidan con la query
            val products = try {
                supabase.from("products")
                    .select {
                        filter {
                            eq("is_available", true)
                            or {
                                ilike("name", "%$searchPattern%")
                                ilike("description", "%$searchPattern%")
                                ilike("category", "%$searchPattern%")
                            }
                        }
                    }
                    .decodeList<Product>()
            } catch (e: Exception) {
                android.util.Log.e("BusinessRepository", "Error buscando productos: ${e.message}")
                e.printStackTrace()
                emptyList()
            }

            android.util.Log.d("BusinessRepository", "📦 Productos encontrados: ${products.size}")

            // 3. Obtener IDs únicos de negocios que tienen esos productos
            val businessIdsFromProducts = products
                .mapNotNull { it.businessId }
                .filter { it.isNotBlank() }
                .distinct()

            android.util.Log.d("BusinessRepository", "🏪 IDs de negocios con productos: ${businessIdsFromProducts.size}")

            // 4. Obtener los negocios de esos productos (solo si hay IDs)
            val businessesFromProducts = if (businessIdsFromProducts.isNotEmpty()) {
                try {
                    // Validar que la lista tenga elementos válidos
                    val validIds = businessIdsFromProducts.filter { it.isNotBlank() }

                    if (validIds.isEmpty()) {
                        android.util.Log.d("BusinessRepository", "⚠️ No hay IDs válidos después de filtrar")
                        emptyList()
                    } else {
                        android.util.Log.d("BusinessRepository", "🔍 Buscando negocios con IDs: $validIds")

                        supabase.from("businesses")
                            .select {
                                filter {
                                    eq("verification_status", "approved")
                                    eq("is_active", true)
                                    isIn("id", validIds)
                                }
                            }
                            .decodeList<Business>()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BusinessRepository", "❌ Error obteniendo negocios por productos: ${e.message}")
                    android.util.Log.e("BusinessRepository", "Stack trace:", e)
                    emptyList()
                }
            } else {
                android.util.Log.d("BusinessRepository", "⚠️ Lista de IDs vacía, saltando búsqueda de negocios")
                emptyList()
            }

            android.util.Log.d("BusinessRepository", "🏪 Negocios obtenidos por productos: ${businessesFromProducts.size}")

            // 5. Combinar resultados y eliminar duplicados
            val allBusinesses = (businessesByNameOrType + businessesFromProducts)
                .distinctBy { it.id }

            android.util.Log.d("BusinessRepository", "✅ Total resultados: ${allBusinesses.size}")

            Result.success(allBusinesses)
        } catch (e: Exception) {
            android.util.Log.e("BusinessRepository", "❌ Error general en búsqueda: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
