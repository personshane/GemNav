# Android Permission & Error Handling UI

**Micro-Project**: MP-013 (Android)  
**Component**: Permission & Error UI Layer  
**Dependencies**: android_navigation_ui.md, android_service_layer.md

---

## 1. PermissionManager (Enhanced)

```kotlin
package com.gemnav.android.ui.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    // Permission state tracking
    private var locationPermissionStatus = PermissionStatus.UNKNOWN
    private var microphonePermissionStatus = PermissionStatus.UNKNOWN
    
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun shouldShowLocationRationale(activity: Activity): Boolean {
        return activity.shouldShowRequestPermissionRationale(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    
    fun shouldShowMicrophoneRationale(activity: Activity): Boolean {
        return activity.shouldShowRequestPermissionRationale(
            Manifest.permission.RECORD_AUDIO
        )
    }
    
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    fun getLocationPermissionStatus(): PermissionStatus {
        return when {
            hasLocationPermission() -> PermissionStatus.GRANTED
            locationPermissionStatus == PermissionStatus.DENIED_PERMANENTLY -> PermissionStatus.DENIED_PERMANENTLY
            else -> PermissionStatus.DENIED
        }
    }
    
    fun setLocationPermissionDeniedPermanently() {
        locationPermissionStatus = PermissionStatus.DENIED_PERMANENTLY
    }
    
    fun setMicrophonePermissionDeniedPermanently() {
        microphonePermissionStatus = PermissionStatus.DENIED_PERMANENTLY
    }
}

enum class PermissionStatus {
    UNKNOWN,
    GRANTED,
    DENIED,
    DENIED_PERMANENTLY
}
```

---

## 2. Permission Request Dialogs

### LocationPermissionDialog.kt
```kotlin
package com.gemnav.android.ui.permission

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.gemnav.android.R

class LocationPermissionDialog(
    context: Context,
    private val onAllow: () -> Unit,
    private val onDeny: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        MaterialAlertDialogBuilder(context)
            .setTitle("Location Permission Required")
            .setMessage(
                "GemNav needs access to your location to:\n\n" +
                "• Provide turn-by-turn navigation\n" +
                "• Show your position on the map\n" +
                "• Calculate accurate routes\n" +
                "• Detect when you've arrived\n\n" +
                "Your location is only used while navigating and is not shared."
            )
            .setPositiveButton("Allow") { _, _ -> onAllow() }
            .setNegativeButton("Not Now") { _, _ -> onDeny() }
            .setCancelable(false)
            .show()
    }
}
```

### MicrophonePermissionDialog.kt
```kotlin
package com.gemnav.android.ui.permission

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.gemnav.android.R

class MicrophonePermissionDialog(
    context: Context,
    private val onAllow: () -> Unit,
    private val onDeny: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        MaterialAlertDialogBuilder(context)
            .setTitle("Microphone Permission Required")
            .setMessage(
                "GemNav needs microphone access to:\n\n" +
                "• Process voice commands\n" +
                "• Enable hands-free navigation\n" +
                "• Improve driving safety\n\n" +
                "Voice data is processed on-device (Free tier) or securely via Gemini (Plus/Pro)."
            )
            .setPositiveButton("Allow") { _, _ -> onAllow() }
            .setNegativeButton("Not Now") { _, _ -> onDeny() }
            .setCancelable(true)
            .show()
    }
}
```

### PermissionDeniedDialog.kt
```kotlin
package com.gemnav.android.ui.permission

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PermissionDeniedDialog(
    context: Context,
    private val permissionType: PermissionType,
    private val onOpenSettings: () -> Unit,
    private val onCancel: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val (title, message) = when (permissionType) {
            PermissionType.LOCATION -> Pair(
                "Location Access Required",
                "GemNav cannot provide navigation without location access.\n\n" +
                "To enable location:\n" +
                "1. Tap 'Open Settings' below\n" +
                "2. Select 'Permissions'\n" +
                "3. Enable 'Location'\n" +
                "4. Return to GemNav"
            )
            PermissionType.MICROPHONE -> Pair(
                "Microphone Access Required",
                "Voice commands require microphone access.\n\n" +
                "To enable microphone:\n" +
                "1. Tap 'Open Settings' below\n" +
                "2. Select 'Permissions'\n" +
                "3. Enable 'Microphone'\n" +
                "4. Return to GemNav"
            )
        }
        
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Open Settings") { _, _ -> onOpenSettings() }
            .setNegativeButton("Cancel") { _, _ -> onCancel() }
            .setCancelable(false)
            .show()
    }
}

enum class PermissionType {
    LOCATION, MICROPHONE
}
```

---

## 3. Error Dialogs

### ErrorDialogFactory.kt
```kotlin
package com.gemnav.android.ui.error

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object ErrorDialogFactory {
    
    fun showNoRouteFoundError(
        context: Context,
        origin: String?,
        destination: String?,
        onRetry: () -> Unit,
        onCancel: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle("No Route Found")
            .setMessage(
                "Unable to find a route between:\n\n" +
                "From: ${origin ?: "Current location"}\n" +
                "To: $destination\n\n" +
                "This may occur if:\n" +
                "• Locations are not connected by roads\n" +
                "• Destination is in a restricted area\n" +
                "• Network connection is unavailable"
            )
            .setPositiveButton("Try Again") { _, _ -> onRetry() }
            .setNegativeButton("Cancel") { _, _ -> onCancel() }
            .show()
    }
    
    fun showGPSSignalLostError(
        context: Context,
        onDismiss: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle("GPS Signal Lost")
            .setMessage(
                "Navigation paused due to weak GPS signal.\n\n" +
                "Tips to improve signal:\n" +
                "• Move away from tall buildings\n" +
                "• Ensure clear view of the sky\n" +
                "• Check that location services are enabled\n\n" +
                "Navigation will resume automatically when signal is restored."
            )
            .setPositiveButton("OK") { _, _ -> onDismiss() }
            .show()
    }
    
    fun showNetworkError(
        context: Context,
        onRetry: () -> Unit,
        onUseOffline: (() -> Unit)? = null
    ) {
        val builder = MaterialAlertDialogBuilder(context)
            .setTitle("Network Connection Error")
            .setMessage(
                "Unable to connect to navigation services.\n\n" +
                "Please check your internet connection and try again."
            )
            .setPositiveButton("Retry") { _, _ -> onRetry() }
            .setNegativeButton("Cancel", null)
        
        if (onUseOffline != null) {
            builder.setNeutralButton("Use Offline") { _, _ -> onUseOffline() }
        }
        
        builder.show()
    }
    
    fun showMapLoadError(
        context: Context,
        onRetry: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Map Load Failed")
            .setMessage(
                "Failed to load map data.\n\n" +
                "This may be due to:\n" +
                "• Network connectivity issues\n" +
                "• Map service unavailability\n" +
                "• Insufficient storage"
            )
            .setPositiveButton("Retry") { _, _ -> onRetry() }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    fun showServiceUnavailableError(
        context: Context,
        serviceName: String,
        onDismiss: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Service Unavailable")
            .setMessage(
                "$serviceName is currently unavailable.\n\n" +
                "Please try again later. If the problem persists, " +
                "check for app updates or contact support."
            )
            .setPositiveButton("OK") { _, _ -> onDismiss() }
            .show()
    }
    
    fun showTierLimitationError(
        context: Context,
        feature: String,
        requiredTier: String,
        onUpgrade: () -> Unit,
        onCancel: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Upgrade Required")
            .setMessage(
                "$feature requires GemNav $requiredTier.\n\n" +
                "Upgrade to unlock:\n" +
                "• $feature\n" +
                "• Advanced AI routing\n" +
                "• Multi-stop navigation\n" +
                "• And more!"
            )
            .setPositiveButton("Upgrade to $requiredTier") { _, _ -> onUpgrade() }
            .setNegativeButton("Not Now") { _, _ -> onCancel() }
            .show()
    }
    
    fun showVoiceRecognitionError(
        context: Context,
        errorMessage: String?,
        onRetry: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Voice Command Failed")
            .setMessage(
                errorMessage ?: "Unable to process voice command.\n\n" +
                "Please try again or use text input."
            )
            .setPositiveButton("Try Again") { _, _ -> onRetry() }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
```

---

## 4. Inline Error Views

### ErrorBannerView.kt
```kotlin
package com.gemnav.android.ui.error

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.gemnav.android.databinding.ViewErrorBannerBinding

class ErrorBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewErrorBannerBinding

    init {
        binding = ViewErrorBannerBinding.inflate(
            LayoutInflater.from(context), this, true
        )
    }

    fun showError(
        type: ErrorType,
        message: String? = null,
        actionText: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        binding.apply {
            errorIcon.setImageResource(type.iconRes)
            errorMessage.text = message ?: type.defaultMessage
            
            if (actionText != null && onAction != null) {
                errorAction.text = actionText
                errorAction.setOnClickListener { onAction() }
                errorAction.isVisible = true
            } else {
                errorAction.isVisible = false
            }
            
            root.setBackgroundColor(context.getColor(type.backgroundColor))
            root.isVisible = true
        }
    }

    fun hide() {
        binding.root.isVisible = false
    }
}

enum class ErrorType(
    val iconRes: Int,
    val defaultMessage: String,
    val backgroundColor: Int
) {
    GPS_WEAK(
        R.drawable.ic_gps_off,
        "Weak GPS signal. Navigation may be inaccurate.",
        R.color.warning_background
    ),
    OFFLINE(
        R.drawable.ic_offline,
        "Offline mode. Limited functionality available.",
        R.color.info_background
    ),
    NETWORK_ERROR(
        R.drawable.ic_network_error,
        "Network error. Some features unavailable.",
        R.color.error_background
    ),
    ROUTE_RECALCULATING(
        R.drawable.ic_route_refresh,
        "Route recalculating...",
        R.color.info_background
    )
}
```

### Layout: view_error_banner.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="12dp"
    android:background="@color/error_background">

    <ImageView
        android:id="@+id/errorIcon"
        android:layout_width="24dp"
        android:layout_height="24dp"
        android:src="@drawable/ic_error"
        app:tint="@android:color/white"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"/>

    <TextView
        android:id="@+id/errorMessage"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="12dp"
        android:layout_marginEnd="8dp"
        android:textColor="@android:color/white"
        android:textSize="14sp"
        app:layout_constraintStart_toEndOf="@id/errorIcon"
        app:layout_constraintEnd_toStartOf="@id/errorAction"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"/>

    <Button
        android:id="@+id/errorAction"
        style="@style/Widget.Material3.Button.TextButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textColor="@android:color/white"
        android:visibility="gone"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"/>

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

## 5. Permission Request Activity

### PermissionRequestActivity.kt
```kotlin
package com.gemnav.android.ui.permission

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.gemnav.android.databinding.ActivityPermissionRequestBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PermissionRequestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionRequestBinding
    
    @Inject lateinit var permissionManager: PermissionManager
    
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onLocationPermissionGranted()
        } else {
            handleLocationPermissionDenied()
        }
    }
    
    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onMicrophonePermissionGranted()
        } else {
            handleMicrophonePermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityPermissionRequestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
    }

    private fun setupUI() {
        binding.apply {
            locationPermissionCard.setOnClickListener {
                requestLocationPermission()
            }
            
            microphonePermissionCard.setOnClickListener {
                requestMicrophonePermission()
            }
            
            continueButton.setOnClickListener {
                if (permissionManager.hasLocationPermission()) {
                    finish()
                } else {
                    showLocationRequiredError()
                }
            }
            
            updatePermissionStatus()
        }
    }

    private fun requestLocationPermission() {
        when {
            permissionManager.hasLocationPermission() -> {
                onLocationPermissionGranted()
            }
            permissionManager.shouldShowLocationRationale(this) -> {
                LocationPermissionDialog(
                    this,
                    onAllow = { 
                        locationPermissionLauncher.launch(
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    },
                    onDeny = { handleLocationPermissionDenied() }
                ).show()
            }
            else -> {
                locationPermissionLauncher.launch(
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }

    private fun requestMicrophonePermission() {
        when {
            permissionManager.hasMicrophonePermission() -> {
                onMicrophonePermissionGranted()
            }
            permissionManager.shouldShowMicrophoneRationale(this) -> {
                MicrophonePermissionDialog(
                    this,
                    onAllow = {
                        microphonePermissionLauncher.launch(
                            android.Manifest.permission.RECORD_AUDIO
                        )
                    },
                    onDeny = { handleMicrophonePermissionDenied() }
                ).show()
            }
            else -> {
                microphonePermissionLauncher.launch(
                    android.Manifest.permission.RECORD_AUDIO
                )
            }
        }
    }

    private fun onLocationPermissionGranted() {
        binding.locationPermissionStatus.apply {
            text = "Granted"
            setTextColor(getColor(R.color.success))
        }
        binding.locationPermissionIcon.setImageResource(R.drawable.ic_check)
        updateContinueButton()
    }

    private fun onMicrophonePermissionGranted() {
        binding.microphonePermissionStatus.apply {
            text = "Granted"
            setTextColor(getColor(R.color.success))
        }
        binding.microphonePermissionIcon.setImageResource(R.drawable.ic_check)
    }

    private fun handleLocationPermissionDenied() {
        if (!permissionManager.shouldShowLocationRationale(this)) {
            permissionManager.setLocationPermissionDeniedPermanently()
            
            PermissionDeniedDialog(
                this,
                PermissionType.LOCATION,
                onOpenSettings = { permissionManager.openAppSettings(this) },
                onCancel = {}
            ).show()
        }
        updatePermissionStatus()
    }

    private fun handleMicrophonePermissionDenied() {
        if (!permissionManager.shouldShowMicrophoneRationale(this)) {
            permissionManager.setMicrophonePermissionDeniedPermanently()
        }
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        binding.apply {
            // Location
            if (permissionManager.hasLocationPermission()) {
                locationPermissionStatus.text = "Granted"
                locationPermissionStatus.setTextColor(getColor(R.color.success))
                locationPermissionIcon.setImageResource(R.drawable.ic_check)
            } else {
                locationPermissionStatus.text = "Required"
                locationPermissionStatus.setTextColor(getColor(R.color.error))
                locationPermissionIcon.setImageResource(R.drawable.ic_error)
            }
            
            // Microphone
            if (permissionManager.hasMicrophonePermission()) {
                microphonePermissionStatus.text = "Granted"
                microphonePermissionStatus.setTextColor(getColor(R.color.success))
                microphonePermissionIcon.setImageResource(R.drawable.ic_check)
            } else {
                microphonePermissionStatus.text = "Optional"
                microphonePermissionStatus.setTextColor(getColor(R.color.warning))
                microphonePermissionIcon.setImageResource(R.drawable.ic_warning)
            }
            
            updateContinueButton()
        }
    }

    private fun updateContinueButton() {
        binding.continueButton.isEnabled = permissionManager.hasLocationPermission()
    }

    private fun showLocationRequiredError() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Location Required")
            .setMessage("Location permission is required to use GemNav.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestLocationPermission()
            }
            .show()
    }
}
```

---

## 6. Offline Mode Indicator

### OfflineModeView.kt
```kotlin
package com.gemnav.android.ui.error

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.gemnav.android.databinding.ViewOfflineModeBinding

class OfflineModeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewOfflineModeBinding

    init {
        binding = ViewOfflineModeBinding.inflate(
            LayoutInflater.from(context), this, true
        )
    }

    fun showOfflineMode(tier: TierType) {
        val message = when (tier) {
            TierType.FREE -> "Limited offline functionality. Location tracking only."
            TierType.PLUS -> "Offline mode. Cached maps available."
            TierType.PRO -> "Offline mode. Truck routing unavailable."
        }
        
        binding.apply {
            offlineMessage.text = message
            root.isVisible = true
        }
    }

    fun hide() {
        binding.root.isVisible = false
    }
}
```

---

## 7. Navigation Error Handling Integration

### NavigationActivity (Enhanced Error Handling)
```kotlin
// Add to NavigationActivity.kt

private fun handleNavigationError(error: NavigationError) {
    when (error) {
        is NavigationError.NoRouteFound -> {
            ErrorDialogFactory.showNoRouteFoundError(
                this,
                error.origin,
                error.destination,
                onRetry = { viewModel.retryNavigation() },
                onCancel = { finish() }
            )
        }
        is NavigationError.GPSSignalLost -> {
            binding.errorBanner.showError(
                ErrorType.GPS_WEAK,
                actionText = "Details",
                onAction = {
                    ErrorDialogFactory.showGPSSignalLostError(this) {}
                }
            )
        }
        is NavigationError.NetworkError -> {
            ErrorDialogFactory.showNetworkError(
                this,
                onRetry = { viewModel.retryNavigation() },
                onUseOffline = if (viewModel.currentTier.value != TierType.FREE) {
                    { viewModel.switchToOfflineMode() }
                } else null
            )
        }
        is NavigationError.MapLoadFailed -> {
            ErrorDialogFactory.showMapLoadError(
                this,
                onRetry = { setupMap() }
            )
        }
        is NavigationError.ServiceUnavailable -> {
            ErrorDialogFactory.showServiceUnavailableError(
                this,
                error.serviceName,
                onDismiss = { finish() }
            )
        }
    }
}

private fun handleOfflineMode(isOffline: Boolean) {
    if (isOffline) {
        binding.offlineModeView.showOfflineMode(viewModel.currentTier.value)
        binding.errorBanner.hide()
    } else {
        binding.offlineModeView.hide()
    }
}
```

---

## 8. Error Data Models

```kotlin
package com.gemnav.android.model

sealed class NavigationError {
    data class NoRouteFound(
        val origin: String?,
        val destination: String
    ) : NavigationError()
    
    object GPSSignalLost : NavigationError()
    
    data class NetworkError(
        val message: String? = null
    ) : NavigationError()
    
    data class MapLoadFailed(
        val reason: String? = null
    ) : NavigationError()
    
    data class ServiceUnavailable(
        val serviceName: String
    ) : NavigationError()
    
    data class VoiceRecognitionFailed(
        val message: String? = null
    ) : NavigationError()
}
```

---

## Integration Notes

1. **Permission Flow**: PermissionRequestActivity → Main App
2. **Error Handling**: Inline banners for recoverable errors, dialogs for critical errors
3. **Offline Detection**: Network state monitoring in service layer
4. **Graceful Degradation**: Feature limitations based on permissions and network state

---

**File Output**: android_permission_error_ui.md
