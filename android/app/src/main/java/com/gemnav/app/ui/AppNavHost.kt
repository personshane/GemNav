package com.gemnav.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gemnav.app.ui.mainflow.HomeScreen
import com.gemnav.app.ui.search.SearchScreen
import com.gemnav.app.ui.settings.SettingsScreen
import com.gemnav.app.ui.voice.VoiceScreen
import com.gemnav.app.ui.route.RouteDetailsScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(navController = navController)
        }
        
        composable("search") {
            SearchScreen(navController = navController)
        }
        
        composable("settings") {
            SettingsScreen(navController = navController)
        }
        
        composable("voice") {
            VoiceScreen(navController = navController)
        }
        
        composable("routeDetails/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            RouteDetailsScreen(
                navController = navController,
                id = id
            )
        }
    }
}
