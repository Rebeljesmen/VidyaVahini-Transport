package com.example.vidya_vahinitransportationassistance.data.model

/**
 * Data class representing a specific bus route.
 */
data class BusRoute(
    val id: String,
    val name: String,
    val stops: List<BusStop>
)
