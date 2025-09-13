package ai.chatrt.app.integration

import ai.chatrt.app.models.*
import ai.chatrt.app.platform.*
import ai.chatrt.app.repository.ChatRepository
import ai.chatrt.app.utils.*
import ai.chatrt.app.viewmodel.MainViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Integration tests for comprehensive error handling system
 * Tests the complete error handling flow from detection to recovery
 * Requirements: 1.6, 2.6, 3.5, 4.3, 5.3
 */
class ErrorHandlingIntegrationTest {
    private lateinit var mockChatRepository: MockChatRepository
    private lateinit var mockAudioManager: MockAudioManager
    private lateinit var mockPermissionManager: MockPermissionManager
    private lateinit var mockNetworkMonitor: MockNetworkMonitor
    private lateinit var mainViewModel: MainViewModel

    @BeforeTest
    fun setup() {
        mockChatRepository = MockChatRepository()
        mockAudioManager = MockAudioManager()
        mockPermissionManager = MockPermissionManager()
        mockNetworkMonitor = MockNetworkMonitor()

        mainViewModel =
            MainViewModel(
                chatRepository = mockChatRepository,
                audioManager = mockAudioManager,
                permissionManager = mockPermissionManager,
                networkMonitor = mockNetworkMonitor,
            )
    }

    @Test
    fun testNetworkErrorHandlingFlow() =
        runTest {
            // Test complete network error handling flow (Requirement 4.3)

            // Simulate network failure during connection
            mockChatRepository.shouldFailConnection = true
            mockChatRepository.connectionError = Exception("Network connection failed")

            // Start connection - should trigger network error
            mainViewModel.startConnection()

            // Verify error is handled
            val currentError = mainViewModel.error.value
            assertNotNull(currentError)
            assertTrue(currentError is ChatRtError.NetworkError)

            // Verify connection state is failed
            assertEquals(ConnectionState.FAILED, mainViewModel.connectionState.value)

            // Simulate network recovery
            mockNetworkMonitor.setNetworkState(NetworkState(isConnected = true, quality = NetworkQuality.GOOD))
            mockChatRepository.shouldFailConnection = false

            // Retry should succeed
            mainViewModel.retryCurrentError()

            // Eventually connection should succeed (in real implementation)
            // For this test, we verify the retry was attempted
            assertTrue(mockChatRepository.createCallAttempts > 1)
        }

    @Test
    fun testCameraPermissionErrorWithFallback() =
        runTest {
            // Test camera permission error with automatic fallback (Requirement 2.6)

            // Set initial video mode to webcam
            mainViewModel.setVideoMode(VideoMode.WEBCAM)
            assertEquals(VideoMode.WEBCAM, mainViewModel.videoMode.value)

            // Simulate camera permission error
            mainViewModel.handlePermissionError(PermissionType.CAMERA, isPermanentlyDenied = false)

            // Verify error is handled
            val currentError = mainViewModel.error.value
            assertNotNull(currentError)
            assertTrue(currentError is ChatRtError.PermissionDenied)
            assertEquals(PermissionType.CAMERA, currentError.permission)

            // Verify automatic fallback to audio-only mode
            assertEquals(VideoMode.AUDIO_ONLY, mainViewModel.videoMode.value)

            // Verify log entry was created
            val logs = mainViewModel.logs.value
            assertTrue(logs.any { it.message.contains("audio-only mode due to camera permission") })
        }

    @Test
    fun testScreenCaptureErrorWithFallback() =
        runTest {
            // Test screen capture error with automatic fallback (Requirement 3.6)

            // Set initial video mode to screen share
            mainViewModel.setVideoMode(VideoMode.SCREEN_SHARE)
            assertEquals(VideoMode.SCREEN_SHARE, mainViewModel.videoMode.value)

            // Simulate screen capture permission error
            mainViewModel.handleScreenCaptureError(ScreenCaptureErrorCause.PERMISSION_DENIED, "Permission denied")

            // Verify error is handled
            val currentError = mainViewModel.error.value
            assertNotNull(currentError)
            assertTrue(currentError is ChatRtError.ScreenCaptureError)
            assertEquals(ScreenCaptureErrorCause.PERMISSION_DENIED, currentError.cause)

            // Verify automatic fallback to camera mode
            assertEquals(VideoMode.WEBCAM, mainViewModel.videoMode.value)

            // Verify log entry was created
            val logs = mainViewModel.logs.value
            assertTrue(logs.any { it.message.contains("camera mode due to screen capture error") })
        }

    @Test
    fun testAudioDeviceChangeHandling() =
        runTest {
            // Test audio device change handling (Requirement 5.3)

            // Start with speaker
            val speakerDevice = AudioDevice("speaker", "Speaker", AudioDeviceType.SPEAKER, true)
            mockAudioManager.setCurrentDevice(speakerDevice)

            // Simulate headphones connection
            val headphonesDevice = AudioDevice("headphones", "Wired Headphones", AudioDeviceType.WIRED_HEADSET, false)
            mockAudioManager.setAvailableDevices(listOf(speakerDevice, headphonesDevice))
            mockAudioManager.simulateDeviceChange(headphonesDevice)

            // Verify current audio device is updated
            assertEquals(headphonesDevice, mainViewModel.currentAudioDevice.value)

            // Verify device state change error is handled
            val currentError = mainViewModel.error.value
            if (currentError != null) {
                assertTrue(currentError is ChatRtError.DeviceStateError)
                assertEquals(DeviceStateChange.HEADPHONES_CONNECTED, currentError.stateChange)
            }

            // Verify log entry was created
            val logs = mainViewModel.logs.value
            assertTrue(logs.any { it.message.contains("Audio device changed to: Wired Headphones") })
        }

    @Test
    fun testPhoneCallInterruptionHandling() =
        runTest {
            // Test phone call interruption handling (Requirement 5.2)

            // Start a connection
            mainViewModel.startConnection()

            // Simulate incoming phone call
            mainViewModel.handlePhoneCallInterruption(PhoneCallState.INCOMING)

            // Verify call is paused
            assertTrue(mainViewModel.isCallPaused.value)

            // Verify error is handled
            val currentError = mainViewModel.error.value
            assertNotNull(currentError)
            assertTrue(currentError is ChatRtError.PhoneCallInterruptionError)
            assertEquals(PhoneCallState.INCOMING, currentError.callState)

            // Simulate phone call ending
            mainViewModel.handlePhoneCallInterruption(PhoneCallState.ENDED)

            // Verify call is resumed
            assertFalse(mainViewModel.isCallPaused.value)

            // Verify log entries were created
            val logs = mainViewModel.logs.value
            assertTrue(logs.any { it.message.contains("paused due to phone call") })
            assertTrue(logs.any { it.message.contains("resumed after phone call") })
        }

    @Test
    fun testCameraErrorWithFallback() =
        runTest {
            // Test camera error with automatic fallback (Requirement 2.6)

            // Set video mode to webcam
            mainViewModel.setVideoMode(VideoMode.WEBCAM)

            // Simulate camera busy error
            mainViewModel.handleCameraError(CameraErrorCause.CAMERA_BUSY, "camera_0")

            // Verify error is handled
            val currentError = mainViewModel.error.value
            assertNotNull(currentError)
            assertTrue(currentError is ChatRtError.CameraError)
            assertEquals(CameraErrorCause.CAMERA_BUSY, currentError.cause)

            // Verify automatic fallback to audio-only mode
            assertEquals(VideoMode.AUDIO_ONLY, mainViewModel.videoMode.value)

            // Verify log entry was created
            val logs = mainViewModel.logs.value
            assertTrue(logs.any { it.message.contains("audio-only mode due to camera error") })
        }

    @Test
    fun testNetworkQualityAdaptation() =
        runTest {
            // Test network quality adaptation

            // Start with good network quality
            mockNetworkMonitor.setNetworkState(NetworkState(isConnected = true, quality = NetworkQuality.GOOD))

            // Simulate network quality degradation
            mockNetworkMonitor.setNetworkState(NetworkState(isConnected = true, quality = NetworkQuality.POOR))

            // Verify network quality change is handled
            assertEquals(NetworkQuality.POOR, mainViewModel.networkQuality.value)

            // Verify platform optimization suggestion is created
            val optimization = mainViewModel.platformOptimization.value
            assertNotNull(optimization)
            assertEquals(OptimizationReason.POOR_NETWORK, optimization.reason)
            assertEquals(VideoMode.AUDIO_ONLY, optimization.recommendedVideoMode)
        }

    @Test
    fun testErrorRecoverySuggestions() =
        runTest {
            // Test error recovery suggestions

            // Create a network error
            val networkError = ChatRtError.NetworkError(NetworkErrorCause.NO_INTERNET, "No connection")
            mockChatRepository.shouldFailConnection = true
            mockChatRepository.connectionError = Exception("Network connection failed")

            mainViewModel.startConnection()

            // Get recovery suggestions
            val suggestions = mainViewModel.getRecoverySuggestions()

            assertTrue(suggestions.isNotEmpty())
            assertTrue(suggestions.any { it.type == RecoverySuggestionType.RETRY })
            assertTrue(suggestions.any { it.message.contains("WiFi", ignoreCase = true) })
        }

    @Test
    fun testErrorHistoryTracking() =
        runTest {
            // Test error history tracking

            // Generate multiple errors
            mainViewModel.handlePermissionError(PermissionType.CAMERA, false)
            mainViewModel.handleCameraError(CameraErrorCause.CAMERA_BUSY, "camera_0")
            mainViewModel.handleScreenCaptureError(ScreenCaptureErrorCause.PERMISSION_DENIED, "Permission denied")

            // Verify error history is tracked
            val errorHistory = mainViewModel.errorHistory.value
            assertTrue(errorHistory.size >= 3)

            // Verify different error types are tracked
            val errorCodes = errorHistory.map { it.error.errorCode }.toSet()
            assertTrue(errorCodes.contains("PERMISSION_DENIED"))
            assertTrue(errorCodes.contains("CAMERA_ERROR"))
            assertTrue(errorCodes.contains("SCREEN_CAPTURE_ERROR"))
        }

    @Test
    fun testRetryMechanismWithLimits() =
        runTest {
            // Test retry mechanism with maximum attempt limits

            // Set up persistent failure
            mockChatRepository.shouldFailConnection = true
            mockChatRepository.connectionError = Exception("Persistent network error")

            // Attempt connection multiple times
            mainViewModel.startConnection()

            // Retry multiple times
            repeat(5) {
                mainViewModel.retryCurrentError()
            }

            // Verify retry attempts are limited
            assertTrue(mockChatRepository.createCallAttempts <= 4) // Initial + 3 retries max

            // Verify logs indicate max retries reached
            val logs = mainViewModel.logs.value
            assertTrue(logs.any { it.message.contains("Maximum retry attempts") })
        }
}

// Mock implementations for integration testing

class MockChatRepository : ChatRepository {
    var shouldFailConnection = false
    var connectionError: Exception? = null
    var createCallAttempts = 0

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())

    override suspend fun createCall(request: CallRequest): Result<CallResponse> {
        createCallAttempts++

        return if (shouldFailConnection) {
            _connectionState.value = ConnectionState.FAILED
            Result.failure(connectionError ?: Exception("Connection failed"))
        } else {
            _connectionState.value = ConnectionState.CONNECTED
            Result.success(CallResponse(callId = "test-call-id"))
        }
    }

    override suspend fun stopConnection(): Result<Unit> {
        _connectionState.value = ConnectionState.DISCONNECTED
        return Result.success(Unit)
    }

    override suspend fun startConnectionMonitoring(callId: String): Result<Unit> = Result.success(Unit)

    override fun observeConnectionState(): Flow<ConnectionState> = _connectionState

    override fun observeLogs(): Flow<List<LogEntry>> = _logs
}

data class CallResponse(
    val callId: String,
)
