package com.ecomap.usuario.domain.repository

import com.ecomap.usuario.data.model.Business
import com.ecomap.usuario.data.model.Product
import com.ecomap.usuario.data.model.Offer

interface BusinessRepository {
    suspend fun getAllBusinesses(): Result<List<Business>>
    suspend fun getBusinessById(businessId: String): Result<Business>
    suspend fun getProductsByBusiness(businessId: String): Result<List<Product>>
    suspend fun getOffersByBusiness(businessId: String): Result<List<Offer>>
    suspend fun getAllActiveOffers(): Result<List<Offer>>
    suspend fun searchBusinesses(query: String): Result<List<Business>>
}
