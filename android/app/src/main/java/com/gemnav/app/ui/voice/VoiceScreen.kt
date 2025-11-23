package com.gemnav.app.ui.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun VoiceScreen(navController: NavController) {

    var isListening by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = if (isListening) "Listening..." else "Hold to Speak",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Fake mic button (visual only)
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(if (isListening) Color.Red else Color.Gray)
                .alpha(if (isListening) 0.9f else 0.7f)
                .padding(16.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Transcription Preview
        OutlinedTextField(
            value = transcript,
            onValueChange = { transcript = it },
            label = { Text("Transcript") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // TODO: send transcript to Gemini speech-intent engine
                if (transcript.isNotBlank()) {
                    navController.navigate("search")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Process Command")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Listening toggle (temporary for MP-013)
        Button(
            onClick = { isListening = !isListening },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isListening) "Stop Listening" else "Start Listening")
        }
    }
}
