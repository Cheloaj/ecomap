package com.ecomap.socio.domain.repository

import com.ecomap.socio.data.model.Product
import java.io.File

interface ProductRepository {
    suspend fun getProductsByBusinessId(
        businessId: String,
        limit: Int? = null,
        offset: Int = 0
    ): Result<List<Product>>
    suspend fun getProductById(productId: String): Result<Product>
    suspend fun createProduct(product: Product): Result<Product>
    suspend fun updateProduct(product: Product): Result<Product>
    suspend fun deleteProduct(productId: String): Result<Unit>
    suspend fun toggleProductAvailability(productId: String, isAvailable: Boolean): Result<Unit>
    suspend fun uploadProductImage(file: File, businessId: String): Result<String>
    suspend fun publishScheduledProducts(): Result<Int> // Devuelve cantidad de productos publicados
}
