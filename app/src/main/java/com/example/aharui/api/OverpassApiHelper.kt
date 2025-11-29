package com.example.aharui.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Helper class to query OpenStreetMap data using Overpass API
 * Documentation: https://wiki.openstreetmap.org/wiki/Overpass_API
 */
class OverpassApiHelper {

    companion object {
        private const val TAG = "OverpassApiHelper"
        private const val OVERPASS_API_URL = "https://overpass-api.de/api/interpreter"
        private const val SEARCH_RADIUS_METERS = 5000 // Fixed: Changed from 50000 to 5000 (5km)
    }

    data class POIResult(
        val name: String,
        val geoPoint: GeoPoint,
        val address: String,
        val type: String
    )

    /**
     * Search for nearby places using Overpass API
     */
    suspend fun searchNearbyPlaces(
        latitude: Double,
        longitude: Double,
        amenityType: String
    ): List<POIResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Searching for $amenityType near ($latitude, $longitude)")

            val query = buildOverpassQuery(latitude, longitude, amenityType)
            Log.d(TAG, "Query: $query")

            val response = executeQuery(query)
            Log.d(TAG, "Response received, parsing...")

            val results = parseResponse(response)
            Log.d(TAG, "Found ${results.size} places")

            results
        } catch (e: Exception) {
            Log.e(TAG, "Error searching places", e)
            emptyList()
        }
    }

    private fun buildOverpassQuery(lat: Double, lon: Double, amenity: String): String {
        // Fixed: Improved OSM tag mapping with correct tags
        val osmQuery = when (amenity.lowercase()) {
            "gym", "gyms" -> """
                (
                  node["leisure"="fitness_centre"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  way["leisure"="fitness_centre"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  relation["leisure"="fitness_centre"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  node["leisure"="sports_centre"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  way["leisure"="sports_centre"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  relation["leisure"="sports_centre"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  node["sport"="fitness"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  way["sport"="fitness"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                );
            """.trimIndent()

            "nutritionist", "dietician", "dieticians" -> """
                (
                  node["healthcare"="nutrition_counselling"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  way["healthcare"="nutrition_counselling"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  node["healthcare"="dietician"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  way["healthcare"="dietician"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  node["amenity"="clinic"]["healthcare:speciality"~"[Nn]utrition|[Dd]iet"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  way["amenity"="clinic"]["healthcare:speciality"~"[Nn]utrition|[Dd]iet"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  node["amenity"="doctors"]["healthcare:speciality"~"[Nn]utrition|[Dd]iet"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  way["amenity"="doctors"]["healthcare:speciality"~"[Nn]utrition|[Dd]iet"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  node["name"~"[Nn]utrition|[Dd]iet"]["amenity"="clinic"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  way["name"~"[Nn]utrition|[Dd]iet"]["amenity"="clinic"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                );
            """.trimIndent()

            "health food store", "health food" -> """
                (
                  node["shop"="health_food"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  way["shop"="health_food"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  node["shop"="nutrition_supplements"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  way["shop"="nutrition_supplements"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  node["shop"="organic"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  way["shop"="organic"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                );
            """.trimIndent()

            "restaurant" -> """
                (
                  node["amenity"="restaurant"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  way["amenity"="restaurant"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                );
            """.trimIndent()

            "pharmacy" -> """
                (
                  node["amenity"="pharmacy"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  way["amenity"="pharmacy"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                );
            """.trimIndent()

            "hospital" -> """
                (
                  node["amenity"="hospital"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  way["amenity"="hospital"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                );
            """.trimIndent()

            "cafe" -> """
                (
                  node["amenity"="cafe"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  way["amenity"="cafe"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                );
            """.trimIndent()

            "doctor" -> """
                (
                  node["amenity"="doctors"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  way["amenity"="doctors"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                );
            """.trimIndent()

            else -> """
                (
                  node["amenity"="$amenity"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                  way["amenity"="$amenity"](around:$SEARCH_RADIUS_METERS,$lat,$lon);
                );
            """.trimIndent()
        }

        return """
            [out:json][timeout:25];
            $osmQuery
            out body center;
        """.trimIndent()
    }

    private fun executeQuery(query: String): String {
        val connection = URL(OVERPASS_API_URL).openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "POST"
                connectTimeout = 30000  // Increased timeout
                readTimeout = 30000     // Increased timeout
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }

            // Send query as POST data
            val postData = "data=${URLEncoder.encode(query, "UTF-8")}"
            connection.outputStream.use { os ->
                os.write(postData.toByteArray())
                os.flush()
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                return response.toString()
            } else {
                val errorStream = connection.errorStream
                if (errorStream != null) {
                    val errorReader = BufferedReader(InputStreamReader(errorStream))
                    val error = errorReader.readText()
                    Log.e(TAG, "Error response: $error")
                }
                throw Exception("HTTP error code: $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseResponse(jsonResponse: String): List<POIResult> {
        val results = mutableListOf<POIResult>()

        try {
            val jsonObject = JSONObject(jsonResponse)
            val elements = jsonObject.getJSONArray("elements")

            Log.d(TAG, "Parsing ${elements.length()} elements")

            for (i in 0 until elements.length()) {
                try {
                    val element = elements.getJSONObject(i)
                    val tags = element.optJSONObject("tags")

                    if (tags == null) {
                        Log.d(TAG, "Element $i has no tags, skipping")
                        continue
                    }

                    // Fixed: Allow places without names but log them
                    val name = tags.optString("name", "").ifEmpty {
                        tags.optString("operator", "").ifEmpty {
                            tags.optString("brand", "Unnamed Location")
                        }
                    }

                    // Get coordinates
                    val lat: Double
                    val lon: Double

                    when {
                        element.has("lat") && element.has("lon") -> {
                            // Node
                            lat = element.getDouble("lat")
                            lon = element.getDouble("lon")
                        }
                        element.has("center") -> {
                            // Way or Relation with center
                            val center = element.getJSONObject("center")
                            lat = center.getDouble("lat")
                            lon = center.getDouble("lon")
                        }
                        else -> {
                            Log.d(TAG, "Element $i has no coordinates, skipping")
                            continue
                        }
                    }

                    val address = buildAddress(tags)
                    val type = getPlaceType(tags)

                    val poi = POIResult(
                        name = name,
                        geoPoint = GeoPoint(lat, lon),
                        address = address,
                        type = type
                    )

                    results.add(poi)
                    Log.d(TAG, "Added place: ${poi.name} at (${poi.geoPoint.latitude}, ${poi.geoPoint.longitude})")

                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing element $i", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response", e)
        }

        return results
    }

    private fun getPlaceType(tags: JSONObject): String {
        return when {
            tags.has("leisure") -> tags.getString("leisure")
            tags.has("amenity") -> tags.getString("amenity")
            tags.has("shop") -> tags.getString("shop")
            tags.has("healthcare") -> tags.getString("healthcare")
            tags.has("sport") -> tags.getString("sport")
            else -> "place"
        }
    }

    private fun buildAddress(tags: JSONObject): String {
        val parts = mutableListOf<String>()

        // Try to build a complete address
        val houseNumber = tags.optString("addr:housenumber", "")
        val street = tags.optString("addr:street", "")
        val city = tags.optString("addr:city", "")
        val postcode = tags.optString("addr:postcode", "")
        val suburb = tags.optString("addr:suburb", "")

        if (houseNumber.isNotEmpty() && street.isNotEmpty()) {
            parts.add("$houseNumber $street")
        } else if (street.isNotEmpty()) {
            parts.add(street)
        }

        if (suburb.isNotEmpty()) {
            parts.add(suburb)
        }

        if (city.isNotEmpty()) {
            if (postcode.isNotEmpty()) {
                parts.add("$city $postcode")
            } else {
                parts.add(city)
            }
        }

        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            "Address not available"
        }
    }
}