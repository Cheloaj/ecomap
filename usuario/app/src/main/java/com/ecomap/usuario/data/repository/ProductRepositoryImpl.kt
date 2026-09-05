package com.ecomap.usuario.data.repository

import android.content.Context
import android.net.Uri
import com.ecomap.usuario.data.model.Product
import com.ecomap.usuario.domain.repository.ProductRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import javax.inject.Inject

@Serializable
data class ProductInsert(
    @SerialName("user_id")
    val userId: String,

    @SerialName("name")
    val name: String,

    @SerialName("description")
    val description: String? = null,

    @SerialName("price")
    val price: Double,

    @SerialName("unit_type")
    val unitType: String,

    @SerialName("image_url")
    val imageUrl: String? = null,

    @SerialName("is_available")
    val isAvailable: Boolean = true,

    @SerialName("latitude")
    val latitude: Double,

    @SerialName("longitude")
    val longitude: Double,

    @SerialName("location_address")
    val locationAddress: String? = null,

    @SerialName("moderation_status")
    val moderationStatus: String = "pending"
)

class ProductRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : ProductRepository {

    private fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }

    override suspend fun uploadProduct(
        name: String,
        description: String?,
        price: Double,
        imageUri: Uri?,
        latitude: Double,
        longitude: Double,
        address: String?,
        unitType: String
    ): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("Usuario no autenticado"))

            android.util.Log.d("ProductRepo", "Subiendo producto: $name por usuario: $userId en lat: $latitude, lon: $longitude")

            // Subir imagen si existe
            var imageUrl: String? = null
            if (imageUri != null) {
                imageUrl = uploadImage(imageUri, userId)
                android.util.Log.d("ProductRepo", "Imagen subida: $imageUrl")
            }

            // Crear producto con ubicación usando clase serializable
            val productInsert = ProductInsert(
                userId = userId,
                name = name,
                description = description,
                price = price,
                unitType = unitType,
                imageUrl = imageUrl,
                isAvailable = true,
                latitude = latitude,
                longitude = longitude,
                locationAddress = address
            )

            supabase.from("products").insert(productInsert)
            android.util.Log.d("ProductRepo", "Producto insertado exitosamente")

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ProductRepo", "Error al subir producto: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun uploadImage(uri: Uri, userId: String): String {
        // Leer archivo desde URI
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("No se pudo leer la imagen")

        val bytes = inputStream.readBytes()
        inputStream.close()

        // Generar nombre único para la imagen
        val timestamp = System.currentTimeMillis()
        val fileName = "product_${timestamp}.jpg"

        // Organizar en carpetas por usuario: users/{userId}/products/{fileName}
        val filePath = "users/$userId/products/$fileName"
        val bucket = "product-images" // Nombre del bucket en Supabase Storage

        android.util.Log.d("ProductRepo", "Subiendo imagen a: $filePath")

        // Subir a Supabase Storage con la nueva estructura de carpetas
        supabase.storage.from(bucket).upload(
            path = filePath,
            data = bytes,
            upsert = false
        )

        // Obtener URL pública
        val publicUrl = supabase.storage.from(bucket).publicUrl(filePath)
        android.util.Log.d("ProductRepo", "URL pública de imagen: $publicUrl")
        return publicUrl
    }

    override suspend fun getCommunityProducts(
        limit: Int?,
        offset: Int
    ): Result<List<Product>> {
        return try {
            // 📄 PAGINACIÓN: Cargar productos en chunks para mejor rendimiento
            val startTime = System.currentTimeMillis()

            // ❌ Productos COMUNITARIOS: SÍ requieren moderación
            // Son vendedores sin establecimiento, deben ser aprobados
            val allProducts = supabase.from("products")
                .select {
                    filter {
                        eq("is_available", true)
                        // Filtrar solo productos comunitarios (sin business_id)
                        Product::businessId isExact null
                    }
                    // 🚀 Aplicar paginación si se especifica límite
                    limit?.let {
                        range(offset.toLong(), (offset + it - 1).toLong())
                    }
                }
                .decodeList<Product>()

            // Filtrar para mostrar SOLO productos aprobados y no programados
            val products = allProducts.filter { product ->
                // ✅ DEBE estar aprobado (o sin estado = aprobado por defecto)
                (product.moderationStatus == "approved" || product.moderationStatus == null) &&
                // ✅ NO debe estar programado
                product.publicationStatus != "scheduled"
            }

            val elapsed = System.currentTimeMillis() - startTime
            val paginationInfo = if (limit != null) "limit=$limit, offset=$offset" else "sin paginación"
            android.util.Log.d("ProductRepo", "📦 Productos comunitarios: ${products.size} aprobados de ${allProducts.size} totales ($paginationInfo) en ${elapsed}ms")

            Result.success(products)
        } catch (e: Exception) {
            android.util.Log.e("ProductRepo", "❌ Error al obtener productos: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getProductById(productId: String): Result<Product> {
        return try {
            // Primero, obtener el producto
            val product = supabase.from("products")
                .select {
                    filter {
                        eq("id", productId)
                    }
                }
                .decodeSingle<Product>()

            // Si el producto tiene businessId, obtener los datos del negocio
            val productWithBusinessData = if (product.businessId != null) {
                try {
                    val business = supabase.from("businesses")
                        .select {
                            filter {
                                eq("id", product.businessId)
                            }
                        }
                        .decodeSingle<JsonObject>()

                    // Agregar datos del negocio al producto
                    product.copy(
                        ownerName = business["business_name"]?.jsonPrimitive?.contentOrNull,
                        locationAddress = business["address"]?.jsonPrimitive?.contentOrNull,
                        latitude = business["latitude"]?.jsonPrimitive?.doubleOrNull,
                        longitude = business["longitude"]?.jsonPrimitive?.doubleOrNull
                    ).also {
                        android.util.Log.d("ProductRepo", "✅ Producto obtenido: ${it.name}")
                        android.util.Log.d("ProductRepo", "   Negocio: ${it.ownerName}")
                        android.util.Log.d("ProductRepo", "   Dirección: ${it.locationAddress}")
                        android.util.Log.d("ProductRepo", "   Coordenadas: (${it.latitude}, ${it.longitude})")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("ProductRepo", "⚠️ No se pudieron obtener datos del negocio: ${e.message}")
                    product
                }
            } else {
                android.util.Log.d("ProductRepo", "✅ Producto comunitario obtenido: ${product.name}")
                product
            }

            Result.success(productWithBusinessData)
        } catch (e: Exception) {
            android.util.Log.e("ProductRepo", "❌ Error al obtener producto: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun searchProducts(
        query: String,
        offset: Int,
        limit: Int
    ): Result<List<Product>> {
        return try {
            val startTime = System.currentTimeMillis()
            val searchPattern = "%${query.lowercase()}%"

            // Buscar productos (excluyendo los que están en oferta)
            val products = supabase.from("products")
                .select {
                    filter {
                        or {
                            // Buscar en nombre o descripción
                            ilike("name", searchPattern)
                            ilike("description", searchPattern)
                        }
                        eq("is_available", true)
                        // ✅ EXCLUIR productos que están en oferta (se mostrarán desde offers)
                        eq("is_on_offer", false)
                    }
                    order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<Product>()

            // Filtrar solo aprobados y publicados
            val filteredProducts = products.filter { product ->
                (product.moderationStatus == "approved" || product.moderationStatus == null) &&
                product.publicationStatus != "scheduled"
            }

            // Buscar productos que ESTÁN en oferta
            val productsOnOffer = try {
                supabase.from("products")
                    .select {
                        filter {
                            or {
                                ilike("name", searchPattern)
                                ilike("description", searchPattern)
                            }
                            eq("is_available", true)
                            eq("is_on_offer", true)
                        }
                        order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        range(offset.toLong(), (offset + limit - 1).toLong())
                    }
                    .decodeList<Product>()
                    .filter { product ->
                        (product.moderationStatus == "approved" || product.moderationStatus == null) &&
                        product.publicationStatus != "scheduled"
                    }
            } catch (e: Exception) {
                android.util.Log.e("ProductRepo", "❌ Error al buscar productos en oferta: ${e.message}")
                emptyList()
            }

            // Combinar y eliminar duplicados
            val allResults = (filteredProducts + productsOnOffer)
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt }

            val elapsed = System.currentTimeMillis() - startTime
            android.util.Log.d("ProductRepo", "🔍 Búsqueda '$query': ${allResults.size} resultados (${filteredProducts.size} productos + ${productsOnOffer.size} ofertas) en ${elapsed}ms")

            Result.success(allResults)
        } catch (e: Exception) {
            android.util.Log.e("ProductRepo", "❌ Error en búsqueda de productos: ${e.message}", e)
            Result.failure(e)
        }
    }
}
