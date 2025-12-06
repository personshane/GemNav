package com.gemnav.routing.orchestrator

import android.content.Context
import android.util.Log
import com.gemnav.routing.domain.LatLng
import com.gemnav.routing.domain.RouteRequest
import com.gemnav.routing.domain.RouteResult
import com.gemnav.routing.domain.RoutingTier
import com.gemnav.routing.google.GoogleRoutingEngine
import com.gemnav.routing.here.HereRoutingEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * DEBUG/TEST-ONLY: Non-UI validation test for RoutingOrchestrator.
 * 
 * This validates that the orchestrator correctly dispatches routing requests
 * to the appropriate engine based on tier.
 * 
 * Expected behavior:
 * - PRO tier -> HereRoutingEngine (returns RouteResult.Failure with engineName="HERE")
 * - FREE tier -> GoogleRoutingEngine (returns RouteResult.Failure with engineName="GOOGLE")
 * 
 * TODO: Remove this test hook after validation is complete.
 */
object OrchestratorValidationTest {

    private const val TAG = "OrchestratorValidation"

    /**
     * Run validation test.
     * Call this from a debug context (e.g., MainActivity onCreate in debug build).
     */
    fun runValidation(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "=== Starting Orchestrator Validation ===")
                
                // Create dummy route request
                val dummyRequest = RouteRequest(
                    origin = LatLng(latitude = 34.0522, longitude = -118.2437), // Los Angeles
                    destination = LatLng(latitude = 36.1699, longitude = -115.1398), // Las Vegas
                    waypoints = emptyList(),
                    truckConstraints = null
                )
                
                // Initialize engines
                val hereEngine = HereRoutingEngine(context)
                val googleEngine = GoogleRoutingEngine()
                val orchestrator = RoutingOrchestrator(hereEngine, googleEngine)
                
                // Test 1: PRO tier should use HERE engine
                Log.d(TAG, "Test 1: Routing with PRO tier (expect HERE)")
                val proResult = orchestrator.route(dummyRequest, RoutingTier.PRO)
                when (proResult) {
                    is RouteResult.Failure -> {
                        Log.d(TAG, "✓ PRO tier result: Failure from engine '${proResult.engineName}'")
                        Log.d(TAG, "  Message: ${proResult.message}")
                        if (proResult.engineName == "HERE") {
                            Log.d(TAG, "  ✓ PASS: HERE engine was invoked as expected")
                        } else {
                            Log.e(TAG, "  ✗ FAIL: Expected HERE but got ${proResult.engineName}")
                        }
                    }
                    is RouteResult.Success -> {
                        Log.w(TAG, "  Unexpected Success result from PRO tier")
                    }
                }
                
                // Test 2: FREE tier should use GOOGLE engine
                Log.d(TAG, "Test 2: Routing with FREE tier (expect GOOGLE)")
                val freeResult = orchestrator.route(dummyRequest, RoutingTier.FREE)
                when (freeResult) {
                    is RouteResult.Failure -> {
                        Log.d(TAG, "✓ FREE tier result: Failure from engine '${freeResult.engineName}'")
                        Log.d(TAG, "  Message: ${freeResult.message}")
                        if (freeResult.engineName == "GOOGLE") {
                            Log.d(TAG, "  ✓ PASS: GOOGLE engine was invoked as expected")
                        } else {
                            Log.e(TAG, "  ✗ FAIL: Expected GOOGLE but got ${freeResult.engineName}")
                        }
                    }
                    is RouteResult.Success -> {
                        Log.w(TAG, "  Unexpected Success result from FREE tier")
                    }
                }
                
                Log.d(TAG, "=== Orchestrator Validation Complete ===")
                
            } catch (e: Exception) {
                Log.e(TAG, "Validation test failed with exception", e)
            }
        }
    }
}
