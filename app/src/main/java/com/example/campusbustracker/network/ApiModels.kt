package com.example.campusbustracker.network

data class ScanRequest(
    val bus_code: String,
    val qr_code_value: String
)

data class ScanResponse(
    val message: String? = null,
    val error: String? = null
)

data class BusStatus(
    val bus_id: String,
    val bus_code: String,
    val checkpoint_name: String,
    val scanned_at: String
)

data class StatusResponse(
    val buses: List<BusStatus>
)

data class LoginRequest(val email: String, val password: String)
data class SignupRequest(val name: String, val email: String, val password: String, val role: String)
data class AuthResponse(
    val token: String? = null,
    val role: String? = null,
    val name: String? = null,
    val error: String? = null
)

data class ManualCheckinRequest(
    val bus_code: String,
    val checkpoint_name: String,
    val action: String = "ARRIVED",
    val lat: Double? = null,
    val lng: Double? = null
)

data class ManualCheckinResponse(
    val message: String? = null,
    val bus_code: String? = null,
    val checkpoint_name: String? = null,
    val action: String? = null,
    val geofence_verified: Boolean? = null,
    val distance_meters: Int? = null,
    val error: String? = null
)

data class AnalyticsResponse(
    val total_checkins: Int = 0,
    val total_trips: Int = 0,
    val on_time_percentage: Double = 0.0,
    val late_checkins_count: Int = 0,
    val active_buses_count: Int = 0,
    val hardware_cost_savings: String = "$0.00 / Bus"
)

data class RouteStopItem(
    val stop_id: String,
    val stop_name: String,
    val lat: Double,
    val lng: Double,
    val is_physical_tag: Boolean = false
)

data class RouteItem(
    val route_id: String,
    val route_name: String,
    val stops: List<RouteStopItem> = emptyList()
)

data class RoutesResponse(
    val routes: List<RouteItem> = emptyList()
)