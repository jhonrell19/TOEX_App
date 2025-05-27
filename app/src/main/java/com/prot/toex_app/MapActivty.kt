package com.prot.toex_app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.prot.toex_app.databinding.ActivityMapBinding
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope // Replace with lifecycleScope or viewModelScope in production
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.api.IMapController
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit // For formatting duration

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    private lateinit var map: MapView
    private lateinit var mapController: IMapController
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocationGeoPoint: GeoPoint? = null
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null
    private var currentRouteOverlay: Polyline? = null
    private var myLocationOverlay: MyLocationNewOverlay? = null

    private val locationRequestCode = 1001
    private val BUS_TRANSPORT_TYPE = "Bus"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = "com.prot.toex_app/1.0" // IMPORTANT!

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMap()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationPermissions()
        setupUIListeners()

        // Initial UI state
        binding.cardViewFareDetails.visibility = View.GONE
        binding.textViewFullAddress.visibility = View.GONE
    }

    private fun setupMap() {
        map = binding.mapView
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        mapController = map.controller
        mapController.setZoom(12.0)
        mapController.setCenter(GeoPoint(14.5995, 120.9842)) // Default to Manila

        val rotationGestureOverlay = RotationGestureOverlay(map)
        rotationGestureOverlay.isEnabled = true
        map.overlays.add(rotationGestureOverlay)

        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        //myLocationOverlay?.enableFollowLocation(false)
        myLocationOverlay?.enableMyLocation()
        map.overlays.add(0, myLocationOverlay)
    }

    private fun setupUIListeners() {
        binding.editTextDestination.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                val destinationQuery = v.text.toString().trim()
                if (destinationQuery.isNotBlank()) {
                    geocodeAndSetDestination(destinationQuery)
                } else {
                    Toast.makeText(this, "Please enter a destination.", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }

        binding.fabRecenter.setOnClickListener {
            myLocationOverlay?.myLocation?.let { geoPoint ->
                mapController.animateTo(geoPoint)
                mapController.setZoom(17.0)
            } ?: run {
                Toast.makeText(this, "Current location not available yet. Trying...", Toast.LENGTH_SHORT).show()
                requestNewLocationData()
            }
        }

        binding.buttonCalculateOrStop.setOnClickListener {
            if (binding.buttonCalculateOrStop.text.toString().equals(getString(R.string.stop_current_trip_button), ignoreCase = true)) {
                clearRouteAndFareDetails()
                binding.buttonCalculateOrStop.text = getString(R.string.calculate_bus_fare_button)
                binding.editTextDestination.text.clear()
                binding.editTextDestination.isEnabled = true
            } else {
                hideKeyboard()
                val destinationQuery = binding.editTextDestination.text.toString().trim()
                if (destinationQuery.isNotBlank()) {
                    geocodeAndSetDestination(destinationQuery) // This will trigger route calculation
                } else if (endMarker?.position != null && startMarker?.position != null) {
                    getRouteAndCalculateFare(startMarker!!.position, endMarker!!.position)
                }
                else {
                    Toast.makeText(this, "Please enter a destination.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.editTextDestination.windowToken, 0)
    }

    private fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), locationRequestCode)
        } else {
            enableLocationTrackingAndFirstFix()
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationTrackingAndFirstFix() {
        binding.textViewCurrentLocationValue.text = "Tracking..." // Update display
        myLocationOverlay?.enableMyLocation()
        myLocationOverlay?.runOnFirstFix {
            runOnUiThread {
                myLocationOverlay?.myLocation?.let { firstFixLocation ->
                    currentLocationGeoPoint = firstFixLocation
                    if (startMarker == null) { // Only pan aggressively on very first fix
                        mapController.animateTo(currentLocationGeoPoint, 16.0, 1500L)
                    }
                    updateStartMarker(currentLocationGeoPoint!!)
                    // Update the non-editable current location display
                    geocodeAndDisplayCurrentLocation(currentLocationGeoPoint!!)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100)
            .setMinUpdateIntervalMillis(50)
            .setMaxUpdates(1)
            .build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                currentLocationGeoPoint = GeoPoint(location.latitude, location.longitude)
                if (startMarker == null) {
                    mapController.animateTo(currentLocationGeoPoint, 16.0, 1000L)
                }
                updateStartMarker(currentLocationGeoPoint!!)
                geocodeAndDisplayCurrentLocation(currentLocationGeoPoint!!)
                fusedLocationClient.removeLocationUpdates(this)
            }
        }
    }

    private fun geocodeAndDisplayCurrentLocation(geoPoint: GeoPoint) {
        GlobalScope.launch(Dispatchers.IO) {
            val geocoder = Geocoder(this@MapActivity, Locale.getDefault())
            try {
                @Suppress("DEPRECATION")
                val addresses: List<Address>? = geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val addressText = address.getAddressLine(0) ?: "Current Location"
                    withContext(Dispatchers.Main) {
                        binding.textViewCurrentLocationValue.text = addressText
                    }
                } else {
                    withContext(Dispatchers.Main) { binding.textViewCurrentLocationValue.text = "Address not found" }
                }
            } catch (e: IOException) {
                Log.e("GEOCODER_CURRENT", "Failed to geocode current location", e)
                withContext(Dispatchers.Main) { binding.textViewCurrentLocationValue.text = "Cannot get address" }
            }
        }
    }


    private fun updateStartMarker(geoPoint: GeoPoint) {
        startMarker?.let { map.overlays.remove(it) }
        startMarker = Marker(map).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Your Location"
            // TODO: Set custom start marker icon
            // icon = ContextCompat.getDrawable(this@MapActivity, R.drawable.ic_marker_start_custom)
        }
        map.overlays.add(startMarker)
        map.invalidate()
    }

    private fun updateEndMarker(geoPoint: GeoPoint, addressString: String?) {
        endMarker?.let { map.overlays.remove(it) }
        endMarker = Marker(map).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Destination"
            snippet = addressString ?: "Selected Location"
            // TODO: Set custom end marker icon
            // icon = ContextCompat.getDrawable(this@MapActivity, R.drawable.ic_marker_end_custom)
        }
        map.overlays.add(endMarker)
        binding.textViewFullAddress.text = addressString ?: "Address not available"
        binding.textViewFullAddress.visibility = View.VISIBLE
        map.invalidate()
    }

    private fun clearRouteAndFareDetails() {
        currentRouteOverlay?.let { map.overlays.remove(it) }
        currentRouteOverlay = null
        endMarker?.let { map.overlays.remove(it) }
        endMarker = null

        binding.cardViewFareDetails.visibility = View.GONE
        binding.textViewFullAddress.visibility = View.GONE
        map.invalidate()
    }

    private fun geocodeAndSetDestination(destinationQuery: String) {
        if (destinationQuery.isBlank()) return
        Toast.makeText(this, "Searching for '$destinationQuery'...", Toast.LENGTH_SHORT).show()

        GlobalScope.launch(Dispatchers.IO) {
            val geocoder = Geocoder(this@MapActivity, Locale.getDefault())
            try {
                @Suppress("DEPRECATION")
                val addresses: List<Address>? = geocoder.getFromLocationName(destinationQuery, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val destinationGeoPoint = GeoPoint(address.latitude, address.longitude)
                    val fullAddress = (0..address.maxAddressLineIndex).joinToString(separator = "\n") {
                        address.getAddressLine(it) ?: ""
                    }
                    withContext(Dispatchers.Main) {
                        updateEndMarker(destinationGeoPoint, fullAddress)
                        if (startMarker?.position != null) {
                            getRouteAndCalculateFare(startMarker!!.position, destinationGeoPoint)
                        } else {
                            Toast.makeText(this@MapActivity, "Your location not found. Please wait.", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MapActivity, "Destination '$destinationQuery' not found.", Toast.LENGTH_SHORT).show()
                        binding.textViewFullAddress.text = "Destination not found"
                        binding.textViewFullAddress.visibility = View.VISIBLE
                    }
                }
            } catch (e: IOException) {
                Log.e("GEOCODER_DEST", "Geocoding service failed for '$destinationQuery'", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MapActivity, "Geocoding service unavailable.", Toast.LENGTH_SHORT).show()
                    binding.textViewFullAddress.text = "Could not geocode destination"
                    binding.textViewFullAddress.visibility = View.VISIBLE
                }
            } catch (e: IllegalArgumentException) {
                Log.e("GEOCODER_DEST", "Invalid destination query '$destinationQuery'", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MapActivity, "Invalid destination. Try being more specific.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getRouteAndCalculateFare(start: GeoPoint, end: GeoPoint) {
        binding.buttonCalculateOrStop.text = getString(R.string.stop_current_trip_button)
        binding.editTextDestination.isEnabled = false // Disable editing while calculating/showing route
        Toast.makeText(this, "Calculating bus route...", Toast.LENGTH_SHORT).show()
        val roadManager: RoadManager = OSRMRoadManager(this, Configuration.getInstance().userAgentValue)
        (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_CAR)

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val waypoints = ArrayList<GeoPoint>()
                waypoints.add(start)
                waypoints.add(end)
                val road: Road = roadManager.getRoad(waypoints)

                withContext(Dispatchers.Main) {
                    if (road.mStatus == Road.STATUS_OK && road.mLength > 0) {
                        drawRouteOnMap(road)
                        val distanceKm = road.mLength
                        val durationSeconds = road.mDuration
                        val durationFormatted = formatDuration(durationSeconds)

                        val fare = calculateFareInternal(distanceKm, BUS_TRANSPORT_TYPE)

                        binding.textViewEstimatedTime.text = getString(R.string.estimated_time_with_label, durationFormatted)
                        binding.textViewFareAmount.text = getString(R.string.fare_amount_with_label, "₱%.2f".format(fare))
                        binding.textViewDistance.text = getString(R.string.distance_with_label, distanceKm)
                        binding.cardViewFareDetails.visibility = View.VISIBLE

                        if (road.mBoundingBox != null) {
                            map.zoomToBoundingBox(road.mBoundingBox, true, 150)
                        }
                    } else {
                        Toast.makeText(this@MapActivity, "Could not calculate bus route. Status: ${road.mStatus}", Toast.LENGTH_LONG).show()
                        Log.e("OSM_ROUTE", "Error Status for Bus Route: ${road.mStatus}")
                        binding.cardViewFareDetails.visibility = View.GONE
                        binding.buttonCalculateOrStop.text = getString(R.string.calculate_bus_fare_button)
                        binding.editTextDestination.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                Log.e("OSM_ROUTE_EXCEPTION", "Error getting bus road", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MapActivity, "Bus routing service error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    binding.cardViewFareDetails.visibility = View.GONE
                    binding.buttonCalculateOrStop.text = getString(R.string.calculate_bus_fare_button)
                    binding.editTextDestination.isEnabled = true
                }
            }
        }
    }

    private fun formatDuration(totalSeconds: Double): String {
        val hours = TimeUnit.SECONDS.toHours(totalSeconds.toLong())
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds.toLong()) % 60
        return when {
            hours > 0 -> String.format("%d hr %02d min", hours, minutes)
            minutes > 0 -> String.format("%d min", minutes)
            else -> String.format("%.0f sec", totalSeconds)
        }
    }


    private fun drawRouteOnMap(road: Road) {
        currentRouteOverlay?.let { map.overlays.remove(it) }
        currentRouteOverlay = null
        // Don't remove endMarker here, it's set by geocodeAndSetDestination and updated by actual route end
        // startMarker is continuously updated by location services

        // Update/ensure start marker is present
        startMarker?.position?.let { updateStartMarker(it) }
            ?: currentLocationGeoPoint?.let { updateStartMarker(it) }

        // Update end marker to the actual end point of the calculated route
        if (road.mNodes.isNotEmpty()) {
            val actualRouteEndPoint = road.mNodes.last().mLocation
            updateEndMarker(actualRouteEndPoint, binding.textViewFullAddress.text.toString()) // Reuse geocoded address for snippet
        }

        val roadOverlay: Polyline = RoadManager.buildRoadOverlay(road)
        roadOverlay.color = ContextCompat.getColor(this, R.color.medium_blue_accent) // Use a color from your resources
        roadOverlay.width = 12.0f
        currentRouteOverlay = roadOverlay
        map.overlays.add(roadOverlay) // Add route above markers by default, or manage z-index
        map.invalidate()
    }

    private fun calculateFareInternal(distanceKm: Double, transportType: String): Double {
        // Ensure this is only called for BUS_TRANSPORT_TYPE
        if (transportType != BUS_TRANSPORT_TYPE) {
            Log.w("FARE_CALC", "Warning: calculateFareInternal called for unexpected type: $transportType")
            return 0.0 // Or handle as an error
        }

        // Philippines Bus Fare (Example - This needs to be accurate for your target area/bus type!)
        // LTFRB rates can be complex (ordinary, aircon, provincial, city, etc.)
        // This is a simplified placeholder.
        val baseFare = 15.00 // e.g., First 5 kilometers for aircon city bus
        val minimumDistanceKm = 5.0
        val perKmRate = 2.65  // e.g., For succeeding kilometers

        return if (distanceKm <= minimumDistanceKm) {
            baseFare
        } else {
            baseFare + ((distanceKm - minimumDistanceKm) * perKmRate)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationRequestCode) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                enableLocationTrackingAndFirstFix()
            } else {
                Toast.makeText(this, "Location permission is required for map features.", Toast.LENGTH_LONG).show()
                binding.textViewCurrentLocationValue.text = "Location Permission Denied"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        myLocationOverlay?.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        myLocationOverlay?.disableMyLocation()
        fusedLocationClient.removeLocationUpdates(locationCallback) // Stop any FusedLocationProvider updates
    }

    override fun onDestroy() {
        super.onDestroy()
        map.onDetach()
    }
}