package com.gemnav.app.ui.route

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gemnav.app.models.Destination
import com.gemnav.app.ui.common.SafeModeBanner

@Composable
fun RouteDetailsScreen(
    navController: NavController,
    id: String,
    destinationProvider: (String) -> Destination? = { null }
) {
    // In MP-012, we use mock data only
    val destination = destinationProvider(id) ?: Destination(
        name = "Mock Destination",
        address = "1234 Example St",
        id = id
    )

    var isFavorite by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SafeModeBanner()

        Text(
            text = destination.name,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = destination.address,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Button(onClick = {
                // TODO: integrate routing engine / Maps / Gemini
            }) {
                Text("Navigate")
            }

            Button(onClick = { isFavorite = !isFavorite }) {
                Text(if (isFavorite) "Unfavorite" else "Favorite")
            }

            Button(onClick = {
                // TODO: Share destination info
            }) {
                Text("Share")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Recent Activity",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("• Last visited: 2 days ago")
        Text("• Typical ETA: 14 min")
        Text("• Category: General")
    }
}
