package com.ecomap.socio.domain.repository

import android.location.Location

interface LocationRepository {
    suspend fun getCurrentLocation(): Result<Location>
    suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<String>
}
