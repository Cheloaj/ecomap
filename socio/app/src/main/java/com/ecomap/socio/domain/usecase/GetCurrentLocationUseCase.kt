package com.ecomap.socio.domain.usecase

import android.location.Location
import com.ecomap.socio.domain.repository.LocationRepository
import javax.inject.Inject

class GetCurrentLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(): Result<Location> {
        return locationRepository.getCurrentLocation()
    }
}
