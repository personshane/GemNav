package com.gemnav.ui.trips

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemnav.app.R
import com.gemnav.app.databinding.FragmentTripHistoryBinding

class TripHistoryFragment : Fragment() {

    private var _binding: FragmentTripHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TripHistoryViewModel by viewModels()

    private lateinit var adapter: TripHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = TripHistoryAdapter()

        binding.recyclerTrips.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTrips.adapter = adapter

        viewModel.trips.collectInLifecycle(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
