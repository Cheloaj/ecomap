package com.ecomap.usuario.domain.repository

import android.net.Uri
import com.ecomap.usuario.data.model.ProductRating
import com.ecomap.usuario.data.model.ProductRatingStats

interface RatingRepository {
    suspend fun rateProduct(productId: String, rating: Int, comment: String?, imageUri: Uri? = null): Result<ProductRating>
    suspend fun updateRating(ratingId: String, rating: Int, comment: String?, imageUri: Uri? = null): Result<ProductRating>
    suspend fun deleteRating(ratingId: String): Result<Unit>
    suspend fun getUserRatingForProduct(productId: String): Result<ProductRating?>
    suspend fun getProductRatings(productId: String): Result<List<ProductRating>>
    suspend fun getProductRatingStats(productId: String): Result<ProductRatingStats?>
}
