package com.gemnav.app.ui.trips

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.gemnav.app.databinding.FragmentTripDetailsBinding
import com.gemnav.app.trips.RouteOverlayModel
import com.gemnav.app.trips.LiveTripController
import com.gemnav.app.MapThemePreferences
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MapStyleOptions
import com.gemnav.app.R

class TripDetailsFragment : Fragment() {

    private var _binding: FragmentTripDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: TripDetailsFragmentArgs by navArgs()
    private val viewModel: TripDetailsViewModel by viewModels()

    private lateinit var themePrefs: MapThemePreferences
    private var map: GoogleMap? = null
    private var liveTrip: LiveTripController? = null
    private var mapPolyline: com.google.android.gms.maps.model.Polyline? = null
    private var autoFollowCamera = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        themePrefs = MapThemePreferences(requireContext())
        viewModel.loadTrip(args.tripId)

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync { gMap ->
            map = gMap
            applyMapTheme(gMap)
            renderRoute() // if route already loaded
        }

        binding.buttonToggleTheme.setOnClickListener {
            val isDark = themePrefs.toggleTheme()
            updateThemeButton(isDark)
            map?.let { applyMapTheme(it) }
        }

        // Initialize theme button appearance
        updateThemeButton(themePrefs.isDarkMode())

        binding.buttonRecenter.setOnClickListener {
            renderRoute()
        }

        binding.buttonStartTrip.setOnClickListener {
            autoFollowCamera = true
            startLiveTrip()
        }

        binding.buttonStopTrip.setOnClickListener {
            stopLiveTrip()
            autoFollowCamera = false
        }

        viewModel.trip.collectInLifecycle(viewLifecycleOwner) { trip ->
            if (trip != null) {
                binding.textTripId.text = "Trip ID: ${trip.id}"
                binding.textStartTime.text = "Start: ${trip.startTimeText}"
                binding.textEndTime.text = "End: ${trip.endTimeText}"
                binding.textDistance.text = "Distance: ${trip.distanceText}"
            }
        }

        viewModel.route.collectInLifecycle(viewLifecycleOwner) { overlay ->
            if (overlay != null) renderRoute()
        }
    }

    private fun startLiveTrip() {
        liveTrip = LiveTripController(requireContext())

        liveTrip?.start { poly, dist ->
            if (map == null) return@start

            val gMap = map!!

            // remove previous polyline
            mapPolyline?.remove()

            // add updated polyline
            mapPolyline = gMap.addPolyline(poly)

            // camera follow mode
            if (autoFollowCamera && poly.points.isNotEmpty()) {
                val last = poly.points.last()
                gMap.animateCamera(CameraUpdateFactory.newLatLng(last))
            }

            // Update UI distance if needed
            binding.textDistanceLive?.text = String.format("%.2f km", dist / 1000.0)
        }
    }

    private fun stopLiveTrip() {
        liveTrip?.stop()
        liveTrip = null
    }

    private fun renderRoute() {
        val gMap = map ?: return
        val route = viewModel.route.value ?: return
        val polyline = route.polyline

        gMap.clear()
        gMap.addPolyline(polyline)

        val points = polyline.points
        if (points.isNotEmpty()) {
            // Enable all map gestures
            gMap.uiSettings.apply {
                isZoomControlsEnabled = true
                isZoomGesturesEnabled = true
                isScrollGesturesEnabled = true
                isRotateGesturesEnabled = true
                isTiltGesturesEnabled = true
                isCompassEnabled = true
            }
            
            val startIcon = BitmapDescriptorFactory.fromResource(R.drawable.start_marker)
            val endIcon = BitmapDescriptorFactory.fromResource(R.drawable.end_marker)

            gMap.addMarker(
                MarkerOptions()
                    .position(points.first())
                    .title("Start")
                    .icon(startIcon)
                    .anchor(0.5f, 1f)
                    .zIndex(10f)
            )

            gMap.addMarker(
                MarkerOptions()
                    .position(points.last())
                    .title("End")
                    .icon(endIcon)
                    .anchor(0.5f, 1f)
                    .zIndex(10f)
            )

            val bounds = LatLngBounds.builder().apply {
                points.forEach { include(it) }
            }.build()

            val update = CameraUpdateFactory.newLatLngBounds(bounds, 150)
            gMap.animateCamera(update)
        }
    }

    private fun applyMapTheme(gMap: GoogleMap) {
        val isDark = themePrefs.isDarkMode()
        val style = if (isDark) R.raw.map_style_dark else R.raw.map_style_light
        gMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), style))
    }

    private fun updateThemeButton(isDark: Boolean) {
        binding.buttonToggleTheme.text = if (isDark) "☀️" else "🌙"
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.onDestroy()
        _binding = null
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }
}
