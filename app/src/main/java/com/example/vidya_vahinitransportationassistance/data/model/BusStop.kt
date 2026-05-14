package com.example.vidya_vahinitransportationassistance.data.model

/**
 * Data class representing a bus stop with geographical coordinates.
 */
data class BusStop(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val standardTravelTimeMinutes: Int = 0
)
