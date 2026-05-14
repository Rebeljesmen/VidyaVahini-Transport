package com.example.vidya_vahinitransportationassistance.data

import com.example.vidya_vahinitransportationassistance.data.model.BusRoute
import com.example.vidya_vahinitransportationassistance.data.model.BusStop
import com.example.vidya_vahinitransportationassistance.data.model.RouteState

object MockData {
    val northCampusStops = listOf(
        BusStop("Village Square", 12.9716, 77.5946, 0),
        BusStop("Primary School", 12.9720, 77.5950, 5),
        BusStop("Market Junction", 12.9730, 77.5960, 8),
        BusStop("Forest Road", 12.9740, 77.5970, 7),
        BusStop("College Campus", 12.9750, 77.5980, 10)
    )

    val westRuralStops = listOf(
        BusStop("West Gate", 12.9800, 77.5000, 0),
        BusStop("River Bridge", 12.9810, 77.5100, 12),
        BusStop("Mountain Pass", 12.9820, 77.5200, 15),
        BusStop("College Campus", 12.9750, 77.5980, 8)
    )

    val availableRoutes = listOf(
        BusRoute("route_a", "Route A: North Campus", northCampusStops),
        BusRoute("route_b", "Route B: West Rural", westRuralStops)
    )

    fun getInitialRouteState(routeId: String = "route_a"): RouteState {
        val route = availableRoutes.find { it.id == routeId } ?: availableRoutes[0]
        return RouteState(
            stops = route.stops,
            currentStopIndex = 0,
            status = "On Time"
        )
    }
}
