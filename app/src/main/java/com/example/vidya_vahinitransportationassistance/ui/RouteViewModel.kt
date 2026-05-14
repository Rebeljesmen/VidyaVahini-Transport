package com.example.vidya_vahinitransportationassistance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vidya_vahinitransportationassistance.data.MockData
import com.example.vidya_vahinitransportationassistance.data.model.BusPing
import com.example.vidya_vahinitransportationassistance.data.model.BusRoute
import com.example.vidya_vahinitransportationassistance.data.model.ReportType
import com.example.vidya_vahinitransportationassistance.data.model.RouteState
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the state of the bus route.
 */
class RouteViewModel : ViewModel() {

    private val _routeState = MutableStateFlow(MockData.getInitialRouteState())
    val routeState: StateFlow<RouteState> = _routeState.asStateFlow()

    private val _selectedRoute = MutableStateFlow(MockData.availableRoutes[0])
    val selectedRoute: StateFlow<BusRoute> = _selectedRoute.asStateFlow()

    // Firebase Database Reference
    private val database = FirebaseDatabase.getInstance()
    private var routeRef: DatabaseReference? = null

    private val _lastUpdated = MutableStateFlow(0L)
    val lastUpdated = _lastUpdated.asStateFlow()

    private val routeListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val remoteIndex = snapshot.child("currentStopIndex").getValue(Int::class.java)
            val remoteStatus = snapshot.child("status").getValue(String::class.java)
            val remoteTimestamp = snapshot.child("lastUpdated").getValue(Long::class.java)
            
            if (remoteIndex != null && remoteStatus != null) {
                _routeState.update { currentState ->
                    currentState.copy(
                        currentStopIndex = remoteIndex,
                        status = remoteStatus
                    )
                }
            }
            
            if (remoteTimestamp != null) {
                _lastUpdated.value = remoteTimestamp
            }
        }

        override fun onCancelled(error: DatabaseError) {
            // Log or handle error if needed
        }
    }

    init {
        // Start listening to the default route
        setupRouteListener(_selectedRoute.value.id)
    }

    /**
     * Sets up the Firebase listener for a specific route.
     */
    private fun setupRouteListener(routeId: String) {
        // Remove old listener if it exists
        routeRef?.removeEventListener(routeListener)
        
        // Update reference to the new route path
        routeRef = database.getReference("Live_Routes").child(routeId)
        
        // Start listening to the new path
        routeRef?.addValueEventListener(routeListener)
    }

    /**
     * Switches the tracking to a different bus route.
     */
    fun switchRoute(route: BusRoute) {
        if (route.id == _selectedRoute.value.id) return

        _selectedRoute.value = route
        
        // Reset local state for the new route
        _routeState.value = RouteState(
            stops = route.stops,
            currentStopIndex = 0,
            status = "On Time"
        )
        _latestPing.value = null
        _lastPingTime.value = 0L
        _lastUpdated.value = 0L

        // Re-connect Firebase to the new path
        setupRouteListener(route.id)
    }

    override fun onCleared() {
        super.onCleared()
        // Remove listener to prevent memory leaks
        routeRef?.removeEventListener(routeListener)
    }

    private val _latestPing = MutableStateFlow<BusPing?>(null)
    val latestPing: StateFlow<BusPing?> = _latestPing.asStateFlow()

    private val _lastPingTime = MutableStateFlow(0L)
    val lastPingTime = _lastPingTime.asStateFlow()

    // A flow that emits the current time every second to drive the cooldown UI and recency check
    private val currentTimeFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())

    // Expose whether the ping button should be enabled
    val isPingEnabled: StateFlow<Boolean> = combine(currentTimeFlow, _lastPingTime) { now, last ->
        now - last >= 30000
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Expose whether the data is considered outdated (> 30 minutes)
    val isDataOutdated: StateFlow<Boolean> = combine(currentTimeFlow, _lastUpdated) { now, last ->
        if (last == 0L) return@combine false // Assume fresh if no data yet
        now - last > 30 * 60 * 1000 // 30 minutes in milliseconds
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Expose whether the bus is within 100m of the College Campus
    val isWithinCollegeProximity: StateFlow<Boolean> = _routeState.map { state ->
        val stops = state.stops
        if (stops.isEmpty()) return@map false
        
        val currentStop = stops[state.currentStopIndex]
        val destinationStop = stops.last()
        
        val distance = calculateDistance(
            currentStop.latitude, currentStop.longitude,
            destinationStop.latitude, destinationStop.longitude
        )
        distance < 100.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * Helper to calculate distance between two coordinates in meters.
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    // Expose remaining cooldown in seconds
    val cooldownSeconds: StateFlow<Int> = combine(currentTimeFlow, _lastPingTime) { now, last ->
        val remaining = 30 - ((now - last) / 1000).toInt()
        if (remaining > 0) remaining else 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * Updates the current bus location by index.
     */
    fun updateBusLocation(index: Int) {
        if (index in _routeState.value.stops.indices) {
            _routeState.update { currentState ->
                currentState.copy(
                    currentStopIndex = index,
                    // Reset status to On Time if it was Breakdown when moving to a new stop
                    status = if (currentState.status == "Breakdown") "On Time" else currentState.status
                )
            }
        }
    }

    /**
     * Sends a ping from a student, potentially updating the bus location.
     */
    fun sendPing(landmarkName: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - _lastPingTime.value < 30000) return

        _lastPingTime.value = currentTime
        _latestPing.value = BusPing(landmarkName, currentTime)
        
        // Algorithm: Update location if ping is from a stop further along the route
        val stops = _routeState.value.stops
        val pingedIndex = stops.indexOfFirst { it.name == landmarkName }
        
        if (pingedIndex > _routeState.value.currentStopIndex) {
            // Write to Firebase - this will trigger updates for all users
            val updates = mapOf(
                "currentStopIndex" to pingedIndex,
                "status" to "On Time",
                "lastUpdated" to currentTime,
                "priority" to "NORMAL"
            )
            routeRef?.updateChildren(updates)
        }
    }

    /**
     * Convenience method to simulate the bus reaching the next stop.
     * In a real app, this would be replaced by actual student pings.
     */
    fun pingBusLocation() {
        val currentState = _routeState.value
        val nextIndex = (currentState.currentStopIndex + 1) % currentState.stops.size
        val nextLandmark = currentState.stops[nextIndex].name
        sendPing(nextLandmark)
    }

    /**
     * Updates the route status using a ReportType enum.
     */
    fun reportStatus(reportType: ReportType) {
        val currentTime = System.currentTimeMillis()
        
        val statusString = when (reportType) {
            ReportType.ON_TIME -> "On Time"
            ReportType.DELAYED -> "Delayed"
            ReportType.BREAKDOWN -> "Critical"
        }

        val updates = mutableMapOf<String, Any>(
            "status" to statusString,
            "lastUpdated" to currentTime
        )

        // Algorithm: If 'Breakdown' is reported, add a high-priority flag
        if (reportType == ReportType.BREAKDOWN) {
            updates["priority"] = "HIGH"
        }

        routeRef?.updateChildren(updates)
    }

    /**
     * Updates the route status. (Legacy support)
     */
    fun updateStatus(newStatus: String) {
        val reportType = when (newStatus) {
            "Breakdown" -> ReportType.BREAKDOWN
            "Delayed" -> ReportType.DELAYED
            else -> ReportType.ON_TIME
        }
        reportStatus(reportType)
    }

    /**
     * Calculates the cumulative ETA in minutes to the destination.
     */
    fun calculateETAToDestination(): Int {
        val state = _routeState.value
        val stops = state.stops
        val currentIndex = state.currentStopIndex
        
        if (currentIndex >= stops.size - 1) return 0
        
        var totalMinutes = 0
        // Sum travel times for all stops after the current one
        for (i in (currentIndex + 1) until stops.size) {
            totalMinutes += stops[i].standardTravelTimeMinutes
        }
        return totalMinutes
    }
}
