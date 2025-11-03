package com.voicerecorder.ui.screen

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.voicerecorder.data.local.entity.RecordingState
import com.voicerecorder.ui.viewmodel.RecordingViewModel
import com.voicerecorder.utils.AudioUtils
import com.voicerecorder.utils.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RecordingScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val recordingState by viewModel.recordingState.collectAsState()
    val recordingDuration by viewModel.recordingDuration.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val permissionsState = rememberMultiplePermissionsState(
        permissions = PermissionUtils.getRequiredPermissions().toList()
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recording") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!permissionsState.allPermissionsGranted) {
                PermissionRequiredContent(
                    onRequestPermissions = {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                )
            } else {
                RecordingContent(
                    recordingState = recordingState,
                    duration = recordingDuration,
                    onStartRecording = { viewModel.startRecording() },
                    onStopRecording = { viewModel.stopRecording() }
                )
            }

            // Show error message
            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRequiredContent(
    onRequestPermissions: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Microphone Permission Required",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This app needs microphone access to record audio",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermissions) {
            Text("Grant Permission")
        }
    }
}

@Composable
fun RecordingContent(
    recordingState: RecordingState,
    duration: Long,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    val isRecording = recordingState == RecordingState.RECORDING

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Status indicator
        StatusIndicator(recordingState)

        Spacer(modifier = Modifier.height(32.dp))

        // Timer
        Text(
            text = AudioUtils.formatDuration(duration),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = if (isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Record/Stop button
        FloatingActionButton(
            onClick = {
                if (isRecording || recordingState != RecordingState.IDLE) {
                    onStopRecording()
                } else {
                    onStartRecording()
                }
            },
            modifier = Modifier.size(80.dp),
            containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = if (isRecording || recordingState != RecordingState.IDLE) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isRecording || recordingState != RecordingState.IDLE) "Stop" else "Record",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun StatusIndicator(recordingState: RecordingState) {
    val statusText = when (recordingState) {
        RecordingState.IDLE -> "Ready to record"
        RecordingState.RECORDING -> "Recording..."
        RecordingState.PAUSED_PHONE_CALL -> "Paused - Phone call"
        RecordingState.PAUSED_AUDIO_FOCUS_LOSS -> "Paused - Audio focus lost"
        RecordingState.PAUSED_NO_AUDIO -> "Paused - No audio detected"
        RecordingState.STOPPED -> "Stopped"
        RecordingState.ERROR -> "Error"
    }

    val statusColor = when (recordingState) {
        RecordingState.RECORDING -> MaterialTheme.colorScheme.primary
        RecordingState.PAUSED_PHONE_CALL,
        RecordingState.PAUSED_AUDIO_FOCUS_LOSS,
        RecordingState.PAUSED_NO_AUDIO -> MaterialTheme.colorScheme.secondary
        RecordingState.ERROR -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        color = statusColor.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = statusText,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            color = statusColor,
            fontWeight = FontWeight.Medium
        )
    }
}
