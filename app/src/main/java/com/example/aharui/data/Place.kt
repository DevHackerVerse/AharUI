package com.example.aharui.data

import org.osmdroid.util.GeoPoint

/**
 * Data class representing a place/location
 */
data class Place(
    val id: String = "",
    val name: String,
    val address: String,
    val geoPoint: GeoPoint,
    val distance: String = "",
    val rating: String = "",
    val type: String = "",
    val phoneNumber: String = "",
    val openingHours: String = ""
) {
    // Helper to calculate distance from current location
    fun calculateDistance(currentLocation: GeoPoint): Double {
        return geoPoint.distanceToAsDouble(currentLocation)
    }

    // Format distance in km or meters
    fun getFormattedDistance(currentLocation: GeoPoint): String {
        val distanceMeters = calculateDistance(currentLocation)
        return when {
            distanceMeters < 1000 -> "${distanceMeters.toInt()} m"
            else -> String.format("%.1f km", distanceMeters / 1000)
        }
    }
}