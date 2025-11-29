package com.example.aharui.ui.maps

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.aharui.R
import com.example.aharui.api.OverpassApiHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@AndroidEntryPoint
class MapsFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var locationOverlay: MyLocationNewOverlay
    private val overpassApi = OverpassApiHelper()

    private var currentLocation: Location? = null
    private val markers = mutableListOf<Marker>()

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                enableMyLocation()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                enableMyLocation()
            }
            else -> {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        Configuration.getInstance().userAgentValue = requireContext().packageName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        filterChipGroup = view.findViewById(R.id.filter_chip_group)
        mapView = view.findViewById(R.id.map)

        setupMap()
        setupFilterChips()
        checkLocationPermission()
    }

    private fun setupMap() {
        mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)

            zoomController.setVisibility(
                org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT
            )

            minZoomLevel = 3.0
            maxZoomLevel = 20.0
        }

        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), mapView)
        locationOverlay.enableMyLocation()
        mapView.overlays.add(locationOverlay)
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableMyLocation()
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationOverlay.enableMyLocation()
            locationOverlay.enableFollowLocation()

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = it
                    val geoPoint = GeoPoint(it.latitude, it.longitude)

                    mapView.controller.animateTo(geoPoint)
                    mapView.controller.setZoom(15.0)

                    // Load default places (gyms)
                    searchNearbyPlaces("gym")
                }
            }
        }
    }

    private fun setupFilterChips() {
        filterChipGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chip_gyms -> searchNearbyPlaces("gym")
                R.id.chip_dieticians -> searchNearbyPlaces("nutritionist")
                R.id.chip_health_food -> searchNearbyPlaces("health food store")
            }
        }
    }

    private fun searchNearbyPlaces(placeType: String) {
        // Check internet connection first
        if (!isNetworkAvailable()) {
            Toast.makeText(
                requireContext(),
                "No internet connection. Please check your network.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Clear existing markers
        markers.forEach { mapView.overlays.remove(it) }
        markers.clear()
        mapView.invalidate()

        currentLocation?.let { location ->
            // Show loading toast
            Toast.makeText(requireContext(), "Searching for nearby places...", Toast.LENGTH_SHORT).show()

            // Use coroutine to fetch real places from Overpass API
            lifecycleScope.launch {
                try {
                    val places = overpassApi.searchNearbyPlaces(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        amenityType = placeType
                    )

                    if (places.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "No $placeType found nearby. Try expanding search area.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        // Add markers for each place
                        places.forEach { place ->
                            val marker = Marker(mapView).apply {
                                position = place.geoPoint
                                title = place.name
                                snippet = "${place.address}\nType: ${place.type}"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                                // Add click listener to show info window
                                setOnMarkerClickListener { marker, _ ->
                                    marker.showInfoWindow()
                                    // Center map on marker when clicked
                                    mapView.controller.animateTo(marker.position)
                                    true
                                }
                            }
                            markers.add(marker)
                            mapView.overlays.add(marker)
                        }

                        mapView.invalidate()

                        Toast.makeText(
                            requireContext(),
                            "Found ${places.size} ${if (places.size == 1) "place" else "places"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Error: ${e.message ?: "Failed to search places"}",
                        Toast.LENGTH_LONG
                    ).show()
                    e.printStackTrace()
                }
            }
        } ?: run {
            Toast.makeText(
                requireContext(),
                "Location not available. Please enable GPS and wait for signal.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
                as android.net.ConnectivityManager

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected == true
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDetach()
    }
}