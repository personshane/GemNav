package com.gemnav.app.di

import android.content.Context
import com.gemnav.app.core.DefaultTierManager
import com.gemnav.app.core.SubscriptionTier
import com.gemnav.app.core.TierManager
import com.gemnav.app.voice.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier for application-level coroutine scope.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * Service Module providing app-level services.
 * 
 * Provides:
 * - TierManager (subscription tier management)
 * - VoiceCommandManager (voice command coordinator)
 * - SpeechRecognitionService (speech-to-text)
 * - VoiceResponseService (text-to-speech)
 * - CommandParser (NLU via Gemini)
 * - CommandExecutor (command routing)
 * - WakeWordDetector (optional, Plus/Pro only)
 * - Application CoroutineScope
 * 
 * Scope: Singleton
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    
    @Provides
    @Singleton
    fun provideTierManager(): TierManager {
        return DefaultTierManager()
    }
    
    @ApplicationScope
    @Provides
    @Singleton
    fun provideApplicationScope(
        @MainDispatcher mainDispatcher: kotlinx.coroutines.CoroutineDispatcher
    ): CoroutineScope {
        return CoroutineScope(SupervisorJob() + mainDispatcher)
    }
    
    @Provides
    @Singleton
    fun provideSpeechRecognitionService(
        @ApplicationContext context: Context,
        tierManager: TierManager
    ): SpeechRecognitionService {
        return AndroidSpeechRecognitionService(
            context, 
            tierManager.getCurrentTier()
        )
    }
    
    @Provides
    @Singleton
    fun provideVoiceResponseService(
        @ApplicationContext context: Context
    ): VoiceResponseService {
        return AndroidVoiceResponseService(context)
    }
    
    @Provides
    @Singleton
    fun provideWakeWordDetector(
        @ApplicationContext context: Context,
        tierManager: TierManager
    ): WakeWordDetector? {
        // Only provide for Plus/Pro tiers
        return if (tierManager.getCurrentTier().isPlus) {
            WakeWordDetector(context)
        } else {
            null
        }
    }
}
