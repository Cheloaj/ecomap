package com.ecomap.usuario.domain.repository

import com.ecomap.usuario.data.model.BasketItem
import com.ecomap.usuario.data.model.Product

interface BasketRepository {
    suspend fun addToBasket(productId: String, quantity: Int = 1): Result<BasketItem>
    suspend fun getBasketItems(): Result<List<BasketItem>>
    suspend fun updateQuantity(basketItemId: String, quantity: Int): Result<BasketItem>
    suspend fun removeFromBasket(basketItemId: String): Result<Unit>
    suspend fun clearBasket(): Result<Unit>
    suspend fun getBasketCount(): Result<Int>
}
