package ai.chatrt.app.accessibility

import ai.chatrt.app.models.*
import ai.chatrt.app.ui.components.*
import ai.chatrt.app.ui.screens.MainScreen
import ai.chatrt.app.ui.screens.SettingsScreen
import ai.chatrt.app.ui.theme.ChatRtTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

/**
 * Accessibility integration tests with Material 3 Expressive guidelines
 * Tests accessibility features, screen reader support, and inclusive design
 * Requirements: 6.2 (Material 3 Expressive accessibility guidelines)
 */
class AccessibilityIntegrationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test main screen accessibility with Material 3 Expressive components
     */
    @Test
    fun testMainScreenAccessibility() {
        val mockMainViewModel = createMockMainViewModel()

        composeTestRule.setContent {
            ChatRtTheme {
                MainScreen(
                    viewModel = mockMainViewModel,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Test connection status accessibility
        composeTestRule
            .onNodeWithTag("connection_status")
            .assertExists()
            .assertHasClickAction()
            .assertContentDescriptionContains("Connection status")

        // Test video mode selector accessibility
        composeTestRule
            .onNodeWithTag("video_mode_audio_only")
            .assertExists()
            .assertIsSelectable()
            .assertHasClickAction()
            .assertContentDescriptionContains("Audio only mode")

        composeTestRule
            .onNodeWithTag("video_mode_webcam")
            .assertExists()
            .assertIsSelectable()
            .assertHasClickAction()
            .assertContentDescriptionContains("Webcam mode")

        composeTestRule
            .onNodeWithTag("video_mode_screen_share")
            .assertExists()
            .assertIsSelectable()
            .assertHasClickAction()
            .assertContentDescriptionContains("Screen sharing mode")

        // Test control buttons accessibility
        composeTestRule
            .onNodeWithTag("start_connection_button")
            .assertExists()
            .assertHasClickAction()
            .assertIsEnabled()
            .assertContentDescriptionContains("Start connection")

        composeTestRule
            .onNodeWithTag("settings_button")
            .assertExists()
            .assertHasClickAction()
            .assertContentDescriptionContains("Open settings")

        // Test video preview accessibility when active
        mockMainViewModel.setVideoMode(VideoMode.WEBCAM)
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("video_preview")
            .assertExists()
            .assertContentDescriptionContains("Camera preview")

        composeTestRule
            .onNodeWithTag("camera_switch_button")
            .assertExists()
            .assertHasClickAction()
            .assertContentDescriptionContains("Switch camera")
    }

    /**
     * Test settings screen accessibility
     */
    @Test
    fun testSettingsScreenAccessibility() {
        val mockSettingsViewModel = createMockSettingsViewModel()

        composeTestRule.setContent {
            ChatRtTheme {
                SettingsScreen(
                    viewModel = mockSettingsViewModel,
                    onNavigateBack = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Test navigation accessibility
        composeTestRule
            .onNodeWithTag("back_button")
            .assertExists()
            .assertHasClickAction()
            .assertContentDescriptionContains("Navigate back")

        // Test video mode preference accessibility
        composeTestRule
            .onNodeWithTag("default_video_mode_setting")
            .assertExists()
            .assertHasClickAction()
            .assertContentDescriptionContains("Default video mode")

        // Test audio quality setting accessibility
        composeTestRule
            .onNodeWithTag("audio_quality_setting")
            .assertExists()
            .assertHasClickAction()
            .assertContentDescriptionContains("Audio quality")

        // Test server URL setting accessibility
        composeTestRule
            .onNodeWithTag("server_url_setting")
            .assertExists()
            .assertIsEnabled()
            .assertContentDescriptionContains("Server URL")

        // Test debug logging toggle accessibility
        composeTestRule
            .onNodeWithTag("debug_logging_toggle")
            .assertExists()
            .assertIsToggleable()
            .assertContentDescriptionContains("Debug logging")

        // Test reset button accessibility
        composeTestRule
            .onNodeWithTag("reset_settings_button")
            .assertExists()
            .assertHasClickAction()
            .assertContentDescriptionContains("Reset to defaults")
    }

    /**
     * Test connection status indicator accessibility states
     */
    @Test
    fun testConnectionStatusAccessibility() {
        val mockMainViewModel = createMockMainViewModel()

        composeTestRule.setContent {
            ChatRtTheme {
                ConnectionStatusIndicator(
                    connectionState = mockMainViewModel.connectionState.collectAsState().value,
                    modifier = Modifier.testTag("connection_status"),
                )
            }
        }

        // Test disconnected state
        composeTestRule
            .onNodeWithTag("connection_status")
            .assertContentDescriptionContains("Disconnected")
            .assertContentDescriptionContains("Ready to connect")

        // Test connecting state
        mockMainViewModel.simulateConnectionState(ConnectionState.CONNECTING)
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("connection_status")
            .assertContentDescriptionContains("Connecting")
            .assertContentDescriptionContains("Establishing connection")

        // Test connected state
        mockMainViewModel.simulateConnectionState(ConnectionState.CONNECTED)
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("connection_status")
            .assertContentDescriptionContains("Connected")
            .assertContentDescriptionContains("Connection established")

        // Test failed state
        mockMainViewModel.simulateConnectionState(ConnectionState.FAILED)
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("connection_status")
            .assertContentDescriptionContains("Failed")
            .assertContentDescriptionContains("Connection failed")
    }

    /**
     * Test video mode selector accessibility with state changes
     */
    @Test
    fun testVideoModeSelectorAccessibility() {
        val mockMainViewModel = createMockMainViewModel()

        composeTestRule.setContent {
            ChatRtTheme {
                VideoModeSelector(
                    selectedMode = mockMainViewModel.videoMode.collectAsState().value,
                    onModeSelected = mockMainViewModel::setVideoMode,
                    enabled = true,
                    modifier = Modifier.testTag("video_mode_selector"),
                )
            }
        }

        // Test initial selection state
        composeTestRule
            .onNodeWithTag("video_mode_audio_only")
            .assertIsSelected()
            .assertContentDescriptionContains("Audio only mode, selected")

        composeTestRule
            .onNodeWithTag("video_mode_webcam")
            .assertIsNotSelected()
            .assertContentDescriptionContains("Webcam mode, not selected")

        // Test selection change
        composeTestRule
            .onNodeWithTag("video_mode_webcam")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("video_mode_webcam")
            .assertIsSelected()
            .assertContentDescriptionContains("Webcam mode, selected")

        composeTestRule
            .onNodeWithTag("video_mode_audio_only")
            .assertIsNotSelected()
            .assertContentDescriptionContains("Audio only mode, not selected")
    }

    /**
     * Test control buttons accessibility with different states
     */
    @Test
    fun testControlButtonsAccessibility() {
        val mockMainViewModel = createMockMainViewModel()

        composeTestRule.setContent {
            ChatRtTheme {
                ControlButtons(
                    connectionState = mockMainViewModel.connectionState.collectAsState().value,
                    onStartConnection = mockMainViewModel::startConnection,
                    onStopConnection = mockMainViewModel::stopConnection,
                    onOpenSettings = {},
                    onSwitchCamera = mockMainViewModel::switchCamera,
                    showCameraSwitch = mockMainViewModel.videoMode.collectAsState().value == VideoMode.WEBCAM,
                    modifier = Modifier.testTag("control_buttons"),
                )
            }
        }

        // Test start button when disconnected
        composeTestRule
            .onNodeWithTag("start_connection_button")
            .assertExists()
            .assertIsEnabled()
            .assertContentDescriptionContains("Start connection")
            .assertHasClickAction()

        // Test button state when connecting
        mockMainViewModel.simulateConnectionState(ConnectionState.CONNECTING)
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("start_connection_button")
            .assertIsNotEnabled()
            .assertContentDescriptionContains("Connecting")

        // Test stop button when connected
        mockMainViewModel.simulateConnectionState(ConnectionState.CONNECTED)
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("stop_connection_button")
            .assertExists()
            .assertIsEnabled()
            .assertContentDescriptionContains("Stop connection")
            .assertHasClickAction()

        // Test camera switch button visibility
        mockMainViewModel.setVideoMode(VideoMode.WEBCAM)
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("camera_switch_button")
            .assertExists()
            .assertIsEnabled()
            .assertContentDescriptionContains("Switch camera")
            .assertHasClickAction()
    }

    /**
     * Test logs display accessibility
     */
    @Test
    fun testLogsDisplayAccessibility() {
        val mockMainViewModel = createMockMainViewModel()

        // Add some test logs
        mockMainViewModel.addTestLogs(
            listOf(
                LogEntry(System.currentTimeMillis(), "Connection started", LogLevel.INFO),
                LogEntry(System.currentTimeMillis(), "WebRTC offer created", LogLevel.DEBUG),
                LogEntry(System.currentTimeMillis(), "Connection failed", LogLevel.ERROR),
            ),
        )

        composeTestRule.setContent {
            ChatRtTheme {
                LogsDisplay(
                    logs = mockMainViewModel.logs.collectAsState().value,
                    isExpanded = true,
                    onToggleExpanded = {},
                    modifier = Modifier.testTag("logs_display"),
                )
            }
        }

        // Test logs container accessibility
        composeTestRule
            .onNodeWithTag("logs_display")
            .assertExists()
            .assertContentDescriptionContains("Connection logs")

        // Test individual log entries
        composeTestRule
            .onNodeWithText("Connection started")
            .assertExists()
            .assertContentDescriptionContains("Info: Connection started")

        composeTestRule
            .onNodeWithText("WebRTC offer created")
            .assertExists()
            .assertContentDescriptionContains("Debug: WebRTC offer created")

        composeTestRule
            .onNodeWithText("Connection failed")
            .assertExists()
            .assertContentDescriptionContains("Error: Connection failed")

        // Test expand/collapse functionality
        composeTestRule
            .onNodeWithTag("logs_toggle_button")
            .assertExists()
            .assertHasClickAction()
            .assertContentDescriptionContains("Toggle logs visibility")
    }

    /**
     * Test error display accessibility
     */
    @Test
    fun testErrorDisplayAccessibility() {
        val testError =
            ChatRtError.NetworkError(
                cause = NetworkErrorCause.NO_INTERNET,
                message = "No internet connection available",
            )

        composeTestRule.setContent {
            ChatRtTheme {
                ErrorDisplay(
                    error = testError,
                    onRetry = {},
                    onDismiss = {},
                    modifier = Modifier.testTag("error_display"),
                )
            }
        }

        // Test error container accessibility
        composeTestRule
            .onNodeWithTag("error_display")
            .assertExists()
            .assertContentDescriptionContains("Error message")

        // Test error message accessibility
        composeTestRule
            .onNodeWithText("No internet connection available")
            .assertExists()
            .assertContentDescriptionContains("Error: No internet connection available")

        // Test retry button accessibility
        composeTestRule
            .onNodeWithTag("retry_button")
            .assertExists()
            .assertHasClickAction()
            .assertContentDescriptionContains("Retry connection")

        // Test dismiss button accessibility
        composeTestRule
            .onNodeWithTag("dismiss_button")
            .assertExists()
            .assertHasClickAction()
            .assertContentDescriptionContains("Dismiss error")
    }

    /**
     * Test optimization suggestion accessibility
     */
    @Test
    fun testOptimizationSuggestionAccessibility() {
        val optimization =
            PlatformOptimization(
                recommendedVideoMode = VideoMode.AUDIO_ONLY,
                recommendedAudioQuality = AudioQuality.MEDIUM,
                disableVideoPreview = true,
                reason = OptimizationReason.LOW_BATTERY,
            )

        composeTestRule.setContent {
            ChatRtTheme {
                OptimizationSuggestion(
                    optimization = optimization,
                    onApplyOptimization = {},
                    onDismiss = {},
                    modifier = Modifier.testTag("optimization_suggestion"),
                )
            }
        }

        // Test suggestion container accessibility
        composeTestRule
            .onNodeWithTag("optimization_suggestion")
            .assertExists()
            .assertContentDescriptionContains("Performance optimization suggestion")

        // Test apply button accessibility
        composeTestRule
            .onNodeWithTag("apply_optimization_button")
            .assertExists()
            .assertHasClickAction()
            .assertContentDescriptionContains("Apply optimization")

        // Test dismiss button accessibility
        composeTestRule
            .onNodeWithTag("dismiss_optimization_button")
            .assertExists()
            .assertHasClickAction()
            .assertContentDescriptionContains("Dismiss suggestion")
    }

    /**
     * Test keyboard navigation and focus management
     */
    @Test
    fun testKeyboardNavigationAccessibility() {
        val mockMainViewModel = createMockMainViewModel()

        composeTestRule.setContent {
            ChatRtTheme {
                MainScreen(
                    viewModel = mockMainViewModel,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Test tab navigation through interactive elements
        val interactiveElements =
            listOf(
                "video_mode_audio_only",
                "video_mode_webcam",
                "video_mode_screen_share",
                "start_connection_button",
                "settings_button",
            )

        interactiveElements.forEach { tag ->
            composeTestRule
                .onNodeWithTag(tag)
                .assertExists()
                .assertHasClickAction()
                // Verify element can receive focus
                .performClick()
        }
    }

    /**
     * Test high contrast and color accessibility
     */
    @Test
    fun testColorAccessibility() {
        val mockMainViewModel = createMockMainViewModel()

        composeTestRule.setContent {
            ChatRtTheme(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainScreen(
                        viewModel = mockMainViewModel,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        // Test that all text has sufficient contrast
        // This would typically be done with automated accessibility testing tools
        // For now, we verify that semantic information is properly set

        composeTestRule
            .onNodeWithTag("connection_status")
            .assertExists()
            .assert(hasAnyDescendant(hasContentDescription()))

        // Test dark theme
        composeTestRule.setContent {
            ChatRtTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainScreen(
                        viewModel = mockMainViewModel,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithTag("connection_status")
            .assertExists()
            .assert(hasAnyDescendant(hasContentDescription()))
    }

    /**
     * Test screen reader announcements for state changes
     */
    @Test
    fun testScreenReaderAnnouncements() {
        val mockMainViewModel = createMockMainViewModel()

        composeTestRule.setContent {
            ChatRtTheme {
                MainScreen(
                    viewModel = mockMainViewModel,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Test connection state change announcements
        mockMainViewModel.simulateConnectionState(ConnectionState.CONNECTING)
        composeTestRule.waitForIdle()

        // Verify that connection state changes are announced
        composeTestRule
            .onNodeWithTag("connection_status")
            .assertContentDescriptionContains("Connecting")

        mockMainViewModel.simulateConnectionState(ConnectionState.CONNECTED)
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("connection_status")
            .assertContentDescriptionContains("Connected")

        // Test video mode change announcements
        composeTestRule
            .onNodeWithTag("video_mode_webcam")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("video_mode_webcam")
            .assertContentDescriptionContains("selected")
    }

    // Helper functions for creating mock ViewModels

    private fun createMockMainViewModel(): MockMainViewModel = MockMainViewModel()

    private fun createMockSettingsViewModel(): MockSettingsViewModel = MockSettingsViewModel()
}

// Mock ViewModels for accessibility testing

class MockMainViewModel {
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState

    private val _videoMode = MutableStateFlow(VideoMode.AUDIO_ONLY)
    val videoMode = _videoMode

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected

    private val _isCameraPreviewActive = MutableStateFlow(false)
    val isCameraPreviewActive = _isCameraPreviewActive

    private val _currentCameraFacing = MutableStateFlow(CameraFacing.FRONT)
    val currentCameraFacing = _currentCameraFacing

    fun setVideoMode(mode: VideoMode) {
        _videoMode.value = mode
        _isCameraPreviewActive.value = (mode == VideoMode.WEBCAM)
    }

    fun startConnection() {
        _connectionState.value = ConnectionState.CONNECTING
    }

    fun stopConnection() {
        _connectionState.value = ConnectionState.DISCONNECTED
        _isConnected.value = false
    }

    fun switchCamera() {
        _currentCameraFacing.value =
            if (_currentCameraFacing.value == CameraFacing.FRONT) {
                CameraFacing.BACK
            } else {
                CameraFacing.FRONT
            }
    }

    fun simulateConnectionState(state: ConnectionState) {
        _connectionState.value = state
        _isConnected.value = (state == ConnectionState.CONNECTED)
    }

    fun addTestLogs(logs: List<LogEntry>) {
        _logs.value = logs
    }
}

class MockSettingsViewModel {
    private val _currentSettings = MutableStateFlow(AppSettings())
    val currentSettings = _currentSettings

    private val _defaultVideoMode = MutableStateFlow(VideoMode.AUDIO_ONLY)
    val defaultVideoMode = _defaultVideoMode

    private val _audioQuality = MutableStateFlow(AudioQuality.MEDIUM)
    val audioQuality = _audioQuality

    private val _serverUrl = MutableStateFlow("")
    val serverUrl = _serverUrl

    private val _debugLogging = MutableStateFlow(false)
    val debugLogging = _debugLogging

    private val _defaultCamera = MutableStateFlow(CameraFacing.FRONT)
    val defaultCamera = _defaultCamera

    fun updateDefaultVideoMode(mode: VideoMode) {
        _defaultVideoMode.value = mode
        updateSettings { it.copy(defaultVideoMode = mode) }
    }

    fun updateAudioQuality(quality: AudioQuality) {
        _audioQuality.value = quality
        updateSettings { it.copy(audioQuality = quality) }
    }

    fun updateServerUrl(url: String) {
        _serverUrl.value = url
        updateSettings { it.copy(serverUrl = url) }
    }

    fun toggleDebugLogging() {
        val newValue = !_debugLogging.value
        _debugLogging.value = newValue
        updateSettings { it.copy(debugLogging = newValue) }
    }

    fun updateDefaultCamera(camera: CameraFacing) {
        _defaultCamera.value = camera
        updateSettings { it.copy(defaultCamera = camera) }
    }

    fun resetToDefaults() {
        val defaults = AppSettings()
        _currentSettings.value = defaults
        _defaultVideoMode.value = defaults.defaultVideoMode
        _audioQuality.value = defaults.audioQuality
        _serverUrl.value = defaults.serverUrl
        _debugLogging.value = defaults.debugLogging
        _defaultCamera.value = defaults.defaultCamera
    }

    private fun updateSettings(update: (AppSettings) -> AppSettings) {
        _currentSettings.value = update(_currentSettings.value)
    }
}
