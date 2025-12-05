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
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions

class TripDetailsFragment : Fragment() {

    private var _binding: FragmentTripDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: TripDetailsFragmentArgs by navArgs()
    private val viewModel: TripDetailsViewModel by viewModels()

    private var map: GoogleMap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.loadTrip(args.tripId)

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync { gMap ->
            map = gMap
            renderRoute() // if route already loaded
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

    private fun renderRoute() {
        val gMap = map ?: return
        val route = viewModel.route.value ?: return
        val polyline = route.polyline

        gMap.clear()
        gMap.addPolyline(polyline)

        val points = polyline.points
        if (points.isNotEmpty()) {
            // Start marker
            gMap.addMarker(MarkerOptions().position(points.first()).title("Start"))
            // End marker
            gMap.addMarker(MarkerOptions().position(points.last()).title("End"))

            val bounds = LatLngBounds.builder().apply {
                points.forEach { include(it) }
            }.build()

            gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        }
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
