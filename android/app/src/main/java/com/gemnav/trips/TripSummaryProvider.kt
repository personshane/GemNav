package com.gemnav.trips

import android.content.Context
import com.gemnav.data.db.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class TripSummaryProvider(context: Context) {

    private val db = DatabaseProvider.getDatabase(context)

    fun recentTrips(limit: Int = 20): Flow<List<TripSummary>> = flow {
        val trips = withContext(Dispatchers.IO) {
            db.tripLogDao().getRecentTrips(limit)
        }

        val summaries = trips.map { entity ->
            TripSummary(
                id = entity.id,
                startTimestamp = entity.startTimestamp,
                endTimestamp = entity.endTimestamp,
                distanceMeters = entity.distanceMeters,
                encodedPath = entity.encodedPath
            )
        }

        emit(summaries)
    }
}
