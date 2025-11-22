# GemNav - ProGuard baseline
# Nothing aggressive yet. Keep everything related to Hilt, Retrofit, Gson, and Compose.

# Hilt
-keep class dagger.hilt.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Retrofit & Gson
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-dontwarn okio.**

# HERE SDK placeholder
-keep class com.here.** { *; }

# Compose keeps
-keep class androidx.compose.** { *; }
