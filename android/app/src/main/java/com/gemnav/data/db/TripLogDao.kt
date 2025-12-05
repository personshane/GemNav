package com.gemnav.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gemnav.data.db.entities.TripLogEntity

@Dao
interface TripLogDao {

    @Insert
    suspend fun insertTrip(trip: TripLogEntity): Long

    @Query("SELECT * FROM trip_logs ORDER BY startTimestamp DESC LIMIT :limit")
    suspend fun getRecentTrips(limit: Int = 20): List<TripLogEntity>
}
