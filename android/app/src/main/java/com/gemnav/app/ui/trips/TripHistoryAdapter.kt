package com.gemnav.app.ui.trips

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.gemnav.app.databinding.ItemTripRowBinding
import com.gemnav.trips.TripDisplayModel

class TripHistoryAdapter(
    private val onTripClicked: (Long) -> Unit
) :
    ListAdapter<TripDisplayModel, TripHistoryAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<TripDisplayModel>() {
        override fun areItemsTheSame(oldItem: TripDisplayModel, newItem: TripDisplayModel) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TripDisplayModel, newItem: TripDisplayModel) =
            oldItem == newItem
    }

    inner class ViewHolder(val binding: ItemTripRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTripRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = getItem(position)

        holder.binding.textDate.text = trip.startTimeText
        holder.binding.textDistance.text = trip.distanceText
        holder.binding.textEndTime.text = trip.endTimeText

        holder.binding.root.setOnClickListener {
            onTripClicked(trip.id)
        }
    }
}
