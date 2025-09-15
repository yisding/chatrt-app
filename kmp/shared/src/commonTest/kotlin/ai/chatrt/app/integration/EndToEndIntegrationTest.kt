package ai.chatrt.app.integration

import ai.chatrt.app.models.*
import ai.chatrt.app.network.ChatRtApiService
import ai.chatrt.app.repository.ChatRepositoryImpl
import ai.chatrt.app.repository.SettingsRepositoryImpl
import ai.chatrt.app.viewmodel.MainViewModel
import ai.chatrt.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Comprehensive end-to-end integration tests for complete user flows
 * Tests all three video modes: audio-only, webcam, and screen sharing
 * Requirements: 1.1, 2.1, 3.1, 4.1, 5.1, 6.1, 6.2
 */
class EndToEndIntegrationTest {
    private lateinit var apiService: ChatRtApiService
    private lateinit var mockWebRtcManager: MockWebRtcManager
    private lateinit var mockAudioManager: MockAudioManager
    private lateinit var mockVideoManager: MockVideoManager
    private lateinit var mockScreenCaptureManager: MockScreenCaptureManager
    private lateinit var mockPermissionManager: MockPermissionManager
    private lateinit var chatRepository: ChatRepositoryImpl
    private lateinit var settingsRepository: SettingsRepositoryImpl
    private lateinit var mainViewModel: MainViewModel
    private lateinit var settingsViewModel: SettingsViewModel

    @BeforeTest
    fun setup() {
        // Initialize all components for end-to-end testing
        apiService = ChatRtApiService("https://test-api.chatrt.com")
        mockWebRtcManager = MockWebRtcManager()
        mockAudioManager = MockAudioManager()
        mockVideoManager = MockVideoManager()
        mockScreenCaptureManager = MockScreenCaptureManager()
        mockPermissionManager = MockPermissionManager()

        chatRepository = ChatRepositoryImpl(apiService, mockWebRtcManager)
        settingsRepository = SettingsRepositoryImpl()

        mainViewModel =
            MainViewModel(
                chatRepository = chatRepository,
                audioManager = mockAudioManager,
                videoManager = mockVideoManager,
                screenCaptureManager = mockScreenCaptureManager,
                permissionManager = mockPermissionManager,
            )

        settingsViewModel = SettingsViewModel(settingsRepository)
    }

    @AfterTest
    fun tearDown() {
        chatRepository.cleanup()
        apiService.close()
    }

    /**
     * Test complete audio-only conversation flow
     * Requirements: 1.1, 1.2, 1.6
     */
    @Test
    fun testCompleteAudioOnlyFlow() =
        runTest {
            // Step 1: Initial state verification
            assertEquals(ConnectionState.DISCONNECTED, mainViewModel.connectionState.first())
            assertEquals(VideoMode.AUDIO_ONLY, mainViewModel.videoMode.first())
            assertFalse(mainViewModel.isConnected.first())

            // Step 2: Request microphone permissions
            mockPermissionManager.setPermissionResult(PermissionType.MICROPHONE, true)
            val micPermissionResult = mainViewModel.requestMicrophonePermission()
            assertTrue(micPermissionResult)

            // Step 3: Start voice chat (Requirement 1.2)
            mainViewModel.setVideoMode(VideoMode.AUDIO_ONLY)
            mainViewModel.startConnection()

            // Simulate successful WebRTC connection
            mockWebRtcManager.simulateConnectionStateChange(ConnectionState.CONNECTING)
            delay(100)
            assertEquals(ConnectionState.CONNECTING, mainViewModel.connectionState.first())

            mockWebRtcManager.simulateConnectionStateChange(ConnectionState.CONNECTED)
            delay(100)
            assertEquals(ConnectionState.CONNECTED, mainViewModel.connectionState.first())
            assertTrue(mainViewModel.isConnected.first())

            // Step 4: Verify audio streaming setup
            assertTrue(mockAudioManager.isAudioStreamActive)
            assertEquals(AudioMode.COMMUNICATION, mockAudioManager.currentAudioMode)

            // Step 5: Simulate conversation with logs
            val initialLogs = mainViewModel.logs.first()
            assertTrue(initialLogs.any { it.message.contains("Starting voice chat") })
            assertTrue(initialLogs.any { it.message.contains("WebRTC connection established") })

            // Step 6: End chat gracefully (Requirement 1.6)
            mainViewModel.stopConnection()
            delay(100)

            assertEquals(ConnectionState.DISCONNECTED, mainViewModel.connectionState.first())
            assertFalse(mainViewModel.isConnected.first())
            assertFalse(mockAudioManager.isAudioStreamActive)

            // Verify cleanup logs
            val finalLogs = mainViewModel.logs.first()
            assertTrue(finalLogs.any { it.message.contains("Connection ended") })
        }

    /**
     * Test complete webcam video conversation flow
     * Requirements: 2.1, 2.2, 2.4, 2.5
     */
    @Test
    fun testCompleteWebcamVideoFlow() =
        runTest {
            // Step 1: Request camera permissions (Requirement 2.1)
            mockPermissionManager.setPermissionResult(PermissionType.CAMERA, true)
            mockPermissionManager.setPermissionResult(PermissionType.MICROPHONE, true)

            val cameraPermissionResult = mainViewModel.requestCameraPermission()
            assertTrue(cameraPermissionResult)

            // Step 2: Set video mode to webcam
            mainViewModel.setVideoMode(VideoMode.WEBCAM)
            assertEquals(VideoMode.WEBCAM, mainViewModel.videoMode.first())

            // Step 3: Verify camera preview setup (Requirement 2.2)
            mockVideoManager.setFrontCameraAvailable(true)
            mockVideoManager.setBackCameraAvailable(true)

            assertTrue(mainViewModel.isCameraPreviewActive.first())
            assertEquals(CameraFacing.FRONT, mainViewModel.currentCameraFacing.first()) // Default front camera

            // Step 4: Start video chat
            mainViewModel.startConnection()

            // Simulate successful connection
            mockWebRtcManager.simulateConnectionStateChange(ConnectionState.CONNECTED)
            delay(100)

            assertTrue(mainViewModel.isConnected.first())
            assertTrue(mockVideoManager.isVideoStreamActive)
            assertTrue(mockAudioManager.isAudioStreamActive)

            // Step 5: Test camera switching (Requirement 2.4)
            mainViewModel.switchCamera()
            delay(50)
            assertEquals(CameraFacing.BACK, mainViewModel.currentCameraFacing.first())

            mainViewModel.switchCamera()
            delay(50)
            assertEquals(CameraFacing.FRONT, mainViewModel.currentCameraFacing.first())

            // Step 6: Test orientation handling (Requirement 2.5)
            mainViewModel.handleOrientationChange(90) // Landscape
            assertTrue(mockVideoManager.lastOrientationChange == 90)

            mainViewModel.handleOrientationChange(0) // Portrait
            assertTrue(mockVideoManager.lastOrientationChange == 0)

            // Step 7: End video chat
            mainViewModel.stopConnection()
            delay(100)

            assertFalse(mainViewModel.isConnected.first())
            assertFalse(mockVideoManager.isVideoStreamActive)
            assertFalse(mainViewModel.isCameraPreviewActive.first())
        }

    /**
     * Test complete screen sharing flow
     * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5
     */
    @Test
    fun testCompleteScreenSharingFlow() =
        runTest {
            // Step 1: Request screen capture permissions (Requirement 3.1)
            mockPermissionManager.setPermissionResult(PermissionType.SCREEN_CAPTURE, true)
            mockScreenCaptureManager.setPermissionGranted(true)

            val screenPermissionResult = mainViewModel.requestScreenCapturePermission()
            assertTrue(screenPermissionResult)

            // Step 2: Set video mode to screen share
            mainViewModel.setVideoMode(VideoMode.SCREEN_SHARE)
            assertEquals(VideoMode.SCREEN_SHARE, mainViewModel.videoMode.first())

            // Step 3: Start screen sharing (Requirement 3.2)
            mainViewModel.startConnection()

            // Simulate successful connection
            mockWebRtcManager.simulateConnectionStateChange(ConnectionState.CONNECTED)
            delay(100)

            assertTrue(mainViewModel.isConnected.first())
            assertTrue(mockScreenCaptureManager.isScreenCaptureActive)
            assertTrue(mockAudioManager.isAudioStreamActive)

            // Step 4: Verify persistent notification (Requirement 3.3)
            assertTrue(mockScreenCaptureManager.isNotificationShown)
            val notification = mockScreenCaptureManager.getCurrentNotification()
            assertNotNull(notification)
            assertTrue(notification.title.contains("Screen recording"))

            // Step 5: Test background continuation (Requirement 3.4)
            mainViewModel.handleAppBackground()
            delay(100)

            // Screen sharing should continue in background
            assertTrue(mockScreenCaptureManager.isScreenCaptureActive)
            assertTrue(mainViewModel.isConnected.first())

            // Step 6: Return to foreground
            mainViewModel.handleAppForeground()
            delay(100)

            assertTrue(mockScreenCaptureManager.isScreenCaptureActive)

            // Step 7: End screen sharing (Requirement 3.5)
            mainViewModel.stopConnection()
            delay(100)

            assertFalse(mainViewModel.isConnected.first())
            assertFalse(mockScreenCaptureManager.isScreenCaptureActive)
            assertFalse(mockScreenCaptureManager.isNotificationShown)
        }

    /**
     * Test complete settings configuration flow
     * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5
     */
    @Test
    fun testCompleteSettingsFlow() =
        runTest {
            // Step 1: Verify initial settings
            val initialSettings = settingsViewModel.currentSettings.first()
            assertEquals(AppSettings(), initialSettings)

            // Step 2: Update default video mode (Requirement 6.1)
            settingsViewModel.updateDefaultVideoMode(VideoMode.WEBCAM)
            delay(50)
            assertEquals(VideoMode.WEBCAM, settingsViewModel.defaultVideoMode.first())

            // Step 3: Update audio quality (Requirement 6.2)
            settingsViewModel.updateAudioQuality(AudioQuality.HIGH)
            delay(50)
            assertEquals(AudioQuality.HIGH, settingsViewModel.audioQuality.first())

            // Step 4: Configure server URL (Requirement 6.5)
            val testServerUrl = "https://custom-chatrt-server.com"
            settingsViewModel.updateServerUrl(testServerUrl)
            delay(50)
            assertEquals(testServerUrl, settingsViewModel.serverUrl.first())

            // Step 5: Enable debug logging (Requirement 6.4)
            settingsViewModel.toggleDebugLogging()
            delay(50)
            assertTrue(settingsViewModel.debugLogging.first())

            // Step 6: Update camera preference (Requirement 6.3)
            settingsViewModel.updateDefaultCamera(CameraFacing.BACK)
            delay(50)
            assertEquals(CameraFacing.BACK, settingsViewModel.defaultCamera.first())

            // Step 7: Verify settings persistence
            val updatedSettings = settingsViewModel.currentSettings.first()
            assertEquals(VideoMode.WEBCAM, updatedSettings.defaultVideoMode)
            assertEquals(AudioQuality.HIGH, updatedSettings.audioQuality)
            assertEquals(testServerUrl, updatedSettings.serverUrl)
            assertTrue(updatedSettings.debugLogging)
            assertEquals(CameraFacing.BACK, updatedSettings.defaultCamera)

            // Step 8: Test settings reset
            settingsViewModel.resetToDefaults()
            delay(50)

            val resetSettings = settingsViewModel.currentSettings.first()
            assertEquals(AppSettings(), resetSettings)
        }

    /**
     * Test real-time connection monitoring and logging
     * Requirements: 4.1, 4.2, 4.3, 4.4
     */
    @Test
    fun testRealTimeMonitoringFlow() =
        runTest {
            // Step 1: Enable debug logging
            settingsViewModel.toggleDebugLogging()
            assertTrue(settingsViewModel.debugLogging.first())

            // Step 2: Start connection with monitoring (Requirement 4.1)
            mainViewModel.setVideoMode(VideoMode.AUDIO_ONLY)
            mainViewModel.startConnection()

            // Simulate connection progress
            mockWebRtcManager.simulateConnectionStateChange(ConnectionState.CONNECTING)
            delay(100)

            // Verify loading indicator and progress (Requirement 4.1)
            assertEquals(ConnectionState.CONNECTING, mainViewModel.connectionState.first())
            val connectingLogs = mainViewModel.logs.first()
            assertTrue(connectingLogs.any { it.message.contains("Connecting") })

            // Step 3: Successful connection (Requirement 4.2)
            mockWebRtcManager.simulateConnectionStateChange(ConnectionState.CONNECTED)
            delay(100)

            assertEquals(ConnectionState.CONNECTED, mainViewModel.connectionState.first())
            val connectedLogs = mainViewModel.logs.first()
            assertTrue(connectedLogs.any { it.message.contains("Connected") })

            // Step 4: Simulate connection issues (Requirement 4.3)
            mockWebRtcManager.simulateConnectionError("Network timeout")
            delay(100)

            assertEquals(ConnectionState.FAILED, mainViewModel.connectionState.first())
            val errorLogs = mainViewModel.logs.first()
            assertTrue(errorLogs.any { it.level == LogLevel.ERROR })
            assertTrue(errorLogs.any { it.message.contains("Network timeout") })

            // Step 5: Verify debug information (Requirement 4.4)
            val debugInfo = mainViewModel.getDebugInfo()
            assertNotNull(debugInfo)
            assertTrue(debugInfo.webRtcEvents.isNotEmpty())
            assertTrue(debugInfo.apiCalls.isNotEmpty())
            assertTrue(debugInfo.systemInfo.isNotEmpty())
        }

    /**
     * Test Android-specific system integration scenarios
     * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5
     */
    @Test
    fun testAndroidSystemIntegrationFlow() =
        runTest {
            // Step 1: Start active call
            mainViewModel.setVideoMode(VideoMode.WEBCAM)
            mainViewModel.startConnection()
            mockWebRtcManager.simulateConnectionStateChange(ConnectionState.CONNECTED)
            delay(100)
            assertTrue(mainViewModel.isConnected.first())

            // Step 2: Test background continuation (Requirement 5.1)
            mainViewModel.handleAppBackground()
            delay(100)
            assertTrue(mainViewModel.isConnected.first()) // Should maintain connection

            // Step 3: Test phone call interruption (Requirement 5.2)
            mainViewModel.handlePhoneCallInterruption(PhoneCallState.INCOMING)
            delay(100)
            assertTrue(mainViewModel.isCallPaused.first())

            mainViewModel.handlePhoneCallInterruption(PhoneCallState.ENDED)
            delay(100)
            assertFalse(mainViewModel.isCallPaused.first())

            // Step 4: Test headphone connection (Requirement 5.3)
            val headphones = AudioDevice("headphones", "Wired Headphones", AudioDeviceType.WIRED_HEADSET, false)
            mockAudioManager.simulateDeviceChange(headphones)
            delay(100)
            assertEquals(headphones, mainViewModel.currentAudioDevice.first())

            // Step 5: Test orientation changes (Requirement 5.4)
            mainViewModel.handleOrientationChange(90) // Landscape
            mainViewModel.handleOrientationChange(0) // Portrait
            assertTrue(mainViewModel.isConnected.first()) // Should maintain connection

            // Step 6: Test proper cleanup (Requirement 5.5)
            mainViewModel.handleAppTermination()
            delay(100)
            assertFalse(mainViewModel.isConnected.first())
            assertFalse(mockWebRtcManager.isConnectionActive)
        }

    /**
     * Test error handling and recovery scenarios
     * Requirements: 1.6, 2.6, 3.6, 4.3, 5.3
     */
    @Test
    fun testErrorHandlingAndRecoveryFlow() =
        runTest {
            // Test camera permission denied with fallback (Requirement 2.6)
            mockPermissionManager.setPermissionResult(PermissionType.CAMERA, false)
            mainViewModel.setVideoMode(VideoMode.WEBCAM)

            val cameraPermissionResult = mainViewModel.requestCameraPermission()
            assertFalse(cameraPermissionResult)

            // Should fallback to audio-only mode
            assertEquals(VideoMode.AUDIO_ONLY, mainViewModel.videoMode.first())

            val logs = mainViewModel.logs.first()
            assertTrue(logs.any { it.message.contains("audio-only mode due to camera permission") })

            // Test screen capture permission denied (Requirement 3.6)
            mockPermissionManager.setPermissionResult(PermissionType.SCREEN_CAPTURE, false)
            mainViewModel.setVideoMode(VideoMode.SCREEN_SHARE)

            val screenPermissionResult = mainViewModel.requestScreenCapturePermission()
            assertFalse(screenPermissionResult)

            // Should display error and offer alternatives
            val currentError = mainViewModel.error.first()
            assertNotNull(currentError)
            assertTrue(currentError is ChatRtError.PermissionDenied)

            // Test connection failure with retry (Requirement 4.3)
            mockWebRtcManager.shouldFailConnection = true
            mainViewModel.setVideoMode(VideoMode.AUDIO_ONLY)
            mainViewModel.startConnection()
            delay(100)

            assertEquals(ConnectionState.FAILED, mainViewModel.connectionState.first())

            // Test retry mechanism
            mockWebRtcManager.shouldFailConnection = false
            mainViewModel.retryConnection()
            mockWebRtcManager.simulateConnectionStateChange(ConnectionState.CONNECTED)
            delay(100)

            assertEquals(ConnectionState.CONNECTED, mainViewModel.connectionState.first())
        }

    /**
     * Test performance and memory management
     * Requirements: Performance testing and memory leak detection
     */
    @Test
    fun testPerformanceAndMemoryManagement() =
        runTest {
            // Test multiple connection cycles
            repeat(5) { cycle ->
                // Start connection
                mainViewModel.setVideoMode(VideoMode.WEBCAM)
                mainViewModel.startConnection()
                mockWebRtcManager.simulateConnectionStateChange(ConnectionState.CONNECTED)
                delay(100)

                assertTrue(mainViewModel.isConnected.first())

                // Generate some activity
                mainViewModel.switchCamera()
                mainViewModel.handleOrientationChange(90)
                delay(50)

                // Stop connection
                mainViewModel.stopConnection()
                delay(100)

                assertFalse(mainViewModel.isConnected.first())

                // Verify cleanup after each cycle
                assertFalse(mockWebRtcManager.isConnectionActive)
                assertFalse(mockVideoManager.isVideoStreamActive)
            }

            // Verify no memory leaks by checking that resources are properly released
            assertTrue(mockWebRtcManager.cleanupCallCount >= 5)
            assertTrue(mockVideoManager.stopCaptureCallCount >= 5)
            assertTrue(mockAudioManager.releaseResourcesCallCount >= 5)
        }
}

// Enhanced mock implementations for comprehensive testing

class MockWebRtcManager {
    var isConnectionActive = false
    var shouldFailConnection = false
    var cleanupCallCount = 0
    private var connectionStateCallback: ((ConnectionState) -> Unit)? = null

    fun simulateConnectionStateChange(state: ConnectionState) {
        isConnectionActive = (state == ConnectionState.CONNECTED)
        connectionStateCallback?.invoke(state)
    }

    fun simulateConnectionError(message: String) {
        isConnectionActive = false
        connectionStateCallback?.invoke(ConnectionState.FAILED)
    }

    fun setConnectionStateCallback(callback: (ConnectionState) -> Unit) {
        connectionStateCallback = callback
    }

    fun cleanup() {
        cleanupCallCount++
        isConnectionActive = false
    }
}

class MockAudioManager {
    var isAudioStreamActive = false
    var currentAudioMode = AudioMode.NORMAL
    var currentDevice: AudioDevice? = null
    var releaseResourcesCallCount = 0
    private var deviceChangeCallback: ((AudioDevice) -> Unit)? = null

    fun simulateDeviceChange(device: AudioDevice) {
        currentDevice = device
        deviceChangeCallback?.invoke(device)
    }

    fun setDeviceChangeCallback(callback: (AudioDevice) -> Unit) {
        deviceChangeCallback = callback
    }

    fun releaseResources() {
        releaseResourcesCallCount++
        isAudioStreamActive = false
    }
}

class MockVideoManager {
    var isVideoStreamActive = false
    var isFrontCameraAvailable = true
    var isBackCameraAvailable = true
    var currentCameraFacing = CameraFacing.FRONT
    var lastOrientationChange = 0
    var stopCaptureCallCount = 0

    fun setFrontCameraAvailable(available: Boolean) {
        isFrontCameraAvailable = available
    }

    fun setBackCameraAvailable(available: Boolean) {
        isBackCameraAvailable = available
    }

    fun switchCamera() {
        currentCameraFacing =
            if (currentCameraFacing == CameraFacing.FRONT) {
                CameraFacing.BACK
            } else {
                CameraFacing.FRONT
            }
    }

    fun handleOrientationChange(orientation: Int) {
        lastOrientationChange = orientation
    }

    fun stopCapture() {
        stopCaptureCallCount++
        isVideoStreamActive = false
    }
}

class MockScreenCaptureManager {
    var isScreenCaptureActive = false
    var isNotificationShown = false
    var isPermissionGranted = false
    private var currentNotification: ScreenCaptureNotification? = null

    fun setPermissionGranted(granted: Boolean) {
        isPermissionGranted = granted
    }

    fun startScreenCapture() {
        if (isPermissionGranted) {
            isScreenCaptureActive = true
            showNotification()
        }
    }

    fun stopScreenCapture() {
        isScreenCaptureActive = false
        hideNotification()
    }

    private fun showNotification() {
        isNotificationShown = true
        currentNotification =
            ScreenCaptureNotification(
                title = "Screen recording active",
                message = "ChatRT is recording your screen",
            )
    }

    private fun hideNotification() {
        isNotificationShown = false
        currentNotification = null
    }

    fun getCurrentNotification(): ScreenCaptureNotification? = currentNotification
}

class MockPermissionManager {
    private val permissionResults = mutableMapOf<PermissionType, Boolean>()

    fun setPermissionResult(
        permission: PermissionType,
        granted: Boolean,
    ) {
        permissionResults[permission] = granted
    }

    fun checkPermission(permission: PermissionType): Boolean = permissionResults[permission] ?: false

    suspend fun requestPermission(permission: PermissionType): Boolean = permissionResults[permission] ?: false
}

data class ScreenCaptureNotification(
    val title: String,
    val message: String,
)

enum class PermissionType {
    MICROPHONE,
    CAMERA,
    SCREEN_CAPTURE,
}

enum class AudioMode {
    NORMAL,
    COMMUNICATION,
}

enum class AudioDeviceType {
    SPEAKER,
    WIRED_HEADSET,
    BLUETOOTH_HEADSET,
}

data class AudioDevice(
    val id: String,
    val name: String,
    val type: AudioDeviceType,
    val isBuiltIn: Boolean,
)

enum class PhoneCallState {
    INCOMING,
    ACTIVE,
    ENDED,
}
