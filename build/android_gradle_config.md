# Android Gradle Build Configuration

**Version**: 1.0  
**Platform**: Android  
**Build System**: Gradle 8.4+

---

## Project-Level build.gradle.kts

```kotlin
// Project: GemNav
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.20")
        classpath("com.google.gms:google-services:4.4.0")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.9")
    }
}

plugins {
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://repo.here.com/artifactory/open-location-platform") }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
```

---

## App Module build.gradle.kts

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    kotlin("kapt")
}

android {
    namespace = "com.gemnav.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gemnav.app"
        minSdk = 24
        targetSdk = 34
        versionCode = getVersionCode()
        versionName = getVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        vectorDrawables {
            useSupportLibrary = true
        }

        // Gemini Nano configuration
        buildConfigField("String", "GEMINI_MODEL_VERSION", "\"nano-2024\"")
        
        // HERE SDK configuration
        buildConfigField("String", "HERE_SDK_VERSION", "\"4.17.3.0\"")
        manifestPlaceholders["hereAppId"] = getHereAppId()
        manifestPlaceholders["hereAppCode"] = getHereAppCode()
        
        // Google Maps configuration
        resValue("string", "google_maps_key", getGoogleMapsKey())
    }

    signingConfigs {
        create("release") {
            storeFile = file(findProperty("RELEASE_STORE_FILE") ?: "release.keystore")
            storePassword = findProperty("RELEASE_STORE_PASSWORD") as String? ?: ""
            keyAlias = findProperty("RELEASE_KEY_ALIAS") as String? ?: ""
            keyPassword = findProperty("RELEASE_KEY_PASSWORD") as String? ?: ""
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            isDebuggable = true
            isMinifyEnabled = false
            
            buildConfigField("String", "API_BASE_URL", "\"https://dev-api.gemnav.app\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            
            buildConfigField("String", "API_BASE_URL", "\"https://api.gemnav.app\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
        }
        
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-STAGING"
            isDebuggable = true
            
            buildConfigField("String", "API_BASE_URL", "\"https://staging-api.gemnav.app\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
        }
    }

    flavorDimensions += "tier"
    productFlavors {
        create("free") {
            dimension = "tier"
            applicationIdSuffix = ".free"
            versionNameSuffix = "-free"
            
            buildConfigField("Boolean", "TIER_FREE", "true")
            buildConfigField("Boolean", "TIER_PLUS", "false")
            buildConfigField("Boolean", "TIER_PRO", "false")
            buildConfigField("Boolean", "INCLUDE_HERE_SDK", "false")
            
            resValue("string", "app_name", "GemNav Free")
        }
        
        create("plus") {
            dimension = "tier"
            applicationIdSuffix = ".plus"
            versionNameSuffix = "-plus"
            
            buildConfigField("Boolean", "TIER_FREE", "false")
            buildConfigField("Boolean", "TIER_PLUS", "true")
            buildConfigField("Boolean", "TIER_PRO", "false")
            buildConfigField("Boolean", "INCLUDE_HERE_SDK", "false")
            
            resValue("string", "app_name", "GemNav Plus")
        }
        
        create("pro") {
            dimension = "tier"
            applicationIdSuffix = ".pro"
            versionNameSuffix = "-pro"
            
            buildConfigField("Boolean", "TIER_FREE", "false")
            buildConfigField("Boolean", "TIER_PLUS", "false")
            buildConfigField("Boolean", "TIER_PRO", "true")
            buildConfigField("Boolean", "INCLUDE_HERE_SDK", "true")
            
            resValue("string", "app_name", "GemNav Pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.activity:activity-ktx:1.8.1")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    
    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    
    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")
    implementation("androidx.navigation:navigation-compose:2.7.5")
    
    // Google Play Services
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    
    // Google Maps SDK
    implementation("com.google.maps.android:maps-ktx:5.0.0")
    implementation("com.google.maps.android:maps-utils-ktx:5.0.0")
    
    // Gemini AI SDK
    implementation("com.google.ai.client.generativeai:generativeai:0.1.2")
    
    // HERE SDK - Pro tier only
    "proImplementation"("com.here.sdk:sdk-navigation:4.17.3.0")
    "proImplementation"("com.here.sdk:sdk-routing:4.17.3.0")
    "proImplementation"("com.here.sdk:sdk-mapview:4.17.3.0")
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    
    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48.1")
    kapt("com.google.dagger:hilt-compiler:2.48.1")
    
    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("com.google.truth:truth:1.1.5")
    testImplementation("io.mockk:mockk:1.13.8")
    
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Version management functions
fun getVersionCode(): Int {
    val major = 1
    val minor = 0
    val patch = 0
    return major * 10000 + minor * 100 + patch
}

fun getVersionName(): String {
    return "1.0.0"
}

fun getHereAppId(): String {
    return findProperty("HERE_APP_ID") as String? ?: ""
}

fun getHereAppCode(): String {
    return findProperty("HERE_APP_CODE") as String? ?: ""
}

fun getGoogleMapsKey(): String {
    return findProperty("GOOGLE_MAPS_KEY") as String? ?: ""
}
```

---

## gradle.properties

```properties
# Project-wide Gradle settings
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true

# Kotlin
kotlin.code.style=official
kotlin.incremental=true

# AndroidX
android.useAndroidX=true
android.enableJetifier=true

# Build optimization
android.nonTransitiveRClass=true
android.defaults.buildfeatures.buildconfig=true
android.defaults.buildfeatures.aidl=false
android.defaults.buildfeatures.renderscript=false
android.defaults.buildfeatures.resvalues=false
android.defaults.buildfeatures.shaders=false

# Secrets (DO NOT COMMIT - Add to .gitignore)
# Set these in local.properties or CI environment variables
# GOOGLE_MAPS_KEY=
# HERE_APP_ID=
# HERE_APP_CODE=
# RELEASE_STORE_FILE=
# RELEASE_STORE_PASSWORD=
# RELEASE_KEY_ALIAS=
# RELEASE_KEY_PASSWORD=
```

---

## proguard-rules.pro

```proguard
# GemNav ProGuard Rules

# Keep BuildConfig
-keep class com.gemnav.app.BuildConfig { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Data models
-keep class com.gemnav.app.data.models.** { *; }
-keep class com.gemnav.app.domain.models.** { *; }

# HERE SDK
-keep class com.here.sdk.** { *; }
-dontwarn com.here.sdk.**

# Google Maps
-keep class com.google.android.gms.maps.** { *; }
-dontwarn com.google.android.gms.**

# Gemini AI
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
```

---

## settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://repo.here.com/artifactory/open-location-platform") }
    }
}

rootProject.name = "GemNav"
include(":app")
```

---

## Build Variants

### Flavor Matrix
| Variant | Tier | Environment | Package ID | Features |
|---------|------|-------------|------------|----------|
| freeDebug | Free | Dev | com.gemnav.app.free.debug | Gemini Nano + Google Maps intents |
| freeRelease | Free | Prod | com.gemnav.app.free | Gemini Nano + Google Maps intents |
| plusDebug | Plus | Dev | com.gemnav.app.plus.debug | Gemini Cloud + Google Maps SDK |
| plusRelease | Plus | Prod | com.gemnav.app.plus | Gemini Cloud + Google Maps SDK |
| proDebug | Pro | Dev | com.gemnav.app.pro.debug | Gemini Cloud + HERE SDK + Google toggle |
| proRelease | Pro | Prod | com.gemnav.app.pro | Gemini Cloud + HERE SDK + Google toggle |
| freeStaging | Free | Staging | com.gemnav.app.free.staging | Testing build |
| plusStaging | Plus | Staging | com.gemnav.app.plus.staging | Testing build |
| proStaging | Pro | Staging | com.gemnav.app.pro.staging | Testing build |

### Build Commands
```bash
# Clean build
./gradlew clean

# Build specific variant
./gradlew assembleFreeDebug
./gradlew assemblePlusRelease
./gradlew assembleProRelease

# Build all variants
./gradlew assembleDebug
./gradlew assembleRelease

# Run tests
./gradlew testFreeDebugUnitTest
./gradlew connectedPlusDebugAndroidTest

# Generate APKs
./gradlew assembleFreeRelease assemblePlusRelease assembleProRelease

# Generate AAB (App Bundle) for Play Store
./gradlew bundleFreeRelease bundlePlusRelease bundleProRelease
```

---

## Dependency Management Strategy

### Version Catalog (libs.versions.toml)
```toml
[versions]
kotlin = "1.9.20"
compose = "1.5.4"
androidGradle = "8.2.0"
hereSDK = "4.17.3.0"
room = "2.6.1"

[libraries]
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
androidx-core = { module = "androidx.core:core-ktx", version = "1.12.0" }
here-navigation = { module = "com.here.sdk:sdk-navigation", version.ref = "hereSDK" }

[plugins]
android-application = { id = "com.android.application", version.ref = "androidGradle" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

---

## Build Optimization

### Performance Settings
- Parallel builds enabled
- Build caching enabled
- Configuration on demand
- Non-transitive R classes
- 4GB heap size

### ProGuard Optimization
- Code shrinking for release builds
- Resource shrinking enabled
- Obfuscation for security
- Tier-specific rules

### Modularization Strategy (Future)
```
app/
core/
  - core-ui/
  - core-data/
  - core-domain/
feature/
  - feature-navigation/
  - feature-chat/
  - feature-routing/
tier/
  - tier-free/
  - tier-plus/
  - tier-pro/
```