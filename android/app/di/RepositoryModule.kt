package com.gemnav.app.di

import com.gemnav.android.app.main_flow.DestinationRepository
import com.gemnav.android.app.main_flow.RouteRepository
import com.gemnav.android.app.main_flow.SearchRepository
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Repository Module providing repository bindings.
 * 
 * Note: All repositories use constructor injection with @Inject,
 * so Hilt can automatically provide them without explicit @Provides methods.
 * This module serves as documentation of available repositories.
 * 
 * Repositories:
 * - DestinationRepository: Recent destinations and favorites
 * - SearchRepository: Place search and search history
 * - RouteRepository: Route calculation (tier-aware)
 * 
 * Scope: Singleton (via @Singleton on repository classes)
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // All repositories use constructor injection
    // No explicit bindings needed
}
