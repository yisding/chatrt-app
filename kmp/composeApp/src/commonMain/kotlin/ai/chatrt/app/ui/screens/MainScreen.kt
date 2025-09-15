package ai.chatrt.app.ui.screens

import ai.chatrt.app.models.*
import ai.chatrt.app.ui.components.*
import ai.chatrt.app.viewmodel.MainViewModel
import ai.chatrt.app.viewmodel.SettingsViewModel
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Main screen UI with ChatRT functionality using Material 3 Expressive theme
 * Integrates MainViewModel with connection controls and status display
 *
 * Requirements: 1.1, 1.6, 2.1, 3.1, 4.1
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Collect state from ViewModels
    val connectionState by mainViewModel.connectionState.collectAsState()
    val videoMode by mainViewModel.videoMode.collectAsState()
    val logs by mainViewModel.logs.collectAsState()
    val webRtcEvents by mainViewModel.webRtcEvents.collectAsState()
    val connectionDiagnostics by mainViewModel.connectionDiagnostics.collectAsState()
    val error by mainViewModel.error.collectAsState()
    val platformOptimization by mainViewModel.platformOptimization.collectAsState()
    val isCallPaused by mainViewModel.isCallPaused.collectAsState()
    val networkQuality by mainViewModel.networkQuality.collectAsState()

    // Local UI state
    var isLogsExpanded by remember { mutableStateOf(false) }
    var isDebugPanelExpanded by remember { mutableStateOf(false) }

    // Motion system animations
    val scrollState = rememberScrollState()

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // App Header with Material 3 Expressive styling
            AppHeader(
                onSettingsClick = onNavigateToSettings,
            )

            // Connection Status Indicator with animations
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
            ) {
                ConnectionStatusIndicator(
                    connectionState = connectionState,
                    networkQuality = networkQuality,
                    isCallPaused = isCallPaused,
                )
            }

            // Video Mode Selector with Material 3 Expressive components
            AnimatedVisibility(
                visible = connectionState != ConnectionState.CONNECTED,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
            ) {
                VideoModeSelector(
                    selectedMode = videoMode,
                    onModeSelected = mainViewModel::setVideoMode,
                    enabled = connectionState == ConnectionState.DISCONNECTED,
                )
            }

            // Video Preview Area with proper aspect ratio management
            AnimatedVisibility(
                visible = videoMode != VideoMode.AUDIO_ONLY,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
            ) {
                VideoPreview(
                    videoMode = videoMode,
                    isPreviewActive = connectionState == ConnectionState.CONNECTED,
                    onCameraSwitch = mainViewModel::switchCamera,
                    connectionState = connectionState,
                )
            }

            // Control Buttons with Material 3 Expressive FABs and buttons
            ControlButtons(
                connectionState = connectionState,
                videoMode = videoMode,
                onStartConnection = mainViewModel::startConnection,
                onStopConnection = mainViewModel::stopConnection,
                onCameraSwitch = mainViewModel::switchCamera,
            )

            // Platform Optimization Suggestions with Material 3 Expressive cards
            AnimatedVisibility(
                visible = platformOptimization != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
            ) {
                platformOptimization?.let { optimization ->
                    OptimizationSuggestion(
                        optimization = optimization,
                        onApplyOptimization = mainViewModel::applyPlatformOptimization,
                        onDismiss = mainViewModel::dismissOptimization,
                    )
                }
            }

            // Error Display with Material 3 Expressive error styling
            AnimatedVisibility(
                visible = error != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
            ) {
                error?.let { currentError ->
                    ErrorDisplay(
                        error = currentError,
                        onRetry = {
                            mainViewModel.clearError()
                            if (connectionState == ConnectionState.FAILED) {
                                mainViewModel.startConnection()
                            }
                        },
                        onDismiss = mainViewModel::clearError,
                    )
                }
            }

            // Real-time Logging Display with expandable/collapsible sections
            LogsDisplay(
                logs = logs,
                isExpanded = isLogsExpanded,
                onToggleExpanded = { isLogsExpanded = !isLogsExpanded },
                debugLoggingEnabled = settingsViewModel.debugLogging.collectAsState().value,
            )

            // Comprehensive Debug Panel with WebRTC events and diagnostics
            AnimatedVisibility(
                visible = settingsViewModel.debugLogging.collectAsState().value,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
            ) {
                DebugPanel(
                    logs = logs,
                    webRtcEvents = webRtcEvents,
                    diagnostics = connectionDiagnostics,
                    isExpanded = isDebugPanelExpanded,
                    onToggleExpanded = { isDebugPanelExpanded = !isDebugPanelExpanded },
                    onExportLogs = {
                        // Launch coroutine to export logs
                        kotlinx.coroutines.MainScope().launch {
                            try {
                                val exportedData = mainViewModel.exportDebugInfo()
                                // In a real app, you'd save this to a file or share it
                                println("Debug info exported: ${exportedData.take(200)}...")
                            } catch (e: Exception) {
                                println("Failed to export debug info: ${e.message}")
                            }
                        }
                    },
                    onClearLogs = {
                        kotlinx.coroutines.MainScope().launch {
                            try {
                                mainViewModel.clearLogs()
                            } catch (e: Exception) {
                                println("Failed to clear logs: ${e.message}")
                            }
                        }
                    },
                )
            }

            // Add bottom padding for better scrolling experience
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * App header with Material 3 Expressive styling
 */
@Composable
private fun AppHeader(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "ChatRT",
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "Real-time AI Voice & Video",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }

            IconButton(
                onClick = onSettingsClick,
                colors =
                    IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                )
            }
        }
    }
}
