package com.ecomap.socio.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.ecomap.socio.data.remote.NominatimApi
import com.ecomap.socio.domain.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nominatimApi: NominatimApi
) : LocationRepository {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Result<Location> {
        return try {
            val cancellationTokenSource = CancellationTokenSource()
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).await()

            if (location != null) {
                Result.success(location)
            } else {
                Result.failure(Exception("Unable to get current location"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<String> {
        return try {
            // Rate limit: 1 request per second
            delay(1000)

            val response = nominatimApi.reverseGeocode(latitude, longitude)
            Result.success(response.displayName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
