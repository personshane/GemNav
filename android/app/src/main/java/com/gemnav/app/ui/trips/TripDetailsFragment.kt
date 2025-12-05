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

class TripDetailsFragment : Fragment() {

    private var _binding: FragmentTripDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: TripDetailsFragmentArgs by navArgs()
    private val viewModel: TripDetailsViewModel by viewModels()

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

        viewModel.trip.collectInLifecycle(viewLifecycleOwner) { trip ->
            if (trip != null) {
                binding.textTripId.text = "Trip ID: ${trip.id}"
                binding.textStartTime.text = "Start: ${trip.startTimeText}"
                binding.textEndTime.text = "End: ${trip.endTimeText}"
                binding.textDistance.text = "Distance: ${trip.distanceText}"
            }
        }

        viewModel.route.collectInLifecycle(viewLifecycleOwner) { overlay ->
            // Map integration happens in MP-027
            // For now: no-op placeholder
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
