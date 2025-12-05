package com.gemnav.app.truck.ui

import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController

/**
 * Composable wrapper for TruckProfileFragment to integrate it into the Compose navigation system.
 */
@Composable
fun TruckProfileScreen(
    navController: NavController,
    fragmentManager: FragmentManager
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            FragmentContainerView(context).apply {
                id = View.generateViewId()
            }
        },
        update = { view ->
            fragmentManager.beginTransaction()
                .replace(view.id, TruckProfileFragment())
                .commit()
        }
    )
}
