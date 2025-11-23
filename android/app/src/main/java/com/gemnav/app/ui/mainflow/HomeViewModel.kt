package com.gemnav.app.ui.mainflow

import androidx.lifecycle.ViewModel
import com.gemnav.app.models.Destination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel : ViewModel() {
    private val _favorites = MutableStateFlow<List<Destination>>(emptyList())
    val favorites: StateFlow<List<Destination>> = _favorites

    private val _recent = MutableStateFlow<List<Destination>>(emptyList())
    val recent: StateFlow<List<Destination>> = _recent

    private val _home = MutableStateFlow<Destination?>(null)
    val home: StateFlow<Destination?> = _home

    private val _work = MutableStateFlow<Destination?>(null)
    val work: StateFlow<Destination?> = _work

    fun loadMockData() {
        _favorites.value = listOf(
            Destination(
                name = "Favorite 1",
                address = "123 Main St",
                latitude = 33.4484,
                longitude = -112.0740
            ),
            Destination(
                name = "Favorite 2",
                address = "555 Center Ave",
                latitude = 33.5484,
                longitude = -112.1740
            )
        )
        _recent.value = listOf(
            Destination(
                name = "Truck Stop",
                address = "AZ-95 Exit 12",
                latitude = 34.0484,
                longitude = -113.0740
            ),
            Destination(
                name = "Warehouse 32A",
                address = "Industrial Rd",
                latitude = 33.3484,
                longitude = -112.2740
            )
        )
        _home.value = Destination(
            name = "Home",
            address = "My House",
            latitude = 33.4484,
            longitude = -112.0740
        )
        _work.value = Destination(
            name = "Work",
            address = "Distribution Center",
            latitude = 33.5484,
            longitude = -112.1740
        )
    }
}
