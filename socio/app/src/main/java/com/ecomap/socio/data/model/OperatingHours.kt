package com.ecomap.socio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OperatingHours(
    @SerialName("monday")
    val monday: DaySchedule = DaySchedule(),

    @SerialName("tuesday")
    val tuesday: DaySchedule = DaySchedule(),

    @SerialName("wednesday")
    val wednesday: DaySchedule = DaySchedule(),

    @SerialName("thursday")
    val thursday: DaySchedule = DaySchedule(),

    @SerialName("friday")
    val friday: DaySchedule = DaySchedule(),

    @SerialName("saturday")
    val saturday: DaySchedule = DaySchedule(),

    @SerialName("sunday")
    val sunday: DaySchedule = DaySchedule()
)

@Serializable
data class TimeRange(
    @SerialName("open_time")
    val openTime: String, // HH:mm format

    @SerialName("close_time")
    val closeTime: String // HH:mm format
)

@Serializable
data class DaySchedule(
    @SerialName("is_open")
    val isOpen: Boolean = false,

    @SerialName("shifts")
    val shifts: List<TimeRange> = emptyList() // Lista de turnos (soporta horario partido)
)
