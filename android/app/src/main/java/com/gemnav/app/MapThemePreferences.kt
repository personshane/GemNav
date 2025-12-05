package com.gemnav.app

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages Google Maps theme preferences (dark/light mode).
 * Uses SharedPreferences for persistence across app sessions.
 */
class MapThemePreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "map_theme_prefs"
        private const val KEY_IS_DARK_MODE = "is_dark_mode"
        private const val DEFAULT_IS_DARK_MODE = false
    }
    
    /**
     * Save map theme preference.
     * @param isDarkMode true for dark theme, false for light theme
     */
    fun saveTheme(isDarkMode: Boolean) {
        prefs.edit().putBoolean(KEY_IS_DARK_MODE, isDarkMode).apply()
    }
    
    /**
     * Retrieve current map theme preference.
     * @return true if dark mode is enabled, false otherwise
     */
    fun isDarkMode(): Boolean {
        return prefs.getBoolean(KEY_IS_DARK_MODE, DEFAULT_IS_DARK_MODE)
    }
    
    /**
     * Toggle between dark and light modes.
     * @return new theme state (true = dark, false = light)
     */
    fun toggleTheme(): Boolean {
        val newMode = !isDarkMode()
        saveTheme(newMode)
        return newMode
    }
}
