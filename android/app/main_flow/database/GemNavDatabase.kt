package com.gemnav.android.app.main_flow.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DestinationEntity::class,
        SearchHistoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class GemNavDatabase : RoomDatabase() {
    
    abstract fun destinationDao(): DestinationDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    
    companion object {
        private const val DATABASE_NAME = "gemnav_database"
        
        @Volatile
        private var INSTANCE: GemNavDatabase? = null
        
        fun getInstance(context: Context): GemNavDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        private fun buildDatabase(context: Context): GemNavDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                GemNavDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
