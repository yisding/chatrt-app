package ai.chatrt.app.utils

import ai.chatrt.app.models.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Tests for ErrorHandler comprehensive error handling functionality
 * Requirements: 1.6, 2.6, 3.5, 4.3, 5.3
 */
class ErrorHandlerTest {
    private lateinit var errorHandler: ErrorHandler

    @BeforeTest
    fun setup() {
        errorHandler = ErrorHandler()
    }

    @Test
    fun testNetworkErrorHandling() =
        runTest {
            // Test network error handling (Requirement 4.3)
            val networkError = ChatRtError.NetworkError(NetworkErrorCause.NO_INTERNET, "No connection")

            errorHandler.handleNetworkError(NetworkErrorCause.NO_INTERNET, "No connection")

            val currentError = errorHandler.currentError.value
            assertNotNull(currentError)
            assertTrue(currentError is ChatRtError.NetworkError)
            assertEquals("No internet connection available", currentError.userMessage)
            assertTrue(currentError.isRetryable)
            assertTrue(currentError.suggestions.isNotEmpty())
        }

    @Test
    fun testPermissionErrorHandling() =
        runTest {
            // Test permission error with fallback (Requirements 1.2, 2.6, 3.6)
            var fallbackCalled = false

            errorHandler.handlePermissionError(
                permission = PermissionType.CAMERA,
                isPermanentlyDenied = false,
                onFallback = { fallbackCalled = true },
            )

            val currentError = errorHandler.currentError.value
            assertNotNull(currentError)
            assertTrue(currentError is ChatRtError.PermissionDenied)
            assertEquals(PermissionType.CAMERA, currentError.permission)
            assertFalse(currentError.isPermanentlyDenied)
            assertTrue(fallbackCalled)
        }

    @Test
    fun testCameraErrorWithFallback() =
        runTest {
            // Test camera error with automatic fallback to audio-only (Requirement 2.6)
            var fallbackCalled = false

            errorHandler.handleCameraError(
                cause = CameraErrorCause.PERMISSION_DENIED,
                cameraId = "camera_0",
                onFallbackToAudio = { fallbackCalled = true },
            )

            val currentError = errorHandler.currentError.value
            assertNotNull(currentError)
            assertTrue(currentError is ChatRtError.CameraError)
            assertEquals(CameraErrorCause.PERMISSION_DENIED, currentError.cause)
            assertTrue(fallbackCalled)
        }

    @Test
    fun testScreenCaptureErrorWithFallback() =
        runTest {
            // Test screen capture error with fallback to camera mode (Requirement 3.6)
            var fallbackCalled = false

            errorHandler.handleScreenCaptureError(
                cause = ScreenCaptureErrorCause.PERMISSION_DENIED,
                details = "Permission denied",
                onFallbackToCamera = { fallbackCalled = true },
            )

            val currentError = errorHandler.currentError.value
            assertNotNull(currentError)
            assertTrue(currentError is ChatRtError.ScreenCaptureError)
            assertEquals(ScreenCaptureErrorCause.PERMISSION_DENIED, currentError.cause)
            assertTrue(fallbackCalled)
        }

    @Test
    fun testAudioDeviceErrorHandling() =
        runTest {
            // Test audio device error handling (Requirement 5.3)
            var deviceSwitchCalled = false

            errorHandler.handleAudioDeviceError(
                cause = AudioErrorCause.ROUTING_FAILED,
                deviceName = "Bluetooth Headset",
                onDeviceSwitch = { deviceSwitchCalled = true },
            )

            val currentError = errorHandler.currentError.value
            assertNotNull(currentError)
            assertTrue(currentError is ChatRtError.AudioDeviceError)
            assertEquals(AudioErrorCause.ROUTING_FAILED, currentError.cause)
            assertTrue(deviceSwitchCalled)
        }

    @Test
    fun testPhoneCallInterruptionHandling() =
        runTest {
            // Test phone call interruption handling (Requirement 5.2)
            var pauseCalled = false
            var resumeCalled = false

            // Test incoming call
            errorHandler.handlePhoneCallInterruption(
                callState = PhoneCallState.INCOMING,
                onPause = { pauseCalled = true },
                onResume = { resumeCalled = true },
            )

            assertTrue(pauseCalled)
            assertFalse(resumeCalled)

            // Reset flags
            pauseCalled = false
            resumeCalled = false

            // Test call ended
            errorHandler.handlePhoneCallInterruption(
                callState = PhoneCallState.ENDED,
                onPause = { pauseCalled = true },
                onResume = { resumeCalled = true },
            )

            assertFalse(pauseCalled)
            assertTrue(resumeCalled)
        }

    @Test
    fun testDeviceStateChangeHandling() =
        runTest {
            // Test device state changes with UI feedback (Requirement 5.3)
            var audioRouteChangeCalled = false

            errorHandler.handleDeviceStateChange(
                stateChange = DeviceStateChange.HEADPHONES_CONNECTED,
                details = "Wired headphones connected",
                onAudioRouteChange = { audioRouteChangeCalled = true },
            )

            val currentError = errorHandler.currentError.value
            assertNotNull(currentError)
            assertTrue(currentError is ChatRtError.DeviceStateError)
            assertEquals(DeviceStateChange.HEADPHONES_CONNECTED, currentError.stateChange)
            assertTrue(audioRouteChangeCalled)
        }

    @Test
    fun testRetryMechanism() =
        runTest {
            // Test retry mechanism with retry limits
            val retryableError = ChatRtError.NetworkError(NetworkErrorCause.TIMEOUT, "Connection timeout")
            errorHandler.handleError(retryableError, "Test")

            // Should be able to retry initially
            assertTrue(errorHandler.canRetry(retryableError))

            // Simulate multiple retry attempts
            repeat(3) {
                errorHandler.retryCurrentError { }
            }

            // Should not be able to retry after max attempts
            assertFalse(errorHandler.canRetry(retryableError))
        }

    @Test
    fun testErrorHistory() =
        runTest {
            // Test error history tracking
            val error1 = ChatRtError.NetworkError(NetworkErrorCause.NO_INTERNET)
            val error2 = ChatRtError.CameraError(CameraErrorCause.CAMERA_BUSY)

            errorHandler.handleError(error1, "Network Test")
            errorHandler.handleError(error2, "Camera Test")

            val history = errorHandler.errorHistory.value
            assertEquals(2, history.size)
            assertEquals(error1.errorCode, history[0].error.errorCode)
            assertEquals(error2.errorCode, history[1].error.errorCode)
            assertEquals("Network Test", history[0].context)
            assertEquals("Camera Test", history[1].context)
        }

    @Test
    fun testExceptionMapping() =
        runTest {
            // Test exception mapping to ChatRtError types
            val networkException = Exception("Network connection failed")
            val permissionException = Exception("Permission denied for camera")
            val webrtcException = Exception("WebRTC peer connection failed")

            val networkError = errorHandler.mapExceptionToChatRtError(networkException, "Network")
            assertTrue(networkError is ChatRtError.NetworkError)

            val permissionError = errorHandler.mapExceptionToChatRtError(permissionException, "Permission")
            assertTrue(permissionError is ChatRtError.PermissionDenied)

            val webrtcError = errorHandler.mapExceptionToChatRtError(webrtcException, "WebRTC")
            assertTrue(webrtcError is ChatRtError.WebRtcError)
        }

    @Test
    fun testRecoverySuggestions() =
        runTest {
            // Test recovery suggestions generation
            val networkError = ChatRtError.NetworkError(NetworkErrorCause.NO_INTERNET)
            errorHandler.handleError(networkError, "Network")

            val suggestions = errorHandler.getRecoverySuggestions(networkError, "Network")
            assertTrue(suggestions.isNotEmpty())

            // Should include error-specific suggestions
            assertTrue(suggestions.any { it.message.contains("WiFi", ignoreCase = true) })

            // Should include retry suggestion for retryable errors
            assertTrue(suggestions.any { it.type == RecoverySuggestionType.RETRY })
        }

    @Test
    fun testErrorClearing() =
        runTest {
            // Test error clearing functionality
            val error = ChatRtError.NetworkError(NetworkErrorCause.TIMEOUT)
            errorHandler.handleError(error, "Test")

            assertNotNull(errorHandler.currentError.value)

            errorHandler.clearCurrentError()
            assertNull(errorHandler.currentError.value)
        }

    @Test
    fun testBatteryOptimizationHandling() =
        runTest {
            // Test battery optimization error handling
            var optimizeCalled = false

            errorHandler.handleBatteryOptimization(
                cause = BatteryErrorCause.LOW_BATTERY,
                batteryLevel = 15,
                onOptimize = { optimizeCalled = true },
            )

            val currentError = errorHandler.currentError.value
            assertNotNull(currentError)
            assertTrue(currentError is ChatRtError.BatteryOptimizationError)
            assertEquals(BatteryErrorCause.LOW_BATTERY, currentError.cause)
            assertEquals(15, currentError.batteryLevel)
            assertTrue(optimizeCalled)
        }

    @Test
    fun testNetworkQualityHandling() =
        runTest {
            // Test network quality error handling
            var qualityReductionCalled = false

            errorHandler.handleNetworkQuality(
                currentQuality = NetworkQuality.POOR,
                minimumRequired = NetworkQuality.FAIR,
                onQualityReduction = { qualityReductionCalled = true },
            )

            val currentError = errorHandler.currentError.value
            assertNotNull(currentError)
            assertTrue(currentError is ChatRtError.NetworkQualityError)
            assertEquals(NetworkQuality.POOR, currentError.currentQuality)
            assertTrue(qualityReductionCalled)
        }

    @Test
    fun testApiErrorHandling() =
        runTest {
            // Test API error handling with retry logic
            errorHandler.handleApiError(
                code = 429,
                message = "Too many requests",
                endpoint = "/rtc",
            )

            val currentError = errorHandler.currentError.value
            assertNotNull(currentError)
            assertTrue(currentError is ChatRtError.ApiError)
            assertEquals(429, currentError.code)
            assertEquals("Too many requests - please wait and try again", currentError.userMessage)
            assertTrue(currentError.isRetryable)
        }
}
