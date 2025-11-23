package com.gemnav.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * GemNav Application class with Hilt dependency injection.
 * 
 * @HiltAndroidApp triggers Hilt's code generation including a base class for the
 * application that serves as the application-level dependency container.
 * 
 * Usage: Add to AndroidManifest.xml:
 * <application
 *     android:name=".GemNavApplication"
 *     ... />
 */
@HiltAndroidApp
class GemNavApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize app-level components if needed
    }
}
