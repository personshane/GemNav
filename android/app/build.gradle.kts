import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.gemnav.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gemnav.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Secure API key injection from local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }
        
        val hereApiKey = localProperties.getProperty("here_api_key") ?: ""
        val hereMapKey = localProperties.getProperty("here_map_key") ?: ""
        val googleMapsApiKey = localProperties.getProperty("google_maps_api_key") ?: ""
        val geminiApiKey = localProperties.getProperty("gemini_api_key") ?: ""
        
        buildConfigField("String", "HERE_API_KEY", "\"${hereApiKey}\"")
        buildConfigField("String", "HERE_MAP_KEY", "\"${hereMapKey}\"")
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"${googleMapsApiKey}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${geminiApiKey}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }
    
    packagingOptions {
        resources.excludes.add("META-INF/*")
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    
    // Compose
    implementation("androidx.compose.ui:ui:1.5.3")
    implementation("androidx.compose.material:material:1.5.3")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.3")
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.3")
    
    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.5")
    
    // Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended:1.5.3")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48.1")
    kapt("com.google.dagger:hilt-compiler:2.48.1")
    
    // Google Play Services
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // Google Maps Compose
    implementation("com.google.maps.android:maps-compose:2.11.4")
    
    // Google Play Billing
    implementation("com.android.billingclient:billing-ktx:6.1.0")
    
    // HERE SDK placeholder (exact import handled later)
    // TODO: Add HERE SDK when available
    // Step 1: Add repository to settings.gradle.kts:
    //   maven { url = uri("https://repo.heremaps.com/artifactory/HERE_SDK_Android") }
    // Step 2: Add HERE SDK dependency:
    //   implementation("com.here.platform.location:location:4.x.x")
    // Step 3: Add credentials to local.properties:
    //   HERE_ACCESS_KEY_ID=your_key_id
    //   HERE_ACCESS_KEY_SECRET=your_key_secret
    // implementation(files("libs/HERE-sdk-android.aar"))
    
    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

kapt {
    correctErrorTypes = true
}
