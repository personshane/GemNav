package com.gemnav.app.di

import android.content.Context
import com.gemnav.app.api.DirectionsApiClient
import com.gemnav.app.api.GeminiApiClient
import com.gemnav.app.api.HereApiClient
import com.gemnav.app.api.PlacesApiClient
import com.gemnav.app.core.TierManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier for Google API key.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GoogleApiKey

/**
 * Qualifier for Gemini API key.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GeminiApiKey

/**
 * API Module providing network clients for external services.
 * 
 * Provides:
 * - GeminiApiClient (tier-aware AI processing)
 * - PlacesApiClient (Google Places API)
 * - DirectionsApiClient (Google Directions API)
 * - HereApiClient (HERE SDK for Pro tier truck routing)
 * 
 * API Keys:
 * - Load from BuildConfig or secure storage
 * - Free tier: No API keys needed (on-device only)
 * - Plus tier: Google API key, Gemini API key
 * - Pro tier: Google + Gemini + HERE credentials
 * 
 * Scope: Singleton
 */
@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    
    @Provides
    @GoogleApiKey
    @Singleton
    fun provideGoogleApiKey(): String {
        // TODO: Load from BuildConfig.GOOGLE_API_KEY or secure storage
        return "YOUR_GOOGLE_API_KEY_HERE"
    }
    
    @Provides
    @GeminiApiKey
    @Singleton
    fun provideGeminiApiKey(): String {
        // TODO: Load from BuildConfig.GEMINI_API_KEY or secure storage
        return "YOUR_GEMINI_API_KEY_HERE"
    }
    
    @Provides
    @Singleton
    fun provideGeminiApiClient(
        @ApplicationContext context: Context,
        @GeminiApiKey apiKey: String,
        tierManager: TierManager
    ): GeminiApiClient {
        return GeminiApiClient(context, apiKey, tierManager.getCurrentTier())
    }
    
    @Provides
    @Singleton
    fun providePlacesApiClient(
        @ApplicationContext context: Context,
        @GoogleApiKey apiKey: String
    ): PlacesApiClient {
        return PlacesApiClient(context, apiKey)
    }
    
    @Provides
    @Singleton
    fun provideDirectionsApiClient(
        @GoogleApiKey apiKey: String
    ): DirectionsApiClient {
        return DirectionsApiClient(apiKey)
    }
    
    @Provides
    @Singleton
    fun provideHereApiClient(): HereApiClient {
        return HereApiClient()
    }
}
