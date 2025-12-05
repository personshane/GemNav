package com.gemnav.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gemnav.data.db.entities.TripLogEntity
import com.gemnav.data.db.entities.SearchHistoryEntity

@Database(
    entities = [
        TripLogEntity::class,
        SearchHistoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class GemNavDatabase : RoomDatabase() {
    abstract fun tripLogDao(): TripLogDao
    abstract fun searchHistoryDao(): SearchHistoryDao
}
