package com.gemnav.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * ViewModel Module for ViewModel dependencies.
 * 
 * Note: ViewModels using @HiltViewModel annotation with constructor injection
 * don't require explicit bindings. Hilt automatically provides them.
 * 
 * ViewModels:
 * - HomeViewModel: Home screen with destination search
 * - RoutePreviewViewModel: Route preview and confirmation
 * - NavigationViewModel: Active navigation and turn-by-turn
 * - SearchViewModel: Place search along route
 * 
 * Scope: ViewModelScoped (created per ViewModel instance)
 */
@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {
    // All ViewModels use @HiltViewModel with constructor injection
    // No explicit bindings needed
}
