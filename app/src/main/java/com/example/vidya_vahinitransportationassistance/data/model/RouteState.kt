package com.example.vidya_vahinitransportationassistance.data.model

/**
 * Data class representing the current state of a bus route.
 */
data class RouteState(
    val stops: List<BusStop>,
    val currentStopIndex: Int,
    val status: String
)
