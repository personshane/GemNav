package com.gemnav.data.db

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    @Volatile
    private var INSTANCE: GemNavDatabase? = null

    fun getDatabase(context: Context): GemNavDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                GemNavDatabase::class.java,
                "gemnav.db"
            )
            .fallbackToDestructiveMigration()
            .build()

            INSTANCE = instance
            instance
        }
    }
}
