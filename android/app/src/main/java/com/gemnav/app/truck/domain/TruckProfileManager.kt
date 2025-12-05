package com.gemnav.app.truck.domain

import android.content.Context
import com.gemnav.app.truck.data.TruckProfileStorage
import com.gemnav.app.truck.model.TruckProfile

/**
 * Simple façade providing higher-level access to the current TruckProfile.
 */
class TruckProfileManager(context: Context) {

    private val storage = TruckProfileStorage(context.applicationContext)

    fun getProfile(): TruckProfile {
        return storage.loadProfile()
    }

    fun saveProfile(profile: TruckProfile) {
        storage.saveProfile(profile)
    }

    fun saveUnitPreference(isMetric: Boolean) {
        storage.saveUnitPreference(isMetric)
    }

    fun getUnitPreference(): Boolean {
        return storage.getUnitPreference()
    }
}
