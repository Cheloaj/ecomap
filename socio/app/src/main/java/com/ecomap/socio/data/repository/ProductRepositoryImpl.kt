package com.ecomap.socio.data.repository

import com.ecomap.socio.data.model.Product
import com.ecomap.socio.domain.repository.ProductRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import java.io.File
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : ProductRepository {

    override suspend fun getProductsByBusinessId(
        businessId: String,
        limit: Int?,
        offset: Int
    ): Result<List<Product>> {
        return try {
            // 📄 PAGINACIÓN: Cargar productos en chunks para mejor rendimiento
            val startTime = System.currentTimeMillis()

            val products = supabase.from("products")
                .select(Columns.ALL) {
                    filter {
                        eq("business_id", businessId)
                    }
                    // 🚀 Aplicar paginación si se especifica límite
                    limit?.let {
                        range(offset.toLong(), (offset + it - 1).toLong())
                    }
                }
                .decodeList<Product>()

            val elapsed = System.currentTimeMillis() - startTime
            val paginationInfo = if (limit != null) "limit=$limit, offset=$offset" else "sin paginación"
            android.util.Log.d("ProductRepo", "✅ Productos cargados: ${products.size} productos ($paginationInfo) en ${elapsed}ms")

            Result.success(products)
        } catch (e: Exception) {
            println("❌ Error al cargar productos: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getProductById(productId: String): Result<Product> {
        return try {
            val product = supabase.from("products")
                .select(Columns.ALL) {
                    filter {
                        eq("id", productId)
                    }
                }
                .decodeSingle<Product>()

            Result.success(product)
        } catch (e: Exception) {
            println("❌ Error al cargar producto: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun createProduct(product: Product): Result<Product> {
        return try {
            val createdProduct = supabase.from("products")
                .insert(product) {
                    select(Columns.ALL)
                }
                .decodeSingle<Product>()

            println("✅ Producto creado: ${createdProduct.name}")
            Result.success(createdProduct)
        } catch (e: Exception) {
            println("❌ Error al crear producto: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun updateProduct(product: Product): Result<Product> {
        return try {
            val updatedProduct = supabase.from("products")
                .update(product) {
                    filter {
                        eq("id", product.id)
                    }
                    select(Columns.ALL)
                }
                .decodeSingle<Product>()

            println("✅ Producto actualizado: ${updatedProduct.name}")
            Result.success(updatedProduct)
        } catch (e: Exception) {
            println("❌ Error al actualizar producto: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            supabase.from("products")
                .delete {
                    filter {
                        eq("id", productId)
                    }
                }

            println("✅ Producto eliminado")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Error al eliminar producto: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun toggleProductAvailability(productId: String, isAvailable: Boolean): Result<Unit> {
        return try {
            supabase.from("products")
                .update(
                    mapOf("is_available" to isAvailable)
                ) {
                    filter {
                        eq("id", productId)
                    }
                }

            println("✅ Disponibilidad actualizada: ${if (isAvailable) "Disponible" else "Agotado"}")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Error al actualizar disponibilidad: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun uploadProductImage(file: File, businessId: String): Result<String> {
        return try {
            val fileName = "product_${System.currentTimeMillis()}_${file.name}"
            // ✅ ORGANIZADO: product-images/businesses/{businessId}/products/{fileName}
            val path = "businesses/$businessId/products/$fileName"

            println("📤 Subiendo imagen de producto a product-images: $path")

            // Subir archivo a Supabase Storage en bucket product-images
            supabase.storage.from("product-images").upload(
                path = path,
                data = file.readBytes(),
                upsert = false
            )

            // Obtener URL pública del bucket product-images
            val publicUrl = supabase.storage.from("product-images").publicUrl(path)

            println("✅ Imagen de producto subida exitosamente: $publicUrl")
            Result.success(publicUrl)
        } catch (e: Exception) {
            println("❌ Error al subir imagen de producto: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun publishScheduledProducts(): Result<Int> {
        return try {
            println("📅 Verificando productos programados para publicar...")

            // Obtener productos programados
            val scheduledProducts = supabase.from("products")
                .select(Columns.ALL) {
                    filter {
                        eq("publication_status", "scheduled")
                    }
                }
                .decodeList<Product>()

            println("📦 Encontrados ${scheduledProducts.size} productos programados")

            // Filtrar productos cuya fecha ya pasó
            val today = java.time.LocalDate.now().toString()
            val productsToPublish = scheduledProducts.filter { product ->
                product.scheduledDate != null && product.scheduledDate <= today
            }

            println("🚀 ${productsToPublish.size} productos listos para publicar")

            // Publicar cada producto
            var publishedCount = 0
            productsToPublish.forEach { product ->
                try {
                    supabase.from("products")
                        .update(
                            mapOf("publication_status" to "published")
                        ) {
                            filter {
                                eq("id", product.id)
                            }
                        }
                    publishedCount++
                    println("✅ Publicado: ${product.name} (programado para ${product.scheduledDate})")
                } catch (e: Exception) {
                    println("❌ Error al publicar ${product.name}: ${e.message}")
                }
            }

            println("✅ Total publicados: $publishedCount productos")
            Result.success(publishedCount)
        } catch (e: Exception) {
            println("❌ Error al publicar productos programados: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
