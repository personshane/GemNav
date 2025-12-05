package com.gemnav.app.ui.trips

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.gemnav.app.databinding.FragmentTripDetailsBinding

class TripDetailsFragment : Fragment() {

    private var _binding: FragmentTripDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: TripDetailsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.textTripId.text = "Trip ID: ${args.tripId}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
