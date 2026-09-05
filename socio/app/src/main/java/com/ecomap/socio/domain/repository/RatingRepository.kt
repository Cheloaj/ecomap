package com.ecomap.socio.domain.repository

import com.ecomap.socio.data.model.ProductRating

interface RatingRepository {
    suspend fun getProductRatings(productId: String): Result<List<ProductRating>>
    suspend fun getAverageRating(productId: String): Result<Double>
    suspend fun getTotalRatings(productId: String): Result<Int>

    // Vendor response methods
    suspend fun addVendorResponse(ratingId: String, response: String): Result<ProductRating>
    suspend fun updateVendorResponse(ratingId: String, response: String): Result<ProductRating>
    suspend fun deleteVendorResponse(ratingId: String): Result<ProductRating>
}
