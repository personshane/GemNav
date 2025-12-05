package com.gemnav.app.truck.data

import android.content.Context
import android.content.SharedPreferences
import com.gemnav.app.truck.model.HazmatClass
import com.gemnav.app.truck.model.TruckProfile
import com.gemnav.app.truck.model.TruckType

class TruckProfileStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveProfile(profile: TruckProfile) {
        prefs.edit().apply {
            putString(KEY_NAME, profile.name)
            putFloat(KEY_HEIGHT, profile.heightMeters.toFloat())
            putFloat(KEY_WIDTH, profile.widthMeters.toFloat())
            putFloat(KEY_LENGTH, profile.lengthMeters.toFloat())
            putFloat(KEY_WEIGHT, profile.weightTons.toFloat())
            putInt(KEY_AXLES, profile.axleCount)
            putString(KEY_HAZMAT, profile.hazmatClass.name)
            putString(KEY_TRUCK_TYPE, profile.truckType.name)
            putBoolean(KEY_AVOID_TOLLS, profile.avoidTolls)
            putBoolean(KEY_AVOID_FERRIES, profile.avoidFerries)
            putBoolean(KEY_AVOID_LOW_BRIDGES, profile.avoidLowBridges)
            commit()
        }
    }

    fun loadProfile(): TruckProfile {
        val hazmatString = prefs.getString(KEY_HAZMAT, HazmatClass.NONE.name) ?: HazmatClass.NONE.name
        val truckTypeString = prefs.getString(KEY_TRUCK_TYPE, TruckType.STRAIGHT_TRUCK.name) ?: TruckType.STRAIGHT_TRUCK.name
        
        val hazmat = try {
            HazmatClass.valueOf(hazmatString)
        } catch (e: IllegalArgumentException) {
            HazmatClass.NONE
        }
        
        val truckType = try {
            TruckType.valueOf(truckTypeString)
        } catch (e: IllegalArgumentException) {
            TruckType.STRAIGHT_TRUCK
        }
        
        return TruckProfile(
            name = prefs.getString(KEY_NAME, "My Truck") ?: "My Truck",
            heightMeters = prefs.getFloat(KEY_HEIGHT, 4.0f).toDouble(),
            widthMeters = prefs.getFloat(KEY_WIDTH, 2.5f).toDouble(),
            lengthMeters = prefs.getFloat(KEY_LENGTH, 16.5f).toDouble(),
            weightTons = prefs.getFloat(KEY_WEIGHT, 40.0f).toDouble(),
            axleCount = prefs.getInt(KEY_AXLES, 5),
            hazmatClass = hazmat,
            truckType = truckType,
            avoidTolls = prefs.getBoolean(KEY_AVOID_TOLLS, false),
            avoidFerries = prefs.getBoolean(KEY_AVOID_FERRIES, false),
            avoidLowBridges = prefs.getBoolean(KEY_AVOID_LOW_BRIDGES, false)
        )
    }

    fun saveUnitPreference(isMetric: Boolean) {
        prefs.edit().apply {
            putBoolean(KEY_IS_METRIC, isMetric)
            commit()
        }
    }

    fun getUnitPreference(): Boolean {
        return prefs.getBoolean(KEY_IS_METRIC, true)
    }

    companion object {
        private const val PREFS_NAME = "truck_profile_prefs"
        private const val KEY_NAME = "truck_name"
        private const val KEY_HEIGHT = "truck_height"
        private const val KEY_WIDTH = "truck_width"
        private const val KEY_LENGTH = "truck_length"
        private const val KEY_WEIGHT = "truck_weight"
        private const val KEY_AXLES = "truck_axles"
        private const val KEY_HAZMAT = "truck_hazmat"
        private const val KEY_TRUCK_TYPE = "truck_type"
        private const val KEY_AVOID_TOLLS = "avoid_tolls"
        private const val KEY_AVOID_FERRIES = "avoid_ferries"
        private const val KEY_AVOID_LOW_BRIDGES = "avoid_low_bridges"
        private const val KEY_IS_METRIC = "truck_is_metric"
    }
}
