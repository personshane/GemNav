package com.gemnav.app.di

import android.content.Context
import androidx.room.Room
import com.gemnav.android.app.main_flow.database.DestinationDao
import com.gemnav.android.app.main_flow.database.GemNavDatabase
import com.gemnav.android.app.main_flow.database.SearchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Database Module providing Room database and DAOs.
 * 
 * Provides:
 * - GemNavDatabase (Room database instance)
 * - DestinationDao (recent destinations and favorites)
 * - SearchHistoryDao (search history)
 * 
 * Database:
 * - Name: gemnav_database
 * - Location: App-specific internal storage
 * - Migrations: Handled via Room migration strategy
 * 
 * Scope: Singleton
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideGemNavDatabase(
        @ApplicationContext context: Context
    ): GemNavDatabase {
        return Room.databaseBuilder(
            context,
            GemNavDatabase::class.java,
            "gemnav_database"
        )
            .fallbackToDestructiveMigration() // For MVP - in production, provide migrations
            .build()
    }
    
    @Provides
    @Singleton
    fun provideDestinationDao(database: GemNavDatabase): DestinationDao {
        return database.destinationDao()
    }
    
    @Provides
    @Singleton
    fun provideSearchHistoryDao(database: GemNavDatabase): SearchHistoryDao {
        return database.searchHistoryDao()
    }
}
