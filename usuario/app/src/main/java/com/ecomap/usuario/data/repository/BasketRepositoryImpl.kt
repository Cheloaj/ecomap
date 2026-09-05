package com.ecomap.usuario.data.repository

import com.ecomap.usuario.data.model.BasketItem
import com.ecomap.usuario.data.model.BasketItemInsert
import com.ecomap.usuario.data.model.BasketItemUpdate
import com.ecomap.usuario.data.model.Product
import com.ecomap.usuario.domain.repository.BasketRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.*
import javax.inject.Inject

class BasketRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : BasketRepository {

    override suspend fun addToBasket(productId: String, quantity: Int): Result<BasketItem> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // Verificar si ya existe el producto en la canasta
            val existing = supabase.from("basket")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("product_id", productId)
                    }
                }
                .decodeSingleOrNull<BasketItem>()

            if (existing != null) {
                // Si ya existe, actualizar cantidad
                return updateQuantity(existing.id, existing.quantity + quantity)
            }

            // Si no existe, insertar nuevo
            val newItem = BasketItemInsert(
                userId = userId,
                productId = productId,
                quantity = quantity
            )

            val response = supabase.from("basket")
                .insert(newItem) {
                    select()
                }

            val insertedItem = response.decodeSingle<BasketItem>()
            Result.success(insertedItem)
        } catch (e: Exception) {
            android.util.Log.e("BasketRepo", "Error al agregar: ${e.message}", e)
            Result.failure(Exception("Error al agregar a la canasta"))
        }
    }

    override suspend fun getBasketItems(): Result<List<BasketItem>> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.success(emptyList())

            // Obtener items de la canasta con información del producto
            val basketData = supabase.from("basket")
                .select {
                    filter { eq("user_id", userId) }
                    order(column = "created_at", order = Order.DESCENDING)
                }
                .decodeList<JsonObject>()

            // Mapear y obtener información de productos
            val basketItems = basketData.mapNotNull { json ->
                try {
                    val productId = json["product_id"]?.jsonPrimitive?.content ?: return@mapNotNull null

                    // Obtener información del producto
                    val product = try {
                        supabase.from("products")
                            .select {
                                filter { eq("id", productId) }
                            }
                            .decodeSingleOrNull<Product>()
                    } catch (e: Exception) {
                        null
                    }

                    BasketItem(
                        id = json["id"]?.jsonPrimitive?.content ?: "",
                        userId = json["user_id"]?.jsonPrimitive?.content ?: "",
                        productId = productId,
                        quantity = json["quantity"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1,
                        createdAt = json["created_at"]?.jsonPrimitive?.content ?: "",
                        updatedAt = json["updated_at"]?.jsonPrimitive?.content ?: "",
                        product = product
                    )
                } catch (e: Exception) {
                    android.util.Log.e("BasketRepo", "Error mapeando item: ${e.message}", e)
                    null
                }
            }

            Result.success(basketItems)
        } catch (e: Exception) {
            android.util.Log.e("BasketRepo", "Error al obtener canasta: ${e.message}", e)
            Result.failure(Exception("Error al obtener la canasta"))
        }
    }

    override suspend fun updateQuantity(basketItemId: String, quantity: Int): Result<BasketItem> {
        return try {
            if (quantity <= 0) {
                return removeFromBasket(basketItemId).map { BasketItem() }
            }

            val updateData = BasketItemUpdate(quantity = quantity)

            val response = supabase.from("basket")
                .update(updateData) {
                    filter { eq("id", basketItemId) }
                    select()
                }

            val updatedItem = response.decodeSingle<BasketItem>()
            Result.success(updatedItem)
        } catch (e: Exception) {
            android.util.Log.e("BasketRepo", "Error al actualizar: ${e.message}", e)
            Result.failure(Exception("Error al actualizar cantidad"))
        }
    }

    override suspend fun removeFromBasket(basketItemId: String): Result<Unit> {
        return try {
            supabase.from("basket")
                .delete {
                    filter { eq("id", basketItemId) }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("BasketRepo", "Error al eliminar: ${e.message}", e)
            Result.failure(Exception("Error al eliminar de la canasta"))
        }
    }

    override suspend fun clearBasket(): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Usuario no autenticado"))

            supabase.from("basket")
                .delete {
                    filter { eq("user_id", userId) }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("BasketRepo", "Error al limpiar: ${e.message}", e)
            Result.failure(Exception("Error al limpiar la canasta"))
        }
    }

    override suspend fun getBasketCount(): Result<Int> {
        return try {
            val items = getBasketItems().getOrThrow()
            val totalCount = items.sumOf { it.quantity }
            Result.success(totalCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
