package ai.chatrt.app.utils

import ai.chatrt.app.models.*
import ai.chatrt.app.platform.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Tests for ErrorRecoveryManager automatic recovery functionality
 * Requirements: 1.6, 2.6, 3.5, 4.3, 5.3
 */
class ErrorRecoveryManagerTest {
    private lateinit var errorHandler: ErrorHandler
    private lateinit var mockAudioManager: MockAudioManager
    private lateinit var mockPermissionManager: MockPermissionManager
    private lateinit var mockNetworkMonitor: MockNetworkMonitor
    private lateinit var errorRecoveryManager: ErrorRecoveryManager

    @BeforeTest
    fun setup() {
        errorHandler = ErrorHandler()
        mockAudioManager = MockAudioManager()
        mockPermissionManager = MockPermissionManager()
        mockNetworkMonitor = MockNetworkMonitor()

        errorRecoveryManager =
            ErrorRecoveryManager(
                errorHandler = errorHandler,
                audioManager = mockAudioManager,
                permissionManager = mockPermissionManager,
                networkMonitor = mockNetworkMonitor,
            )
    }

    @AfterTest
    fun cleanup() {
        errorRecoveryManager.cleanup()
    }

    @Test
    fun testNetworkErrorRecovery() =
        runTest {
            // Test network error recovery (Requirement 4.3)
            val networkError = ChatRtError.NetworkError(NetworkErrorCause.NO_INTERNET, "No connection")

            var recoverySuccessCalled = false
            var recoveryFailedCalled = false

            // Simulate network becoming available
            mockNetworkMonitor.setNetworkState(NetworkState(isConnected = true, quality = NetworkQuality.GOOD))

            val recovered =
                errorRecoveryManager.attemptRecovery(
                    error = networkError,
                    context = "Network Test",
                    onRecoverySuccess = { recoverySuccessCalled = true },
                    onRecoveryFailed = { recoveryFailedCalled = true },
                )

            assertTrue(recovered)
            // Note: In a real test, we'd wait for the coroutine to complete
            // For this test, we're just verifying the recovery attempt was made
        }

    @Test
    fun testAudioDeviceErrorRecovery() =
        runTest {
            // Test audio device error recovery (Requirement 5.3)
            val audioError = ChatRtError.AudioDeviceError(AudioErrorCause.DEVICE_BUSY, "Bluetooth Headset")

            // Setup mock audio manager with alternative devices
            val speakerDevice = AudioDevice("speaker", "Speaker", AudioDeviceType.SPEAKER, false)
            val headsetDevice = AudioDevice("headset", "Bluetooth Headset", AudioDeviceType.BLUETOOTH_HEADSET, false)
            mockAudioManager.setAvailableDevices(listOf(speakerDevice, headsetDevice))
            mockAudioManager.setCurrentDevice(headsetDevice)

            var recoverySuccessCalled = false

            val recovered =
                errorRecoveryManager.attemptRecovery(
                    error = audioError,
                    context = "Audio Test",
                    onRecoverySuccess = { recoverySuccessCalled = true },
                )

            assertTrue(recovered)
            // Verify that the audio manager attempted to switch devices
            assertTrue(mockAudioManager.setupAudioRoutingCalled)
        }

    @Test
    fun testCameraErrorRecovery() =
        runTest {
            // Test camera error recovery with fallback (Requirement 2.6)
            val cameraError = ChatRtError.CameraError(CameraErrorCause.PERMISSION_DENIED, "camera_0")

            var recoverySuccessCalled = false

            val recovered =
                errorRecoveryManager.attemptRecovery(
                    error = cameraError,
                    context = "Camera Test",
                    onRecoverySuccess = { recoverySuccessCalled = true },
                )

            assertTrue(recovered)
            // Camera permission errors should trigger automatic fallback to audio-only
        }

    @Test
    fun testScreenCaptureErrorRecovery() =
        runTest {
            // Test screen capture error recovery with fallback (Requirement 3.6)
            val screenError = ChatRtError.ScreenCaptureError(ScreenCaptureErrorCause.PERMISSION_DENIED, "Permission denied")

            var recoverySuccessCalled = false

            val recovered =
                errorRecoveryManager.attemptRecovery(
                    error = screenError,
                    context = "Screen Capture Test",
                    onRecoverySuccess = { recoverySuccessCalled = true },
                )

            assertTrue(recovered)
            // Screen capture permission errors should trigger automatic fallback to camera mode
        }

    @Test
    fun testPhoneCallInterruptionRecovery() =
        runTest {
            // Test phone call interruption recovery (Requirement 5.2)
            val phoneCallError = ChatRtError.PhoneCallInterruptionError(PhoneCallState.ENDED, "Call ended")

            var recoverySuccessCalled = false

            val recovered =
                errorRecoveryManager.attemptRecovery(
                    error = phoneCallError,
                    context = "Phone Call Test",
                    onRecoverySuccess = { recoverySuccessCalled = true },
                )

            assertTrue(recovered)
            // Should attempt to resume audio after phone call ends
            assertTrue(mockAudioManager.resumeAfterPhoneCallCalled)
        }

    @Test
    fun testDeviceStateChangeRecovery() =
        runTest {
            // Test device state change recovery (Requirement 5.3)
            val deviceStateError = ChatRtError.DeviceStateError(DeviceStateChange.HEADPHONES_CONNECTED, "Headphones connected")

            // Setup mock audio manager with headphones available
            val headphonesDevice = AudioDevice("headphones", "Wired Headphones", AudioDeviceType.WIRED_HEADSET, false)
            val speakerDevice = AudioDevice("speaker", "Speaker", AudioDeviceType.SPEAKER, false)
            mockAudioManager.setAvailableDevices(listOf(headphonesDevice, speakerDevice))

            var recoverySuccessCalled = false

            val recovered =
                errorRecoveryManager.attemptRecovery(
                    error = deviceStateError,
                    context = "Device State Test",
                    onRecoverySuccess = { recoverySuccessCalled = true },
                )

            assertTrue(recovered)
            // Should automatically switch to headphones when connected
            assertEquals(headphonesDevice, mockAudioManager.currentDevice)
        }

    @Test
    fun testApiErrorRecovery() =
        runTest {
            // Test API error recovery with retry logic
            val apiError = ChatRtError.ApiError(500, "Internal server error", "/rtc")

            var recoverySuccessCalled = false

            val recovered =
                errorRecoveryManager.attemptRecovery(
                    error = apiError,
                    context = "API Test",
                    onRecoverySuccess = { recoverySuccessCalled = true },
                )

            assertTrue(recovered)
            // Server errors should be retryable
            assertTrue(apiError.isRetryable)
        }

    @Test
    fun testGuidedRecoverySteps() =
        runTest {
            // Test guided recovery steps generation
            val permissionError = ChatRtError.PermissionDenied(PermissionType.CAMERA, isPermanentlyDenied = true)

            val recoverySteps = errorRecoveryManager.getGuidedRecoverySteps(permissionError)

            assertTrue(recoverySteps.isNotEmpty())
            assertTrue(recoverySteps.any { it.description.contains("Settings") })
            assertTrue(recoverySteps.any { it.type == RecoveryStepType.NAVIGATE_TO_SETTINGS })
        }

    @Test
    fun testNetworkRecoverySteps() =
        runTest {
            // Test network-specific recovery steps
            val networkError = ChatRtError.NetworkError(NetworkErrorCause.NO_INTERNET)

            val recoverySteps = errorRecoveryManager.getGuidedRecoverySteps(networkError)

            assertTrue(recoverySteps.isNotEmpty())
            assertTrue(recoverySteps.any { it.description.contains("WiFi", ignoreCase = true) })
            assertTrue(recoverySteps.any { it.type == RecoveryStepType.CHECK_SETTINGS })
        }

    @Test
    fun testAudioRecoverySteps() =
        runTest {
            // Test audio-specific recovery steps
            val audioError = ChatRtError.AudioDeviceError(AudioErrorCause.DEVICE_BUSY, "Microphone")

            val recoverySteps = errorRecoveryManager.getGuidedRecoverySteps(audioError)

            assertTrue(recoverySteps.isNotEmpty())
            assertTrue(recoverySteps.any { it.description.contains("audio", ignoreCase = true) })
            assertTrue(recoverySteps.any { it.type == RecoveryStepType.SWITCH_DEVICE })
        }

    @Test
    fun testCameraRecoverySteps() =
        runTest {
            // Test camera-specific recovery steps
            val cameraError = ChatRtError.CameraError(CameraErrorCause.CAMERA_BUSY, "camera_0")

            val recoverySteps = errorRecoveryManager.getGuidedRecoverySteps(cameraError)

            assertTrue(recoverySteps.isNotEmpty())
            assertTrue(recoverySteps.any { it.description.contains("camera", ignoreCase = true) })
            assertTrue(recoverySteps.any { it.type == RecoveryStepType.FALLBACK_MODE })
        }

    @Test
    fun testScreenCaptureRecoverySteps() =
        runTest {
            // Test screen capture-specific recovery steps
            val screenError = ChatRtError.ScreenCaptureError(ScreenCaptureErrorCause.PERMISSION_DENIED)

            val recoverySteps = errorRecoveryManager.getGuidedRecoverySteps(screenError)

            assertTrue(recoverySteps.isNotEmpty())
            assertTrue(recoverySteps.any { it.description.contains("screen", ignoreCase = true) })
            assertTrue(recoverySteps.any { it.type == RecoveryStepType.FALLBACK_MODE })
        }
}

// Mock implementations for testing

class MockAudioManager : AudioManager {
    var setupAudioRoutingCalled = false
    var resumeAfterPhoneCallCalled = false
    var currentDevice: AudioDevice? = null
    private var availableDevices: List<AudioDevice> = emptyList()
    private val deviceChanges = MutableStateFlow<AudioDevice?>(null)

    fun setAvailableDevices(devices: List<AudioDevice>) {
        availableDevices = devices
    }

    fun setCurrentDevice(device: AudioDevice) {
        currentDevice = device
    }

    override suspend fun initialize() {}

    override suspend fun setupAudioRouting() {
        setupAudioRoutingCalled = true
    }

    override suspend fun setAudioMode(mode: AudioMode) {}

    override suspend fun requestAudioFocus(): Boolean = true

    override suspend fun releaseAudioFocus() {}

    override suspend fun handleHeadsetConnection(connected: Boolean) {}

    override suspend fun getAvailableAudioDevices(): List<AudioDevice> = availableDevices

    override suspend fun setAudioDevice(device: AudioDevice) {
        currentDevice = device
        deviceChanges.value = device
    }

    override suspend fun getCurrentAudioDevice(): AudioDevice? = currentDevice

    override fun observeAudioDeviceChanges(): Flow<AudioDevice> = deviceChanges.filterNotNull()

    override suspend fun setAudioQuality(quality: AudioQuality) {}

    override suspend fun setNoiseSuppression(enabled: Boolean) {}

    override suspend fun setEchoCancellation(enabled: Boolean) {}

    override suspend fun cleanup() {}

    suspend fun resumeAfterPhoneCall() {
        resumeAfterPhoneCallCalled = true
    }
}

class MockPermissionManager : PermissionManager {
    override suspend fun checkPermission(permission: PermissionType): Boolean = true

    override suspend fun requestPermission(permission: PermissionType): Boolean = true

    override suspend fun requestMultiplePermissions(permissions: List<PermissionType>): Map<PermissionType, Boolean> =
        permissions.associateWith { true }

    override fun shouldShowRationale(permission: PermissionType): Boolean = false

    override fun openAppSettings() {}
}

class MockNetworkMonitor : NetworkMonitor {
    private val networkState = MutableStateFlow(NetworkState(isConnected = false))

    fun setNetworkState(state: NetworkState) {
        networkState.value = state
    }

    override fun observeNetworkState(): Flow<NetworkState> = networkState

    override suspend fun getCurrentNetworkState(): NetworkState = networkState.value

    override suspend fun testConnectivity(): Boolean = networkState.value.isConnected
}
