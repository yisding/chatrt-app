package ai.chatrt.app.models

import kotlinx.serialization.Serializable

/**
 * Sealed class hierarchy for ChatRT error types with comprehensive error handling
 * Requirements: 1.6, 2.6, 3.5, 4.3, 5.3
 */
sealed class ChatRtError : Exception() {
    abstract val errorCode: String
    abstract val userMessage: String
    abstract val technicalMessage: String
    abstract val isRetryable: Boolean
    abstract val suggestions: List<String>

    /**
     * Network-related errors
     * Requirement: 4.3 - Connection issues with appropriate error messages
     */
    data class NetworkError(
        val errorCause: NetworkErrorCause = NetworkErrorCause.UNKNOWN,
        val details: String? = null,
    ) : ChatRtError() {
        override val errorCode: String = "NETWORK_ERROR"
        override val userMessage: String =
            when (errorCause) {
                NetworkErrorCause.NO_INTERNET -> "No internet connection available"
                NetworkErrorCause.SERVER_UNREACHABLE -> "Unable to reach the ChatRT server"
                NetworkErrorCause.TIMEOUT -> "Connection timed out"
                NetworkErrorCause.DNS_FAILURE -> "Unable to resolve server address"
                NetworkErrorCause.SSL_ERROR -> "Secure connection failed"
                NetworkErrorCause.UNKNOWN -> "Network connection error"
            }
        override val technicalMessage: String = details ?: "Network error: $errorCause"
        override val isRetryable: Boolean = true
        override val suggestions: List<String> =
            when (errorCause) {
                NetworkErrorCause.NO_INTERNET ->
                    listOf(
                        "Check your WiFi or mobile data connection",
                        "Try switching between WiFi and mobile data",
                        "Move closer to your router if using WiFi",
                    )
                NetworkErrorCause.SERVER_UNREACHABLE ->
                    listOf(
                        "Check if the server URL is correct in settings",
                        "Try again in a few moments",
                        "Contact support if the problem persists",
                    )
                NetworkErrorCause.TIMEOUT ->
                    listOf(
                        "Check your internet connection speed",
                        "Try switching to a different network",
                        "Retry the connection",
                    )
                else ->
                    listOf(
                        "Check your internet connection",
                        "Try again in a few moments",
                        "Restart the app if the problem persists",
                    )
            }
    }

    /**
     * Permission denied errors
     * Requirements: 1.2, 2.6, 3.6 - Permission handling with fallback options
     */
    data class PermissionDenied(
        val permission: PermissionType,
        val isPermanentlyDenied: Boolean = false,
    ) : ChatRtError() {
        override val errorCode: String = "PERMISSION_DENIED"
        override val userMessage: String =
            when (permission) {
                PermissionType.MICROPHONE -> "Microphone permission is required for voice chat"
                PermissionType.CAMERA -> "Camera permission is required for video chat"
                PermissionType.SCREEN_CAPTURE -> "Screen recording permission is required for screen sharing"
                PermissionType.NOTIFICATION -> "Notification permission is required for call alerts"
            }
        override val technicalMessage: String = "Permission denied: $permission (permanent: $isPermanentlyDenied)"
        override val isRetryable: Boolean = !isPermanentlyDenied
        override val suggestions: List<String> =
            if (isPermanentlyDenied) {
                listOf(
                    "Go to Settings > Apps > ChatRT > Permissions",
                    "Enable ${permission.displayName} permission",
                    "Restart the app after granting permissions",
                )
            } else {
                listOf(
                    "Tap 'Allow' when prompted for ${permission.displayName} permission",
                    if (permission == PermissionType.CAMERA) "Switch to audio-only mode if camera is not needed" else null,
                ).filterNotNull()
            }
    }

    /**
     * WebRTC connection errors
     * Requirement: 4.3 - WebRTC connection issues with suggested actions
     */
    data class WebRtcError(
        val errorCause: WebRtcErrorCause = WebRtcErrorCause.UNKNOWN,
        val details: String? = null,
    ) : ChatRtError() {
        override val errorCode: String = "WEBRTC_ERROR"
        override val userMessage: String =
            when (errorCause) {
                WebRtcErrorCause.PEER_CONNECTION_FAILED -> "Failed to establish call connection"
                WebRtcErrorCause.ICE_CONNECTION_FAILED -> "Network connection failed"
                WebRtcErrorCause.MEDIA_STREAM_FAILED -> "Failed to access media devices"
                WebRtcErrorCause.SDP_ERROR -> "Call setup failed"
                WebRtcErrorCause.UNKNOWN -> "Call connection error"
            }
        override val technicalMessage: String = details ?: "WebRTC error: $errorCause"
        override val isRetryable: Boolean = true
        override val suggestions: List<String> =
            when (errorCause) {
                WebRtcErrorCause.ICE_CONNECTION_FAILED ->
                    listOf(
                        "Check your internet connection",
                        "Try switching networks",
                        "Disable VPN if active",
                    )
                WebRtcErrorCause.MEDIA_STREAM_FAILED ->
                    listOf(
                        "Check camera and microphone permissions",
                        "Close other apps using camera/microphone",
                        "Try switching to audio-only mode",
                    )
                else ->
                    listOf(
                        "Check your internet connection",
                        "Try switching to audio-only mode",
                        "Restart the app if the problem persists",
                    )
            }
    }

    /**
     * Audio device errors
     * Requirement: 5.3 - Audio routing and device management
     */
    data class AudioDeviceError(
        val errorCause: AudioErrorCause = AudioErrorCause.UNKNOWN,
        val deviceName: String? = null,
    ) : ChatRtError() {
        override val errorCode: String = "AUDIO_DEVICE_ERROR"
        override val userMessage: String =
            when (errorCause) {
                AudioErrorCause.DEVICE_UNAVAILABLE -> "Audio device is not available"
                AudioErrorCause.PERMISSION_DENIED -> "Microphone permission is required"
                AudioErrorCause.DEVICE_BUSY -> "Audio device is being used by another app"
                AudioErrorCause.ROUTING_FAILED -> "Failed to route audio to selected device"
                AudioErrorCause.FOCUS_LOST -> "Audio focus was lost to another app"
                AudioErrorCause.UNKNOWN -> "Audio device error"
            }
        override val technicalMessage: String = "Audio error: $errorCause${deviceName?.let { " (device: $it)" } ?: ""}"
        override val isRetryable: Boolean = errorCause != AudioErrorCause.PERMISSION_DENIED
        override val suggestions: List<String> =
            when (errorCause) {
                AudioErrorCause.DEVICE_BUSY ->
                    listOf(
                        "Close other apps using the microphone",
                        "Try unplugging and reconnecting headphones",
                        "Restart the app to reset audio settings",
                    )
                AudioErrorCause.ROUTING_FAILED ->
                    listOf(
                        "Try switching to a different audio device",
                        "Check headphone connection",
                        "Restart the app if the problem persists",
                    )
                AudioErrorCause.FOCUS_LOST ->
                    listOf(
                        "Close other audio apps",
                        "Try starting the call again",
                        "Check notification settings",
                    )
                else ->
                    listOf(
                        "Check microphone permissions",
                        "Try unplugging and reconnecting headphones",
                        "Restart the app to reset audio settings",
                    )
            }
    }

    /**
     * Camera-related errors
     * Requirement: 2.6 - Camera permission denied with fallback to audio-only
     */
    data class CameraError(
        val errorCause: CameraErrorCause = CameraErrorCause.UNKNOWN,
        val cameraId: String? = null,
    ) : ChatRtError() {
        override val errorCode: String = "CAMERA_ERROR"
        override val userMessage: String =
            when (errorCause) {
                CameraErrorCause.PERMISSION_DENIED -> "Camera permission is required for video chat"
                CameraErrorCause.CAMERA_UNAVAILABLE -> "Camera is not available"
                CameraErrorCause.CAMERA_BUSY -> "Camera is being used by another app"
                CameraErrorCause.CAMERA_DISCONNECTED -> "Camera was disconnected"
                CameraErrorCause.UNKNOWN -> "Camera access error"
            }
        override val technicalMessage: String = "Camera error: $errorCause${cameraId?.let { " (camera: $it)" } ?: ""}"
        override val isRetryable: Boolean = errorCause != CameraErrorCause.PERMISSION_DENIED
        override val suggestions: List<String> =
            when (errorCause) {
                CameraErrorCause.PERMISSION_DENIED ->
                    listOf(
                        "Grant camera permission in settings",
                        "Switch to audio-only mode if camera is not needed",
                    )
                CameraErrorCause.CAMERA_BUSY ->
                    listOf(
                        "Close other apps using the camera",
                        "Switch to audio-only mode",
                        "Try switching between front and back camera",
                    )
                else ->
                    listOf(
                        "Check if another app is using the camera",
                        "Switch to audio-only mode",
                        "Restart the app to reset camera settings",
                    )
            }
    }

    /**
     * Screen capture errors
     * Requirement: 3.6 - Screen sharing permission denied with alternative modes
     */
    data class ScreenCaptureError(
        val errorCause: ScreenCaptureErrorCause = ScreenCaptureErrorCause.UNKNOWN,
        val details: String? = null,
    ) : ChatRtError() {
        override val errorCode: String = "SCREEN_CAPTURE_ERROR"
        override val userMessage: String =
            when (errorCause) {
                ScreenCaptureErrorCause.PERMISSION_DENIED -> "Screen recording permission is required"
                ScreenCaptureErrorCause.MEDIA_PROJECTION_FAILED -> "Failed to start screen recording"
                ScreenCaptureErrorCause.DISPLAY_UNAVAILABLE -> "Display is not available for recording"
                ScreenCaptureErrorCause.SECURITY_RESTRICTED -> "Screen recording is restricted by security policy"
                ScreenCaptureErrorCause.UNKNOWN -> "Screen capture error"
            }
        override val technicalMessage: String = details ?: "Screen capture error: $errorCause"
        override val isRetryable: Boolean = errorCause != ScreenCaptureErrorCause.SECURITY_RESTRICTED
        override val suggestions: List<String> =
            when (errorCause) {
                ScreenCaptureErrorCause.PERMISSION_DENIED ->
                    listOf(
                        "Grant screen recording permission when prompted",
                        "Try switching to camera mode instead",
                        "Check if screen recording is restricted by your device",
                    )
                ScreenCaptureErrorCause.SECURITY_RESTRICTED ->
                    listOf(
                        "Screen recording is disabled by your device administrator",
                        "Try using camera mode instead",
                        "Contact your IT administrator for assistance",
                    )
                else ->
                    listOf(
                        "Try restarting the screen share",
                        "Switch to camera mode instead",
                        "Restart the app if the problem persists",
                    )
            }
    }

    /**
     * Service connection errors
     */
    data class ServiceConnectionError(
        val serviceName: String,
        val reason: String? = null,
    ) : ChatRtError() {
        override val errorCode: String = "SERVICE_CONNECTION_ERROR"
        override val userMessage: String = "Unable to connect to $serviceName service"
        override val technicalMessage: String = reason ?: "Service connection failed: $serviceName"
        override val isRetryable: Boolean = true
        override val suggestions: List<String> =
            listOf(
                "Try restarting the app",
                "Check your internet connection",
                "Contact support if the problem persists",
            )
    }

    /**
     * Phone call interruption errors
     * Requirement: 5.2 - Phone call interruption handling
     */
    data class PhoneCallInterruptionError(
        val callState: PhoneCallState,
        val details: String? = null,
    ) : ChatRtError() {
        override val errorCode: String = "PHONE_CALL_INTERRUPTION"
        override val userMessage: String =
            when (callState) {
                PhoneCallState.INCOMING -> "ChatRT call paused due to incoming phone call"
                PhoneCallState.ACTIVE -> "ChatRT call paused during phone call"
                PhoneCallState.ENDED -> "Resuming ChatRT call after phone call ended"
            }
        override val technicalMessage: String = details ?: "Phone call interruption: $callState"
        override val isRetryable: Boolean = callState == PhoneCallState.ENDED
        override val suggestions: List<String> =
            when (callState) {
                PhoneCallState.ENDED ->
                    listOf(
                        "ChatRT call will resume automatically",
                        "Tap 'Resume' if the call doesn't resume automatically",
                    )
                else ->
                    listOf(
                        "ChatRT call will pause during phone call",
                        "Call will resume after phone call ends",
                    )
            }
    }

    /**
     * Battery optimization errors
     */
    data class BatteryOptimizationError(
        val errorCause: BatteryErrorCause = BatteryErrorCause.UNKNOWN,
        val batteryLevel: Int? = null,
    ) : ChatRtError() {
        override val errorCode: String = "BATTERY_OPTIMIZATION_ERROR"
        override val userMessage: String =
            when (errorCause) {
                BatteryErrorCause.LOW_BATTERY -> "Low battery may affect call quality"
                BatteryErrorCause.POWER_SAVE_MODE -> "Power saving mode is affecting performance"
                BatteryErrorCause.BACKGROUND_RESTRICTED -> "Background activity is restricted"
                BatteryErrorCause.UNKNOWN -> "Battery optimization is affecting performance"
            }
        override val technicalMessage: String = "Battery error: $errorCause${batteryLevel?.let { " (level: $it%)" } ?: ""}"
        override val isRetryable: Boolean = false
        override val suggestions: List<String> =
            when (errorCause) {
                BatteryErrorCause.LOW_BATTERY ->
                    listOf(
                        "Connect to a charger for better performance",
                        "Switch to audio-only mode to save battery",
                        "Close other apps to conserve battery",
                    )
                BatteryErrorCause.POWER_SAVE_MODE ->
                    listOf(
                        "Disable power saving mode for better performance",
                        "Add ChatRT to battery optimization whitelist",
                        "Switch to audio-only mode",
                    )
                BatteryErrorCause.BACKGROUND_RESTRICTED ->
                    listOf(
                        "Allow ChatRT to run in background",
                        "Disable battery optimization for ChatRT",
                        "Check app permissions in settings",
                    )
                else ->
                    listOf(
                        "Check battery optimization settings",
                        "Allow ChatRT to run in background",
                        "Switch to audio-only mode to save battery",
                    )
            }
    }

    /**
     * Network quality errors
     */
    data class NetworkQualityError(
        val currentQuality: NetworkQuality,
        val minimumRequired: NetworkQuality = NetworkQuality.FAIR,
    ) : ChatRtError() {
        override val errorCode: String = "NETWORK_QUALITY_ERROR"
        override val userMessage: String = "Poor network quality is affecting the call"
        override val technicalMessage: String = "Network quality: $currentQuality (minimum required: $minimumRequired)"
        override val isRetryable: Boolean = true
        override val suggestions: List<String> =
            listOf(
                "Switch to audio-only mode for better performance",
                "Move closer to your WiFi router",
                "Close other apps using internet",
                "Try switching between WiFi and mobile data",
            )
    }

    /**
     * API errors with specific code and message
     */
    @Serializable
    data class ApiError(
        val code: Int,
        override val message: String,
        val endpoint: String? = null,
    ) : ChatRtError() {
        override val errorCode: String = "API_ERROR_$code"
        override val userMessage: String =
            when (code) {
                400 -> "Invalid request - please check your settings"
                401 -> "Authentication failed - please check your API key"
                403 -> "Access denied - insufficient permissions"
                404 -> "Service not found - please check server URL"
                429 -> "Too many requests - please wait and try again"
                500 -> "Server error - please try again later"
                503 -> "Service unavailable - please try again later"
                else -> message
            }
        override val technicalMessage: String = "API error $code: $message${endpoint?.let { " (endpoint: $it)" } ?: ""}"
        override val isRetryable: Boolean = code in listOf(429, 500, 502, 503, 504)
        override val suggestions: List<String> =
            when (code) {
                401 ->
                    listOf(
                        "Check your API key in settings",
                        "Ensure your API key is valid and active",
                    )
                404 ->
                    listOf(
                        "Check the server URL in settings",
                        "Ensure the ChatRT backend is running",
                    )
                429 ->
                    listOf(
                        "Wait a few moments before trying again",
                        "Reduce the frequency of requests",
                    )
                500, 502, 503, 504 ->
                    listOf(
                        "Try again in a few moments",
                        "Check if the server is experiencing issues",
                        "Contact support if the problem persists",
                    )
                else ->
                    listOf(
                        "Check your settings and try again",
                        "Contact support if the problem persists",
                    )
            }
    }

    /**
     * Device state change errors
     * Requirement: 5.3 - Device state changes with appropriate UI feedback
     */
    data class DeviceStateError(
        val stateChange: DeviceStateChange,
        val details: String? = null,
    ) : ChatRtError() {
        override val errorCode: String = "DEVICE_STATE_ERROR"
        override val userMessage: String =
            when (stateChange) {
                DeviceStateChange.ORIENTATION_CHANGED -> "Screen orientation changed"
                DeviceStateChange.HEADPHONES_DISCONNECTED -> "Headphones disconnected"
                DeviceStateChange.HEADPHONES_CONNECTED -> "Headphones connected"
                DeviceStateChange.BLUETOOTH_DISCONNECTED -> "Bluetooth device disconnected"
                DeviceStateChange.BLUETOOTH_CONNECTED -> "Bluetooth device connected"
                DeviceStateChange.NETWORK_CHANGED -> "Network connection changed"
            }
        override val technicalMessage: String = details ?: "Device state changed: $stateChange"
        override val isRetryable: Boolean = false
        override val suggestions: List<String> =
            when (stateChange) {
                DeviceStateChange.HEADPHONES_DISCONNECTED ->
                    listOf(
                        "Audio switched to speaker",
                        "Reconnect headphones to switch back",
                    )
                DeviceStateChange.HEADPHONES_CONNECTED ->
                    listOf(
                        "Audio switched to headphones",
                        "Check audio quality settings",
                    )
                DeviceStateChange.BLUETOOTH_DISCONNECTED ->
                    listOf(
                        "Audio switched to phone speaker",
                        "Reconnect Bluetooth device if needed",
                    )
                DeviceStateChange.BLUETOOTH_CONNECTED ->
                    listOf(
                        "Audio switched to Bluetooth device",
                        "Check audio quality and volume",
                    )
                DeviceStateChange.NETWORK_CHANGED ->
                    listOf(
                        "Connection may be interrupted briefly",
                        "Call will reconnect automatically",
                    )
                else -> emptyList()
            }
    }
}

// Supporting enums for error categorization

enum class NetworkErrorCause {
    NO_INTERNET,
    SERVER_UNREACHABLE,
    TIMEOUT,
    DNS_FAILURE,
    SSL_ERROR,
    UNKNOWN,
}

enum class PermissionType(
    val displayName: String,
) {
    MICROPHONE("Microphone"),
    CAMERA("Camera"),
    SCREEN_CAPTURE("Screen Recording"),
    NOTIFICATION("Notifications"),
}

enum class WebRtcErrorCause {
    PEER_CONNECTION_FAILED,
    ICE_CONNECTION_FAILED,
    MEDIA_STREAM_FAILED,
    SDP_ERROR,
    UNKNOWN,
}

enum class AudioErrorCause {
    DEVICE_UNAVAILABLE,
    PERMISSION_DENIED,
    DEVICE_BUSY,
    ROUTING_FAILED,
    FOCUS_LOST,
    UNKNOWN,
}

enum class CameraErrorCause {
    PERMISSION_DENIED,
    CAMERA_UNAVAILABLE,
    CAMERA_BUSY,
    CAMERA_DISCONNECTED,
    UNKNOWN,
}

enum class ScreenCaptureErrorCause {
    PERMISSION_DENIED,
    MEDIA_PROJECTION_FAILED,
    DISPLAY_UNAVAILABLE,
    SECURITY_RESTRICTED,
    UNKNOWN,
}

enum class PhoneCallState {
    INCOMING,
    ACTIVE,
    ENDED,
}

enum class BatteryErrorCause {
    LOW_BATTERY,
    POWER_SAVE_MODE,
    BACKGROUND_RESTRICTED,
    UNKNOWN,
}

enum class DeviceStateChange {
    ORIENTATION_CHANGED,
    HEADPHONES_DISCONNECTED,
    HEADPHONES_CONNECTED,
    BLUETOOTH_DISCONNECTED,
    BLUETOOTH_CONNECTED,
    NETWORK_CHANGED,
}
