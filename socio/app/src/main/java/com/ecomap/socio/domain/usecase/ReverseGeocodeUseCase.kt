package com.ecomap.socio.domain.usecase

import com.ecomap.socio.domain.repository.LocationRepository
import javax.inject.Inject

class ReverseGeocodeUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(latitude: Double, longitude: Double): Result<String> {
        return locationRepository.reverseGeocode(latitude, longitude)
    }
}
