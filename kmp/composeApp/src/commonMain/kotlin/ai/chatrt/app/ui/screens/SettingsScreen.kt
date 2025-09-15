package ai.chatrt.app.ui.screens

import ai.chatrt.app.models.*
import ai.chatrt.app.viewmodel.SettingsViewModel
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Settings screen with Material 3 Expressive preference categories and sections
 * Integrates SettingsViewModel with two-way data binding for all settings
 *
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionName")
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Collect state from SettingsViewModel
    val settings by settingsViewModel.settings.collectAsState()
    val isLoading by settingsViewModel.isLoading.collectAsState()
    val error by settingsViewModel.error.collectAsState()
    val saveSuccess by settingsViewModel.saveSuccess.collectAsState()

    // Local UI state
    var serverUrlText by remember { mutableStateOf(settings.serverUrl) }
    var isServerUrlValid by remember { mutableStateOf(true) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Update local server URL when settings change
    LaunchedEffect(settings.serverUrl) {
        serverUrlText = settings.serverUrl
    }

    // Motion system animations
    val scrollState = rememberScrollState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        // Top App Bar with Material 3 Expressive styling
        TopAppBar(
            title = {
                Text(
                    text = "Settings",
                    style =
                        MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
        )

        // Settings Content
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Success/Error Messages
            AnimatedVisibility(
                visible = saveSuccess,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
            ) {
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Settings saved successfully",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = error != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
            ) {
                error?.let { currentError ->
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Error saving settings",
                                    style =
                                        MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                        ),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Text(
                                    text =
                                        when (currentError) {
                                            is ChatRtError.NetworkError -> "Network connection error"
                                            is ChatRtError.ApiError -> currentError.message
                                            else -> "Unknown error occurred"
                                        },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                )
                            }
                            IconButton(onClick = { settingsViewModel.clearError() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss error",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }
            }

            // Video & Audio Preferences Section
            SettingsSection(
                title = "Video & Audio",
                icon = Icons.Default.PlayArrow,
            ) {
                // Default Video Mode Setting
                VideoModePreference(
                    selectedMode = settings.defaultVideoMode,
                    onModeSelected = settingsViewModel::updateDefaultVideoMode,
                    enabled = !isLoading,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Audio Quality Setting
                AudioQualityPreference(
                    selectedQuality = settings.audioQuality,
                    onQualitySelected = settingsViewModel::updateAudioQuality,
                    enabled = !isLoading,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Default Camera Setting
                CameraPreference(
                    selectedCamera = settings.defaultCamera,
                    onCameraSelected = settingsViewModel::updateDefaultCamera,
                    enabled = !isLoading,
                )
            }

            // Server Configuration Section
            SettingsSection(
                title = "Server Configuration",
                icon = Icons.Default.Settings,
            ) {
                // Server URL Configuration
                ServerUrlPreference(
                    serverUrl = serverUrlText,
                    onServerUrlChanged = { newUrl ->
                        serverUrlText = newUrl
                        // Real-time validation
                        settingsViewModel.validateServerUrl(newUrl) { isValid, _ ->
                            isServerUrlValid = isValid
                        }
                    },
                    onServerUrlSaved = { url ->
                        if (isServerUrlValid) {
                            settingsViewModel.updateServerUrl(url)
                        }
                    },
                    isValid = isServerUrlValid,
                    enabled = !isLoading,
                )
            }

            // Debug & Advanced Section
            SettingsSection(
                title = "Debug & Advanced",
                icon = Icons.Default.Build,
            ) {
                // Debug Logging Toggle
                DebugLoggingPreference(
                    enabled = settings.debugLogging,
                    onToggle = settingsViewModel::toggleDebugLogging,
                    isLoading = isLoading,
                )
            }

            // Reset Section
            SettingsSection(
                title = "Reset",
                icon = Icons.Default.Refresh,
            ) {
                // Reset to Defaults Button
                ResetToDefaultsPreference(
                    onReset = { showResetDialog = true },
                    enabled = !isLoading,
                )
            }

            // Add bottom padding for better scrolling experience
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Reset Confirmation Dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = {
                    Text(
                        text = "Reset Settings",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to reset all settings to their default values? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            settingsViewModel.resetToDefaults()
                            showResetDialog = false
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                            ),
                    ) {
                        Text("Reset")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

/**
 * Settings section with Material 3 Expressive styling
 */
@Suppress("FunctionName")
@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Section Content
            content()
        }
    }
}

/**
 * Video Mode preference with Material 3 Expressive radio buttons
 */
@Suppress("FunctionName")
@Composable
private fun VideoModePreference(
    selectedMode: VideoMode,
    onModeSelected: (VideoMode) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.selectableGroup()) {
        Text(
            text = "Default Video Mode",
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        VideoMode.entries.forEach { mode ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedMode == mode,
                            onClick = { if (enabled) onModeSelected(mode) },
                            role = Role.RadioButton,
                        ).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selectedMode == mode,
                    onClick = null,
                    enabled = enabled,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text =
                            when (mode) {
                                VideoMode.AUDIO_ONLY -> "Audio Only"
                                VideoMode.WEBCAM -> "Webcam"
                                VideoMode.SCREEN_SHARE -> "Screen Share"
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            if (enabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            },
                    )
                    Text(
                        text =
                            when (mode) {
                                VideoMode.AUDIO_ONLY -> "Voice conversation only"
                                VideoMode.WEBCAM -> "Share your camera feed"
                                VideoMode.SCREEN_SHARE -> "Share your screen"
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (enabled) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            },
                    )
                }
            }
        }
    }
}

/**
 * Audio Quality preference with Material 3 Expressive radio buttons
 */
@Suppress("FunctionName")
@Composable
private fun AudioQualityPreference(
    selectedQuality: AudioQuality,
    onQualitySelected: (AudioQuality) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.selectableGroup()) {
        Text(
            text = "Audio Quality",
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        AudioQuality.entries.forEach { quality ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedQuality == quality,
                            onClick = { if (enabled) onQualitySelected(quality) },
                            role = Role.RadioButton,
                        ).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selectedQuality == quality,
                    onClick = null,
                    enabled = enabled,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text =
                            when (quality) {
                                AudioQuality.LOW -> "Low"
                                AudioQuality.MEDIUM -> "Medium"
                                AudioQuality.HIGH -> "High"
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            if (enabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            },
                    )
                    Text(
                        text =
                            when (quality) {
                                AudioQuality.LOW -> "Lower bandwidth usage"
                                AudioQuality.MEDIUM -> "Balanced quality and bandwidth"
                                AudioQuality.HIGH -> "Best quality, higher bandwidth"
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (enabled) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            },
                    )
                }
            }
        }
    }
}

/**
 * Camera preference with Material 3 Expressive radio buttons
 */
@Suppress("FunctionName")
@Composable
private fun CameraPreference(
    selectedCamera: CameraFacing,
    onCameraSelected: (CameraFacing) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.selectableGroup()) {
        Text(
            text = "Default Camera",
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        CameraFacing.entries.forEach { camera ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedCamera == camera,
                            onClick = { if (enabled) onCameraSelected(camera) },
                            role = Role.RadioButton,
                        ).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selectedCamera == camera,
                    onClick = null,
                    enabled = enabled,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text =
                            when (camera) {
                                CameraFacing.FRONT -> "Front Camera"
                                CameraFacing.BACK -> "Back Camera"
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            if (enabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            },
                    )
                    Text(
                        text =
                            when (camera) {
                                CameraFacing.FRONT -> "Face the screen"
                                CameraFacing.BACK -> "Face away from screen"
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (enabled) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            },
                    )
                }
            }
        }
    }
}

/**
 * Server URL preference with Material 3 Expressive text fields and validation
 */
@Suppress("FunctionName")
@Composable
private fun ServerUrlPreference(
    serverUrl: String,
    onServerUrlChanged: (String) -> Unit,
    onServerUrlSaved: (String) -> Unit,
    isValid: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Server URL",
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        OutlinedTextField(
            value = serverUrl,
            onValueChange = onServerUrlChanged,
            label = { Text("ChatRT Server URL") },
            placeholder = { Text("https://your-chatrt-server.com") },
            supportingText = {
                if (!isValid && serverUrl.isNotBlank()) {
                    Text(
                        text = "Invalid URL format",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text("Enter the URL of your ChatRT server")
                }
            },
            isError = !isValid && serverUrl.isNotBlank(),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            trailingIcon = {
                if (serverUrl.isNotBlank() && isValid) {
                    IconButton(
                        onClick = { onServerUrlSaved(serverUrl) },
                        enabled = enabled,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save URL",
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Debug logging preference with Material 3 Expressive switches
 */
@Suppress("FunctionName")
@Composable
private fun DebugLoggingPreference(
    enabled: Boolean,
    onToggle: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Debug Logging",
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Show detailed logs for troubleshooting",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }

        Switch(
            checked = enabled,
            onCheckedChange = { if (!isLoading) onToggle() },
            enabled = !isLoading,
        )
    }
}

/**
 * Reset to defaults preference with Material 3 Expressive buttons
 */
@Suppress("FunctionName")
@Composable
private fun ResetToDefaultsPreference(
    onReset: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Reset to Defaults",
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Text(
            text = "Reset all settings to their default values",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 12.dp),
        )

        OutlinedButton(
            onClick = onReset,
            enabled = enabled,
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reset All Settings")
        }
    }
}
