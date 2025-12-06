package com.gemnav.routing.google

import com.gemnav.routing.domain.RouteRequest
import com.gemnav.routing.domain.RouteResult

/**
 * Google-based routing engine for GemNav Free/Basic tiers.
 *
 * CURRENT STATUS:
 * - This is a skeleton implementation.
 * - It does NOT yet call the real Google Directions API or SDK.
 * - It returns a Failure stub by default.
 */
class GoogleRoutingEngine {

    suspend fun calculateRoute(request: RouteRequest): RouteResult {
        return RouteResult.Failure(
            message = "GoogleRoutingEngine.calculateRoute is not yet wired to Google Directions API.",
            engineName = "GOOGLE"
        )
    }
}
