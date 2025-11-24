package com.gemnav.app.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gemnav.app.models.Destination
import com.gemnav.app.ui.common.SafeModeBanner
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    var query by remember { mutableStateOf("") }
    val mockResults = remember(query) {
        if (query.isBlank()) emptyList()
        else List(5) { index ->
            Destination(
                name = "Mock Result $index",
                address = "$query Street $index",
                id = UUID.randomUUID().toString()
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SafeModeBanner()
        
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(mockResults) { result ->
                SearchResultItem(
                    result = result,
                    onClick = {
                        navController.navigate("routeDetails/${result.id}")
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultItem(result: Destination, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(result.name, style = MaterialTheme.typography.titleMedium)
            Text(result.address, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
