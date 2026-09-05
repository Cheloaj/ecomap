package com.ecomap.socio.domain.repository

import com.ecomap.socio.data.model.Business
import java.io.File

interface BusinessRepository {
    suspend fun createBusiness(business: Business): Result<Business>
    suspend fun getBusinessByOwnerId(ownerId: String): Result<Business?>
    suspend fun getAllBusinessesByOwnerId(ownerId: String): Result<List<Business>>
    suspend fun updateBusiness(business: Business): Result<Business>
    suspend fun uploadVerificationDocument(file: File, ownerId: String): Result<String>
    suspend fun updateBusinessAvatar(file: File, businessId: String): Result<String>
    suspend fun deactivateBusiness(businessId: String): Result<Unit>
    suspend fun reactivateBusiness(businessId: String): Result<Unit>
    suspend fun markApprovalAsSeen(businessId: String): Result<Unit>
}
