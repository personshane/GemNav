# Android Navigation UI Implementation

**Micro-Project**: MP-012 (Android)  
**Component**: Navigation UI Layer  
**Dependencies**: android_service_layer.md, android_app_architecture.md

---

## 1. Navigation Screen (NavigationActivity)

### Layout: activity_navigation.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- Map Container -->
    <FrameLayout
        android:id="@+id/mapContainer"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

    <!-- Turn Instruction Card -->
    <com.google.android.material.card.MaterialCardView
        android:id="@+id/turnInstructionCard"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_margin="16dp"
        app:cardElevation="8dp"
        app:cardCornerRadius="12dp"
        android:layout_gravity="top">
        
        <include layout="@layout/turn_instruction_view"/>
    </com.google.android.material.card.MaterialCardView>

    <!-- Bottom Info Panel -->
    <LinearLayout
        android:id="@+id/bottomPanel"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:background="@drawable/bottom_panel_bg"
        android:padding="16dp"
        android:layout_gravity="bottom">
        
        <include layout="@layout/route_info_view"/>
        <include layout="@layout/voice_command_button"/>
    </LinearLayout>

    <!-- Speed/Street Name Overlay -->
    <include layout="@layout/speed_overlay"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|start"
        android:layout_marginStart="16dp"
        android:layout_marginBottom="160dp"/>

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

### NavigationActivity.kt
```kotlin
package com.gemnav.android.ui.navigation

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gemnav.android.databinding.ActivityNavigationBinding
import com.gemnav.android.service.NavigationService
import com.gemnav.android.service.VoiceCommandService
import com.gemnav.android.ui.permission.PermissionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NavigationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNavigationBinding
    private val viewModel: NavigationViewModel by viewModels()
    
    @Inject lateinit var navigationService: NavigationService
    @Inject lateinit var voiceService: VoiceCommandService
    @Inject lateinit var permissionManager: PermissionManager
    
    private var mapFragment: BaseMapFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Keep screen on during navigation
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        binding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupMap()
        setupObservers()
        setupVoiceCommands()
        checkPermissions()
    }

    private fun setupMap() {
        val tier = viewModel.currentTier.value
        
        mapFragment = when (tier) {
            TierType.FREE -> null // Intent-based, no fragment
            TierType.PLUS -> GoogleMapFragment()
            TierType.PRO -> HEREMapFragment()
        }
        
        mapFragment?.let {
            supportFragmentManager.beginTransaction()
                .replace(binding.mapContainer.id, it)
                .commit()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.navigationState.collectLatest { state ->
                when (state) {
                    is NavigationState.Active -> updateNavigationUI(state)
                    is NavigationState.Completed -> showRouteCompleted()
                    is NavigationState.Cancelled -> finish()
                    NavigationState.Idle -> {}
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.turnInstruction.collectLatest { instruction ->
                updateTurnInstruction(instruction)
            }
        }
        
        lifecycleScope.launch {
            viewModel.routeProgress.collectLatest { progress ->
                updateRouteProgress(progress)
            }
        }
    }

    private fun setupVoiceCommands() {
        binding.voiceCommandButton.setOnClickListener {
            if (permissionManager.hasMicrophonePermission()) {
                voiceService.startListening { command ->
                    handleVoiceCommand(command)
                }
            } else {
                permissionManager.requestMicrophonePermission(this)
            }
        }
    }

    private fun checkPermissions() {
        if (!permissionManager.hasLocationPermission()) {
            permissionManager.requestLocationPermission(this)
        }
    }

    private fun updateNavigationUI(state: NavigationState.Active) {
        binding.apply {
            turnInstructionCard.isVisible = true
            bottomPanel.isVisible = true
        }
        
        mapFragment?.updateCamera(state.currentLocation, state.bearing)
        mapFragment?.drawRoute(state.route)
    }

    private fun updateTurnInstruction(instruction: TurnInstruction?) {
        instruction?.let {
            binding.turnInstructionView.apply {
                setTurnType(it.type)
                setDistance(it.distanceMeters)
                setStreetName(it.streetName)
            }
        }
    }

    private fun updateRouteProgress(progress: RouteProgress) {
        binding.routeInfoView.apply {
            setDistance(progress.remainingDistanceMeters)
            setDuration(progress.remainingDurationSeconds)
            setArrivalTime(progress.estimatedArrivalTime)
        }
        
        binding.speedOverlay.apply {
            setCurrentSpeed(progress.currentSpeedKmh)
            setSpeedLimit(progress.speedLimitKmh)
            setCurrentStreet(progress.currentStreet)
        }
    }

    private fun handleVoiceCommand(command: String) {
        lifecycleScope.launch {
            val result = viewModel.processVoiceCommand(command)
            showVoiceCommandFeedback(result)
        }
    }

    private fun showVoiceCommandFeedback(result: VoiceCommandResult) {
        binding.voiceCommandFeedback.apply {
            setText(result.message)
            isVisible = true
            animate().alpha(0f).setDuration(2000).withEndAction {
                isVisible = false
                alpha = 1f
            }
        }
    }

    private fun showRouteCompleted() {
        // Show completion dialog, then finish
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Destination Reached")
            .setMessage("You have arrived at your destination")
            .setPositiveButton("OK") { _, _ -> finish() }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        voiceService.stopListening()
    }
}
```

---

## 2. Turn Instruction View

### Layout: turn_instruction_view.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:padding="16dp"
    android:gravity="center_vertical">

    <!-- Turn Arrow Icon -->
    <ImageView
        android:id="@+id/turnArrowIcon"
        android:layout_width="64dp"
        android:layout_height="64dp"
        android:src="@drawable/ic_turn_right"
        android:tint="@color/primary"
        tools:ignore="ContentDescription"/>

    <!-- Distance and Street Info -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical"
        android:layout_marginStart="16dp">

        <TextView
            android:id="@+id/turnDistance"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="28sp"
            android:textStyle="bold"
            android:textColor="@color/text_primary"
            tools:text="500 m"/>

        <TextView
            android:id="@+id/turnStreetName"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="16sp"
            android:textColor="@color/text_secondary"
            android:layout_marginTop="4dp"
            tools:text="Main Street"/>

    </LinearLayout>

    <!-- Next Turn Preview (Optional) -->
    <ImageView
        android:id="@+id/nextTurnPreview"
        android:layout_width="32dp"
        android:layout_height="32dp"
        android:src="@drawable/ic_turn_left"
        android:tint="@color/text_tertiary"
        android:layout_marginStart="8dp"
        android:visibility="gone"
        tools:ignore="ContentDescription"/>

</LinearLayout>
```

### TurnInstructionView.kt
```kotlin
package com.gemnav.android.ui.navigation

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.gemnav.android.databinding.TurnInstructionViewBinding
import com.gemnav.android.model.TurnType

class TurnInstructionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: TurnInstructionViewBinding

    init {
        binding = TurnInstructionViewBinding.inflate(
            android.view.LayoutInflater.from(context), this, true
        )
    }

    fun setTurnType(type: TurnType) {
        binding.turnArrowIcon.setImageResource(type.iconRes)
    }

    fun setDistance(meters: Int) {
        binding.turnDistance.text = formatDistance(meters)
    }

    fun setStreetName(name: String?) {
        binding.turnStreetName.apply {
            text = name ?: "Continue"
            isVisible = !name.isNullOrBlank()
        }
    }

    fun setNextTurnPreview(type: TurnType?) {
        type?.let {
            binding.nextTurnPreview.apply {
                setImageResource(it.iconRes)
                isVisible = true
            }
        } ?: run {
            binding.nextTurnPreview.isVisible = false
        }
    }

    private fun formatDistance(meters: Int): String {
        return when {
            meters < 100 -> "$meters m"
            meters < 1000 -> "${(meters / 10) * 10} m"
            else -> {
                val km = meters / 1000.0
                "%.1f km".format(km)
            }
        }
    }
}
```

---

## 3. Route Info View

### Layout: route_info_view.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical">

    <!-- Remaining Distance -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:id="@+id/remainingDistanceValue"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="20sp"
            android:textStyle="bold"
            android:textColor="@color/text_primary"
            tools:text="12.5 km"/>

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Distance"
            android:textSize="12sp"
            android:textColor="@color/text_secondary"/>

    </LinearLayout>

    <!-- Remaining Duration -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:id="@+id/remainingDurationValue"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="20sp"
            android:textStyle="bold"
            android:textColor="@color/text_primary"
            tools:text="18 min"/>

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Time"
            android:textSize="12sp"
            android:textColor="@color/text_secondary"/>

    </LinearLayout>

    <!-- ETA -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:id="@+id/etaValue"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="20sp"
            android:textStyle="bold"
            android:textColor="@color/text_primary"
            tools:text="2:45 PM"/>

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Arrival"
            android:textSize="12sp"
            android:textColor="@color/text_secondary"/>

    </LinearLayout>

</LinearLayout>
```

### RouteInfoView.kt
```kotlin
package com.gemnav.android.ui.navigation

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.gemnav.android.databinding.RouteInfoViewBinding
import java.text.SimpleDateFormat
import java.util.*

class RouteInfoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: RouteInfoViewBinding
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    init {
        binding = RouteInfoViewBinding.inflate(
            android.view.LayoutInflater.from(context), this, true
        )
    }

    fun setDistance(meters: Int) {
        binding.remainingDistanceValue.text = when {
            meters < 1000 -> "$meters m"
            else -> "%.1f km".format(meters / 1000.0)
        }
    }

    fun setDuration(seconds: Int) {
        binding.remainingDurationValue.text = when {
            seconds < 60 -> "$seconds sec"
            seconds < 3600 -> "${seconds / 60} min"
            else -> {
                val hours = seconds / 3600
                val mins = (seconds % 3600) / 60
                "${hours}h ${mins}m"
            }
        }
    }

    fun setArrivalTime(timestamp: Long) {
        binding.etaValue.text = timeFormat.format(Date(timestamp))
    }
}
```

---

## 4. Voice Command Button

### Layout: voice_command_button.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="12dp">

    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/voiceCommandButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:src="@drawable/ic_microphone"
        app:tint="@android:color/white"
        app:backgroundTint="@color/primary"/>

    <!-- Voice Command Feedback -->
    <TextView
        android:id="@+id/voiceCommandFeedback"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="top|center_horizontal"
        android:layout_marginBottom="72dp"
        android:background="@drawable/feedback_bubble_bg"
        android:padding="12dp"
        android:textColor="@android:color/white"
        android:textSize="14sp"
        android:gravity="center"
        android:visibility="gone"/>

</FrameLayout>
```

---

## 5. Speed Overlay

### Layout: speed_overlay.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:cardElevation="4dp"
    app:cardCornerRadius="8dp">

    <LinearLayout
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="12dp"
        android:gravity="center">

        <!-- Current Speed -->
        <TextView
            android:id="@+id/currentSpeed"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="24sp"
            android:textStyle="bold"
            android:textColor="@color/text_primary"
            tools:text="65"/>

        <!-- Speed Limit -->
        <TextView
            android:id="@+id/speedLimit"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="12sp"
            android:textColor="@color/text_secondary"
            android:layout_marginTop="2dp"
            tools:text="60 km/h"/>

        <!-- Current Street -->
        <TextView
            android:id="@+id/currentStreet"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="10sp"
            android:textColor="@color/text_tertiary"
            android:layout_marginTop="4dp"
            android:maxLines="1"
            android:ellipsize="end"
            tools:text="Main St"/>

    </LinearLayout>

</com.google.android.material.card.MaterialCardView>
```

### SpeedOverlayView.kt
```kotlin
package com.gemnav.android.ui.navigation

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.card.MaterialCardView
import com.gemnav.android.databinding.SpeedOverlayBinding

class SpeedOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val binding: SpeedOverlayBinding

    init {
        binding = SpeedOverlayBinding.inflate(
            android.view.LayoutInflater.from(context), this, true
        )
    }

    fun setCurrentSpeed(kmh: Float) {
        binding.currentSpeed.text = kmh.toInt().toString()
        
        // Color warning if exceeding speed limit
        binding.currentSpeed.setTextColor(
            if (binding.speedLimit.text.isNotEmpty() && 
                kmh > parseSpeedLimit()) {
                context.getColor(android.R.color.holo_red_dark)
            } else {
                context.getColor(R.color.text_primary)
            }
        )
    }

    fun setSpeedLimit(kmh: Int?) {
        binding.speedLimit.apply {
            text = kmh?.let { "$it km/h" } ?: ""
            isVisible = kmh != null
        }
    }

    fun setCurrentStreet(name: String?) {
        binding.currentStreet.apply {
            text = name ?: ""
            isVisible = !name.isNullOrBlank()
        }
    }

    private fun parseSpeedLimit(): Float {
        return binding.speedLimit.text.toString()
            .replace(" km/h", "")
            .toFloatOrNull() ?: Float.MAX_VALUE
    }
}
```

---

## 6. Map Fragments

### BaseMapFragment.kt
```kotlin
package com.gemnav.android.ui.navigation

import android.location.Location
import androidx.fragment.app.Fragment
import com.gemnav.android.model.Route

abstract class BaseMapFragment : Fragment() {
    abstract fun updateCamera(location: Location, bearing: Float)
    abstract fun drawRoute(route: Route)
    abstract fun clearRoute()
}
```

### GoogleMapFragment.kt (Plus Tier)
```kotlin
package com.gemnav.android.ui.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.gemnav.android.model.Route
import android.location.Location

class GoogleMapFragment : BaseMapFragment(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private var routePolyline: Polyline? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_google_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) 
            as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        map.apply {
            uiSettings.apply {
                isZoomControlsEnabled = false
                isCompassEnabled = true
                isMyLocationButtonEnabled = false
            }
            mapType = GoogleMap.MAP_TYPE_NORMAL
        }
    }

    override fun updateCamera(location: Location, bearing: Float) {
        googleMap?.let { map ->
            val position = CameraPosition.Builder()
                .target(LatLng(location.latitude, location.longitude))
                .zoom(17f)
                .bearing(bearing)
                .tilt(45f)
                .build()
            
            map.animateCamera(CameraUpdateFactory.newCameraPosition(position))
        }
    }

    override fun drawRoute(route: Route) {
        routePolyline?.remove()
        
        googleMap?.let { map ->
            val polylineOptions = PolylineOptions()
                .addAll(route.coordinates.map { LatLng(it.latitude, it.longitude) })
                .width(12f)
                .color(context?.getColor(R.color.route_line) ?: 0xFF4285F4.toInt())
                .geodesic(true)
            
            routePolyline = map.addPolyline(polylineOptions)
        }
    }

    override fun clearRoute() {
        routePolyline?.remove()
        routePolyline = null
    }
}
```

### HEREMapFragment.kt (Pro Tier)
```kotlin
package com.gemnav.android.ui.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.core.GeoPolyline
import com.here.sdk.mapview.MapScheme
import com.here.sdk.mapview.MapView
import com.here.sdk.mapview.MapPolyline
import com.gemnav.android.model.Route
import android.location.Location

class HEREMapFragment : BaseMapFragment() {

    private var mapView: MapView? = null
    private var routePolyline: MapPolyline? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mapView = MapView(requireContext())
        return mapView!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        mapView?.apply {
            onCreate(savedInstanceState)
            mapScene.loadScene(MapScheme.NORMAL_DAY) { error ->
                if (error == null) {
                    // Map ready
                }
            }
        }
    }

    override fun updateCamera(location: Location, bearing: Float) {
        mapView?.let { map ->
            val target = GeoCoordinates(location.latitude, location.longitude)
            map.camera.lookAt(
                target,
                com.here.sdk.mapview.MapMeasure(
                    com.here.sdk.mapview.MapMeasure.Kind.DISTANCE,
                    500.0
                ),
                bearing.toDouble(),
                45.0
            )
        }
    }

    override fun drawRoute(route: Route) {
        routePolyline?.let { mapView?.mapScene?.removeMapPolyline(it) }
        
        mapView?.let { map ->
            val coordinates = route.coordinates.map {
                GeoCoordinates(it.latitude, it.longitude)
            }
            
            val geoPolyline = GeoPolyline(coordinates)
            routePolyline = MapPolyline(
                geoPolyline,
                com.here.sdk.mapview.MapPolyline.SolidRepresentation(
                    com.here.sdk.mapview.MapMeasureDependentRenderSize.withZoomLevelAndUnit(
                        14,
                        com.here.sdk.mapview.RenderSize.Unit.PIXELS,
                        12.0
                    ),
                    android.graphics.Color.parseColor("#4285F4"),
                    null
                )
            )
            
            map.mapScene.addMapPolyline(routePolyline!!)
        }
    }

    override fun clearRoute() {
        routePolyline?.let { mapView?.mapScene?.removeMapPolyline(it) }
        routePolyline = null
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }
}
```

---

## 7. Navigation ViewModel

```kotlin
package com.gemnav.android.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemnav.android.model.*
import com.gemnav.android.service.NavigationService
import com.gemnav.android.service.VoiceCommandService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val navigationService: NavigationService,
    private val voiceService: VoiceCommandService
) : ViewModel() {

    val currentTier = MutableStateFlow(TierType.FREE)
    
    val navigationState: StateFlow<NavigationState> = 
        navigationService.navigationState.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            NavigationState.Idle
        )
    
    val turnInstruction: StateFlow<TurnInstruction?> = 
        navigationService.currentTurnInstruction.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )
    
    val routeProgress: StateFlow<RouteProgress> = 
        navigationService.routeProgress.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            RouteProgress.EMPTY
        )

    suspend fun processVoiceCommand(command: String): VoiceCommandResult {
        return voiceService.processCommand(command)
    }

    fun cancelNavigation() {
        viewModelScope.launch {
            navigationService.stopNavigation()
        }
    }
}
```

---

## 8. Data Models

```kotlin
package com.gemnav.android.model

import android.location.Location

sealed class NavigationState {
    object Idle : NavigationState()
    data class Active(
        val route: Route,
        val currentLocation: Location,
        val bearing: Float
    ) : NavigationState()
    object Completed : NavigationState()
    object Cancelled : NavigationState()
}

data class TurnInstruction(
    val type: TurnType,
    val distanceMeters: Int,
    val streetName: String?
)

enum class TurnType(val iconRes: Int) {
    STRAIGHT(R.drawable.ic_turn_straight),
    RIGHT(R.drawable.ic_turn_right),
    LEFT(R.drawable.ic_turn_left),
    SLIGHT_RIGHT(R.drawable.ic_turn_slight_right),
    SLIGHT_LEFT(R.drawable.ic_turn_slight_left),
    SHARP_RIGHT(R.drawable.ic_turn_sharp_right),
    SHARP_LEFT(R.drawable.ic_turn_sharp_left),
    UTURN(R.drawable.ic_turn_uturn),
    EXIT_RIGHT(R.drawable.ic_exit_right),
    EXIT_LEFT(R.drawable.ic_exit_left),
    MERGE(R.drawable.ic_merge),
    ROUNDABOUT(R.drawable.ic_roundabout)
}

data class RouteProgress(
    val remainingDistanceMeters: Int,
    val remainingDurationSeconds: Int,
    val estimatedArrivalTime: Long,
    val currentSpeedKmh: Float,
    val speedLimitKmh: Int?,
    val currentStreet: String?
) {
    companion object {
        val EMPTY = RouteProgress(0, 0, 0L, 0f, null, null)
    }
}

data class Route(
    val coordinates: List<Coordinate>
)

data class Coordinate(
    val latitude: Double,
    val longitude: Double
)

enum class TierType {
    FREE, PLUS, PRO
}
```

---

## Integration Notes

1. **Free Tier**: No map fragment, uses Google Maps app via intent
2. **Plus Tier**: GoogleMapFragment with Google Maps SDK
3. **Pro Tier**: HEREMapFragment with HERE SDK

All UI components are tier-agnostic except map rendering.

---

**File Output**: android_navigation_ui.md