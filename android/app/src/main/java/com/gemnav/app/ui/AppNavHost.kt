package com.gemnav.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gemnav.app.ui.mainflow.HomeScreen
import com.gemnav.app.ui.search.SearchScreenPlaceholder
import com.gemnav.app.ui.settings.SettingsScreenPlaceholder
import com.gemnav.app.ui.voice.VoiceScreenPlaceholder
import com.gemnav.app.ui.route.RouteDetailsScreenPlaceholder

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
            SearchScreenPlaceholder()
        }
        
        composable("settings") {
            SettingsScreenPlaceholder()
        }
        
        composable("voice") {
            VoiceScreenPlaceholder()
        }
        
        composable("routeDetails/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            RouteDetailsScreenPlaceholder(id)
        }
    }
}
