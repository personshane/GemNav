package com.gemnav.routing.google

import android.util.Log
import com.gemnav.app.BuildConfig
import com.gemnav.routing.domain.LatLng
import com.gemnav.routing.domain.RouteRequest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * HTTP client for Google Directions REST API.
 * 
 * Provides a focused abstraction over the Directions API call:
 * - Takes RouteRequest domain model
 * - Returns Result<String> with raw JSON body or failure
 * - Uses BuildConfig.GOOGLE_MAPS_API_KEY
 * - Safe for suspend context (no main thread blocking)
 */
class GoogleDirectionsClient {
    
    private val api: GoogleDirectionsApi
    
    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        api = retrofit.create(GoogleDirectionsApi::class.java)
    }
    
    /**
     * Requests driving directions from Google Directions API.
     * 
     * @param routeRequest Domain model containing origin, destination, and routing preferences
     * @return Result.success with raw JSON string on success, Result.failure on any error
     */
    suspend fun requestDirections(routeRequest: RouteRequest): Result<String> {
        return try {
            val origin = formatLatLng(routeRequest.origin)
            val destination = formatLatLng(routeRequest.destination)
            
            Log.d(TAG, "Requesting directions: origin=$origin, dest=$destination, mode=driving")
            
            val response = api.getDirections(
                origin = origin,
                destination = destination,
                mode = "driving",
                key = BuildConfig.GOOGLE_MAPS_API_KEY
            )
            
            Log.d(TAG, "Response status: ${response.code()}")
            
            if (response.isSuccessful) {
                val body = response.body()?.string()
                if (body != null) {
                    Result.success(body)
                } else {
                    val error = "Google Directions API returned empty response body"
                    Log.e(TAG, error)
                    Result.failure(Throwable(error))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val error = "Google Directions API failed with status ${response.code()}: $errorBody"
                Log.e(TAG, error)
                Result.failure(Throwable(error))
            }
        } catch (e: Exception) {
            val error = "Network request failed: ${e.message}"
            Log.e(TAG, error, e)
            Result.failure(e)
        }
    }
    
    /**
     * Formats LatLng for Google API: "lat,lng"
     */
    private fun formatLatLng(latLng: LatLng): String {
        return "${latLng.latitude},${latLng.longitude}"
    }
    
    companion object {
        private const val TAG = "GoogleDirectionsClient"
    }
}

/**
 * Retrofit interface for Google Directions API.
 * Returns ResponseBody for raw JSON access.
 */
private interface GoogleDirectionsApi {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String,
        @Query("key") key: String
    ): Response<okhttp3.ResponseBody>
}
