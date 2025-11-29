package com.example.aharui.data.routing

import com.google.gson.annotations.SerializedName

data class OSRMResponse(
    @SerializedName("code") val code: String,
    @SerializedName("routes") val routes: List<Route>?
)

data class Route(
    @SerializedName("geometry") val geometry: String,
    @SerializedName("legs") val legs: List<Leg>,
    @SerializedName("distance") val distance: Double,
    @SerializedName("duration") val duration: Double
)

data class Leg(
    @SerializedName("steps") val steps: List<Step>,
    @SerializedName("distance") val distance: Double,
    @SerializedName("duration") val duration: Double
)

data class Step(
    @SerializedName("maneuver") val maneuver: Maneuver,
    @SerializedName("name") val name: String,
    @SerializedName("distance") val distance: Double,
    @SerializedName("duration") val duration: Double
)

data class Maneuver(
    @SerializedName("type") val type: String,
    @SerializedName("instruction") val instruction: String,
    @SerializedName("location") val location: List<Double>
)