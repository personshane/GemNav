package com.gemnav.app.truck.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.gemnav.app.truck.domain.TruckProfileManager
import com.gemnav.app.truck.model.HazmatClass
import com.gemnav.app.truck.model.TruckProfile
import com.gemnav.app.truck.model.TruckType

class TruckProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val manager = TruckProfileManager(application)

    private val _profile = MutableLiveData<TruckProfile>()
    val profile: LiveData<TruckProfile> = _profile

    init {
        _profile.value = manager.getProfile()
    }

    fun updateName(name: String) {
        _profile.value = _profile.value?.copy(name = name)
    }

    fun updateHeight(value: Double) {
        _profile.value = _profile.value?.copy(heightMeters = value)
    }

    fun updateWidth(value: Double) {
        _profile.value = _profile.value?.copy(widthMeters = value)
    }

    fun updateLength(value: Double) {
        _profile.value = _profile.value?.copy(lengthMeters = value)
    }

    fun updateWeight(value: Double) {
        _profile.value = _profile.value?.copy(weightTons = value)
    }

    fun updateAxles(value: Int) {
        _profile.value = _profile.value?.copy(axleCount = value)
    }

    fun updateHazmat(hazmat: HazmatClass) {
        _profile.value = _profile.value?.copy(hazmatClass = hazmat)
    }

    fun updateTruckType(type: TruckType) {
        _profile.value = _profile.value?.copy(truckType = type)
    }

    fun updateAvoidTolls(enabled: Boolean) {
        _profile.value = _profile.value?.copy(avoidTolls = enabled)
    }

    fun updateAvoidFerries(enabled: Boolean) {
        _profile.value = _profile.value?.copy(avoidFerries = enabled)
    }

    fun updateAvoidLowBridges(enabled: Boolean) {
        _profile.value = _profile.value?.copy(avoidLowBridges = enabled)
    }

    fun saveProfile() {
        _profile.value?.let { manager.saveProfile(it) }
    }
}
