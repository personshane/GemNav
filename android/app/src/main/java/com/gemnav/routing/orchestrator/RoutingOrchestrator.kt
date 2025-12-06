package com.gemnav.routing.orchestrator

import android.util.Log
import com.gemnav.routing.domain.RouteRequest
import com.gemnav.routing.domain.RouteResult
import com.gemnav.routing.domain.RoutingTier
import com.gemnav.routing.here.HereRoutingEngine
import com.gemnav.routing.google.GoogleRoutingEngine

/**
 * Central orchestrator that chooses which routing engine to use based on tier.
 *
 * NOTE:
 * - This does not yet wire into the UI or actual subscription logic.
 * - You will later connect RoutingTier to GemNav's real plan/subscription state.
 */
class RoutingOrchestrator(
    private val hereEngine: HereRoutingEngine,
    private val googleEngine: GoogleRoutingEngine
) {

    /**
     * Route using the appropriate engine based on tier.
     *
     * PRO  -> HERE (primary)
     * BASIC/FREE -> Google
     */
    suspend fun route(
        request: RouteRequest,
        tier: RoutingTier
    ): RouteResult {
        val engineName = when (tier) {
            RoutingTier.PRO -> "HERE"
            RoutingTier.BASIC, RoutingTier.FREE -> "GOOGLE"
        }
        
        Log.d(TAG, "Routing request: tier=$tier → engine=$engineName")
        
        return when (tier) {
            RoutingTier.PRO -> {
                // For now, just call HERE engine. In future, we can add fallback to Google on certain failures.
                hereEngine.calculateRoute(request)
            }
            RoutingTier.BASIC,
            RoutingTier.FREE -> {
                googleEngine.calculateRoute(request)
            }
        }
    }
    
    companion object {
        private const val TAG = "RoutingOrchestrator"
    }
}
