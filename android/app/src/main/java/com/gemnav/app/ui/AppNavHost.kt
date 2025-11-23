package com.gemnav.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gemnav.app.ui.mainflow.HomeScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToRoute = { /* TODO: Navigate to route preview */ },
                onSettingsClick = { /* TODO: Navigate to settings */ }
            )
        }
    }
}
