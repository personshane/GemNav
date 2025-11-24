package com.gemnav.app.ui.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gemnav.app.ui.common.SafeModeBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(navController: NavController) {
    val viewModel: VoiceViewModel = viewModel()
    
    val voiceState by viewModel.voiceState.collectAsState()
    val transcribedText by viewModel.transcribedText.collectAsState()
    val featureSummary by viewModel.featureSummary.collectAsState()
    
    val isSafeModeActive = featureSummary.isSafeModeActive
    val isAdvancedVoiceEnabled = featureSummary.advancedVoice
    val isAIEnabled = featureSummary.aiFeatures
    
    // Derived state
    val isListening = voiceState is VoiceViewModel.VoiceState.Listening
    val isProcessing = voiceState is VoiceViewModel.VoiceState.Processing
    val isDisabled = isSafeModeActive
    
    LaunchedEffect(Unit) {
        viewModel.refreshFeatureState()
    }
    
    // Handle result navigation
    LaunchedEffect(voiceState) {
        val state = voiceState
        if (state is VoiceViewModel.VoiceState.Result && state.intent != null) {
            // Navigate to search with the parsed destination
            navController.navigate("search")
        }
    }

    Scaffold(
        topBar = {
            Column {
                SafeModeBanner(isVisible = isSafeModeActive)
                TopAppBar(
                    title = { Text("Voice Command") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Status text
            Text(
                text = when {
                    isDisabled -> "Voice commands unavailable"
                    isListening -> "Listening..."
                    isProcessing -> "Processing..."
                    voiceState is VoiceViewModel.VoiceState.Error -> 
                        (voiceState as VoiceViewModel.VoiceState.Error).message
                    else -> "Tap to speak"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = if (isDisabled) MaterialTheme.colorScheme.onSurfaceVariant 
                        else MaterialTheme.colorScheme.onSurface
            )
            
            // Feature tier hint
            if (!isAdvancedVoiceEnabled && !isSafeModeActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Basic voice commands • Upgrade for AI-powered commands",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Mic button
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isDisabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            isListening -> MaterialTheme.colorScheme.error
                            isProcessing -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .alpha(if (isDisabled) 0.5f else 1f),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = if (isDisabled) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (isListening) MaterialTheme.colorScheme.onError
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Transcription Preview
            OutlinedTextField(
                value = transcribedText,
                onValueChange = { viewModel.onTranscriptionResult(it) },
                label = { Text("What you said") },
                enabled = !isDisabled,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Button(
                onClick = {
                    if (isListening) {
                        viewModel.stopListening()
                    } else {
                        viewModel.startListening()
                    }
                },
                enabled = !isDisabled && !isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isListening) "Stop Listening" else "Start Listening")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cancel button
            OutlinedButton(
                onClick = { 
                    viewModel.cancel()
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
            
            // TODO: Show upgrade prompt for Pro features
            if (!isAdvancedVoiceEnabled && !isSafeModeActive) {
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = { /* TODO: Navigate to upgrade screen */ }) {
                    Text("Upgrade to Plus for AI voice commands")
                }
            }
        }
    }
}
