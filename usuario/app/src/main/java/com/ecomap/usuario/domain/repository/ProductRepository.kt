package com.ecomap.usuario.domain.repository

import android.net.Uri
import com.ecomap.usuario.data.model.Product

interface ProductRepository {
    suspend fun uploadProduct(
        name: String,
        description: String?,
        price: Double,
        imageUri: Uri?,
        latitude: Double,
        longitude: Double,
        address: String?,
        unitType: String = "unit"
    ): Result<Unit>

    suspend fun getCommunityProducts(
        limit: Int? = null,
        offset: Int = 0
    ): Result<List<Product>>

    suspend fun getProductById(productId: String): Result<Product>

    suspend fun searchProducts(
        query: String,
        offset: Int = 0,
        limit: Int = 20
    ): Result<List<Product>>
}
