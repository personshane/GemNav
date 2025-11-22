package com.gemnav.android.app.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.gemnav.android.app.main_flow.models.Destination
import com.gemnav.android.app.main_flow.models.Route
import com.gemnav.android.app.main_flow.models.RouteOptions
import com.gemnav.android.app.main_flow.models.RoutingEngine
import com.gemnav.android.core.TierManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NavigationLauncher handles tier-aware navigation routing.
 * 
 * TIER LOGIC:
 * - Free: Launch Google Maps app via intents
 * - Plus: Launch in-app navigation with Google Maps SDK
 * - Pro: Launch HERE SDK (truck) or Google Maps SDK (car mode)
 * 
 * CRITICAL: Never mix HERE data with Google Maps UI/tiles per terms.
 */
@Singleton
class NavigationLauncher @Inject constructor(
    private val tierManager: TierManager,
    private val context: Context
) {
    
    /**
     * Launch navigation based on tier and route.
     * 
     * @param route Calculated route
     * @param options Route options (engine, truck specs, etc.)
     * @return Launch result (success/failure)
     */
    suspend fun launchNavigation(
        route: Route,
        options: RouteOptions = RouteOptions()
    ): NavigationResult {
        return when (tierManager.currentTier) {
            TierManager.Tier.FREE -> launchFreeNavigation(route)
            TierManager.Tier.PLUS -> launchPlusNavigation(route)
            TierManager.Tier.PRO -> launchProNavigation(route, options)
        }
    }
    
    /**
     * Free tier: Launch Google Maps app with intent.
     */
    private fun launchFreeNavigation(route: Route): NavigationResult {
        return try {
            val intent = createGoogleMapsIntent(
                destination = route.destination,
                waypoints = route.waypoints,
                travelMode = "driving"
            )
            
            context.startActivity(intent)
            
            NavigationResult.Success(
                engine = RoutingEngine.GOOGLE_MAPS,
                message = "Launched Google Maps app"
            )
        } catch (e: Exception) {
            NavigationResult.Error(
                message = "Failed to launch Google Maps: ${e.message}",
                exception = e
            )
        }
    }
    
    /**
     * Plus tier: Launch in-app navigation with Google Maps SDK.
     */
    private suspend fun launchPlusNavigation(route: Route): NavigationResult {
        return try {
            // For Plus tier, we launch the in-app navigation activity
            // This will be handled by NavigationActivity with Maps SDK
            val intent = Intent(context, Class.forName("com.gemnav.android.app.navigation.NavigationActivity"))
            intent.putExtra("ROUTE_ID", route.id)
            intent.putExtra("ENGINE", RoutingEngine.GOOGLE_MAPS.name)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            context.startActivity(intent)
            
            NavigationResult.Success(
                engine = RoutingEngine.GOOGLE_MAPS,
                message = "Launched in-app navigation (Google Maps SDK)"
            )
        } catch (e: Exception) {
            NavigationResult.Error(
                message = "Failed to launch in-app navigation: ${e.message}",
                exception = e
            )
        }
    }
    
    /**
     * Pro tier: Launch HERE SDK or Google Maps SDK based on toggle.
     */
    private suspend fun launchProNavigation(
        route: Route,
        options: RouteOptions
    ): NavigationResult {
        return try {
            val engine = if (options.routingEngine == RoutingEngine.HERE && options.truckSpecs != null) {
                // Truck routing via HERE SDK
                RoutingEngine.HERE
            } else {
                // Car routing via Google Maps SDK
                RoutingEngine.GOOGLE_MAPS
            }
            
            val intent = Intent(context, Class.forName("com.gemnav.android.app.navigation.NavigationActivity"))
            intent.putExtra("ROUTE_ID", route.id)
            intent.putExtra("ENGINE", engine.name)
            intent.putExtra("TRUCK_SPECS", options.truckSpecs) // Serializable
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            context.startActivity(intent)
            
            NavigationResult.Success(
                engine = engine,
                message = "Launched in-app navigation (${engine.name})"
            )
        } catch (e: Exception) {
            NavigationResult.Error(
                message = "Failed to launch Pro navigation: ${e.message}",
                exception = e
            )
        }
    }
    
    /**
     * Create Google Maps intent with destination and waypoints.
     * 
     * Intent format:
     * google.navigation:q={lat},{lng}&waypoints={lat1},{lng1}|{lat2},{lng2}&mode=d
     */
    private fun createGoogleMapsIntent(
        destination: Destination,
        waypoints: List<Destination> = emptyList(),
        travelMode: String = "driving"
    ): Intent {
        val destCoords = "${destination.latitude},${destination.longitude}"
        
        val waypointsStr = if (waypoints.isNotEmpty()) {
            waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }
        } else {
            null
        }
        
        // Build URI
        val uriBuilder = StringBuilder("google.navigation:q=$destCoords")
        if (waypointsStr != null) {
            uriBuilder.append("&waypoints=$waypointsStr")
        }
        
        // Add travel mode: d=driving, w=walking, b=bicycling
        val mode = when (travelMode) {
            "walking" -> "w"
            "bicycling" -> "b"
            "transit" -> "r"
            else -> "d"
        }
        uriBuilder.append("&mode=$mode")
        
        val uri = Uri.parse(uriBuilder.toString())
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        return intent
    }
    
    /**
     * Check if Google Maps app is installed.
     */
    fun isGoogleMapsInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.google.android.apps.maps", 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Launch Google Play to install Google Maps.
     */
    fun promptInstallGoogleMaps() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Failed to open Play Store
        }
    }
}

/**
 * Navigation launch result.
 */
sealed class NavigationResult {
    data class Success(
        val engine: RoutingEngine,
        val message: String
    ) : NavigationResult()
    
    data class Error(
        val message: String,
        val exception: Exception? = null
    ) : NavigationResult()
}
