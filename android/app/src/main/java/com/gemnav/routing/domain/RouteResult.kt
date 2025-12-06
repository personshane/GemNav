package com.gemnav.routing.domain

/**
 * Wrapper for routing results across engines.
 */
sealed class RouteResult {
    data class Success(val route: Route) : RouteResult()
    data class Failure(
        val message: String,
        val throwable: Throwable? = null,
        val engineName: String? = null
    ) : RouteResult()
}
