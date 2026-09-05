package com.ecomap.socio.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApi {

    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "json"
    ): NominatimResponse
}

data class NominatimResponse(
    @SerializedName("display_name")
    val displayName: String,

    @SerializedName("address")
    val address: NominatimAddress?
)

data class NominatimAddress(
    @SerializedName("road")
    val road: String?,

    @SerializedName("suburb")
    val suburb: String?,

    @SerializedName("city")
    val city: String?,

    @SerializedName("state")
    val state: String?,

    @SerializedName("postcode")
    val postcode: String?,

    @SerializedName("country")
    val country: String?
)
