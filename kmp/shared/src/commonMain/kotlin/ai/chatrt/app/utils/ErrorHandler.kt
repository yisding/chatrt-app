package ai.chatrt.app.utils

import ai.chatrt.app.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Comprehensive error handling utility for ChatRT application
 * Provides centralized error management, retry mechanisms, and recovery suggestions
 * Requirements: 1.6, 2.6, 3.5, 4.3, 5.3
 */
class ErrorHandler {
    private val _currentError = MutableStateFlow<ChatRtError?>(null)
    val currentError: StateFlow<ChatRtError?> = _currentError.asStateFlow()

    private val _errorHistory = MutableStateFlow<List<ErrorHistoryEntry>>(emptyList())
    val errorHistory: StateFlow<List<ErrorHistoryEntry>> = _errorHistory.asStateFlow()

    private val retryAttempts = mutableMapOf<String, Int>()
    private val maxRetryAttempts = 3

    /**
     * Handles an error with automatic retry logic and user-friendly messaging
     */
    fun handleError(
        error: ChatRtError,
        context: String = "",
        onRetry: (() -> Unit)? = null,
    ) {
        // Add to error history
        addToHistory(error, context)

        // Set current error for UI display
        _currentError.value = error

        // Handle automatic retry for retryable errors
        if (error.isRetryable && onRetry != null) {
            val attempts = retryAttempts.getOrDefault(error.errorCode, 0)
            if (attempts < maxRetryAttempts) {
                retryAttempts[error.errorCode] = attempts + 1
                // Could implement automatic retry with exponential backoff here
                // For now, just track attempts and let UI handle retry
            }
        }
    }

    /**
     * Handles network errors with specific network diagnostics
     * Requirement: 4.3 - Connection issues with appropriate error messages
     */
    fun handleNetworkError(
        cause: NetworkErrorCause,
        details: String? = null,
        onRetry: (() -> Unit)? = null,
    ) {
        val error = ChatRtError.NetworkError(cause, details)
        handleError(error, "Network", onRetry)
    }

    /**
     * Handles permission errors with fallback suggestions
     * Requirements: 1.2, 2.6, 3.6 - Permission handling with fallback options
     */
    fun handlePermissionError(
        permission: PermissionType,
        isPermanentlyDenied: Boolean = false,
        onFallback: (() -> Unit)? = null,
    ) {
        val error = ChatRtError.PermissionDenied(permission, isPermanentlyDenied)
        handleError(error, "Permission")

        // Suggest fallback actions
        when (permission) {
            PermissionType.CAMERA -> {
                // Suggest switching to audio-only mode (Requirement 2.6)
                onFallback?.invoke()
            }
            PermissionType.SCREEN_CAPTURE -> {
                // Suggest alternative modes (Requirement 3.6)
                onFallback?.invoke()
            }
            else -> {
                // Handle other permission types
            }
        }
    }

    /**
     * Handles WebRTC connection errors with connection diagnostics
     * Requirement: 4.3 - WebRTC connection issues with suggested actions
     */
    fun handleWebRtcError(
        cause: WebRtcErrorCause,
        details: String? = null,
        onRetry: (() -> Unit)? = null,
    ) {
        val error = ChatRtError.WebRtcError(cause, details)
        handleError(error, "WebRTC", onRetry)
    }

    /**
     * Handles audio device errors with routing suggestions
     * Requirement: 5.3 - Audio routing and device management
     */
    fun handleAudioDeviceError(
        cause: AudioErrorCause,
        deviceName: String? = null,
        onDeviceSwitch: (() -> Unit)? = null,
    ) {
        val error = ChatRtError.AudioDeviceError(cause, deviceName)
        handleError(error, "Audio Device")

        // Suggest device switching for routing failures
        if (cause == AudioErrorCause.ROUTING_FAILED) {
            onDeviceSwitch?.invoke()
        }
    }

    /**
     * Handles camera errors with fallback to audio-only
     * Requirement: 2.6 - Camera permission denied with fallback to audio-only
     */
    fun handleCameraError(
        cause: CameraErrorCause,
        cameraId: String? = null,
        onFallbackToAudio: (() -> Unit)? = null,
    ) {
        val error = ChatRtError.CameraError(cause, cameraId)
        handleError(error, "Camera")

        // Automatically suggest fallback to audio-only mode
        if (cause in listOf(CameraErrorCause.PERMISSION_DENIED, CameraErrorCause.CAMERA_BUSY)) {
            onFallbackToAudio?.invoke()
        }
    }

    /**
     * Handles screen capture errors with alternative mode suggestions
     * Requirement: 3.6 - Screen sharing permission denied with alternative modes
     */
    fun handleScreenCaptureError(
        cause: ScreenCaptureErrorCause,
        details: String? = null,
        onFallbackToCamera: (() -> Unit)? = null,
    ) {
        val error = ChatRtError.ScreenCaptureError(cause, details)
        handleError(error, "Screen Capture")

        // Suggest fallback to camera mode
        if (cause in listOf(ScreenCaptureErrorCause.PERMISSION_DENIED, ScreenCaptureErrorCause.SECURITY_RESTRICTED)) {
            onFallbackToCamera?.invoke()
        }
    }

    /**
     * Handles phone call interruptions
     * Requirement: 5.2 - Phone call interruption handling
     */
    fun handlePhoneCallInterruption(
        callState: PhoneCallState,
        details: String? = null,
        onPause: (() -> Unit)? = null,
        onResume: (() -> Unit)? = null,
    ) {
        val error = ChatRtError.PhoneCallInterruptionError(callState, details)
        handleError(error, "Phone Call Interruption")

        when (callState) {
            PhoneCallState.INCOMING, PhoneCallState.ACTIVE -> onPause?.invoke()
            PhoneCallState.ENDED -> onResume?.invoke()
        }
    }

    /**
     * Handles device state changes with appropriate UI feedback
     * Requirement: 5.3 - Device state changes with appropriate UI feedback
     */
    fun handleDeviceStateChange(
        stateChange: DeviceStateChange,
        details: String? = null,
        onAudioRouteChange: ((AudioDevice?) -> Unit)? = null,
    ) {
        val error = ChatRtError.DeviceStateError(stateChange, details)
        handleError(error, "Device State Change")

        // Handle audio routing changes
        when (stateChange) {
            DeviceStateChange.HEADPHONES_CONNECTED,
            DeviceStateChange.HEADPHONES_DISCONNECTED,
            DeviceStateChange.BLUETOOTH_CONNECTED,
            DeviceStateChange.BLUETOOTH_DISCONNECTED,
            -> {
                onAudioRouteChange?.invoke(null) // Let the audio manager determine the best route
            }
            else -> {
                // Handle other state changes
            }
        }
    }

    /**
     * Handles battery optimization issues
     */
    fun handleBatteryOptimization(
        cause: BatteryErrorCause,
        batteryLevel: Int? = null,
        onOptimize: (() -> Unit)? = null,
    ) {
        val error = ChatRtError.BatteryOptimizationError(cause, batteryLevel)
        handleError(error, "Battery Optimization")

        // Suggest optimization
        onOptimize?.invoke()
    }

    /**
     * Handles network quality issues
     */
    fun handleNetworkQuality(
        currentQuality: NetworkQuality,
        minimumRequired: NetworkQuality = NetworkQuality.FAIR,
        onQualityReduction: (() -> Unit)? = null,
    ) {
        if (currentQuality < minimumRequired) {
            val error = ChatRtError.NetworkQualityError(currentQuality, minimumRequired)
            handleError(error, "Network Quality")

            // Suggest quality reduction
            onQualityReduction?.invoke()
        }
    }

    /**
     * Handles API errors with retry logic
     */
    fun handleApiError(
        code: Int,
        message: String,
        endpoint: String? = null,
        onRetry: (() -> Unit)? = null,
    ) {
        val error = ChatRtError.ApiError(code, message, endpoint)
        handleError(error, "API", onRetry)
    }

    /**
     * Retries the current error if it's retryable
     */
    fun retryCurrentError(onRetry: () -> Unit): Boolean {
        val error = _currentError.value
        return if (error?.isRetryable == true) {
            val attempts = retryAttempts.getOrDefault(error.errorCode, 0)
            if (attempts < maxRetryAttempts) {
                retryAttempts[error.errorCode] = attempts + 1
                onRetry()
                true
            } else {
                false // Max retries reached
            }
        } else {
            false // Not retryable
        }
    }

    /**
     * Clears the current error
     */
    fun clearCurrentError() {
        _currentError.value = null
    }

    /**
     * Clears retry attempts for a specific error code
     */
    fun clearRetryAttempts(errorCode: String) {
        retryAttempts.remove(errorCode)
    }

    /**
     * Gets retry attempts for a specific error code
     */
    fun getRetryAttempts(errorCode: String): Int = retryAttempts.getOrDefault(errorCode, 0)

    /**
     * Checks if an error can be retried
     */
    fun canRetry(error: ChatRtError): Boolean {
        val attempts = retryAttempts.getOrDefault(error.errorCode, 0)
        return error.isRetryable && attempts < maxRetryAttempts
    }

    /**
     * Gets error recovery suggestions based on error type and context
     */
    fun getRecoverySuggestions(
        error: ChatRtError,
        context: String = "",
    ): List<RecoverySuggestion> {
        val suggestions = mutableListOf<RecoverySuggestion>()

        // Add error-specific suggestions
        error.suggestions.forEach { suggestion ->
            suggestions.add(RecoverySuggestion(suggestion, RecoverySuggestionType.USER_ACTION))
        }

        // Add context-specific suggestions
        when (context) {
            "Connection" -> {
                suggestions.add(RecoverySuggestion("Check network settings", RecoverySuggestionType.SETTINGS))
            }
            "Permission" -> {
                suggestions.add(RecoverySuggestion("Open app settings", RecoverySuggestionType.SETTINGS))
            }
            "Audio Device" -> {
                suggestions.add(RecoverySuggestion("Switch audio device", RecoverySuggestionType.ACTION))
            }
        }

        // Add retry suggestion if applicable
        if (canRetry(error)) {
            suggestions.add(RecoverySuggestion("Retry operation", RecoverySuggestionType.RETRY))
        }

        return suggestions
    }

    /**
     * Adds an error to the history
     */
    private fun addToHistory(
        error: ChatRtError,
        context: String,
    ) {
        val entry =
            ErrorHistoryEntry(
                error = error,
                context = context,
                timestamp =
                    kotlinx.datetime.Clock.System
                        .now()
                        .toEpochMilliseconds(),
                retryAttempts = retryAttempts.getOrDefault(error.errorCode, 0),
            )

        val currentHistory = _errorHistory.value.toMutableList()
        currentHistory.add(entry)

        // Keep only the last 50 errors
        if (currentHistory.size > 50) {
            currentHistory.removeAt(0)
        }

        _errorHistory.value = currentHistory
    }

    /**
     * Maps generic exceptions to ChatRtError types
     */
    fun mapExceptionToChatRtError(
        exception: Throwable,
        context: String = "",
    ): ChatRtError =
        when {
            exception.message?.contains("network", ignoreCase = true) == true ||
                exception.message?.contains("connection", ignoreCase = true) == true ||
                exception.message?.contains("host", ignoreCase = true) == true -> {
                ChatRtError.NetworkError(NetworkErrorCause.UNKNOWN, exception.message)
            }
            exception.message?.contains("permission", ignoreCase = true) == true ||
                exception.message?.contains("security", ignoreCase = true) == true -> {
                ChatRtError.PermissionDenied(PermissionType.MICROPHONE, false) // Default to microphone
            }
            exception.message?.contains("webrtc", ignoreCase = true) == true ||
                exception.message?.contains("peer", ignoreCase = true) == true -> {
                ChatRtError.WebRtcError(WebRtcErrorCause.UNKNOWN, exception.message)
            }
            exception.message?.contains("audio", ignoreCase = true) == true ||
                exception.message?.contains("microphone", ignoreCase = true) == true -> {
                ChatRtError.AudioDeviceError(AudioErrorCause.UNKNOWN, exception.message)
            }
            exception.message?.contains("camera", ignoreCase = true) == true -> {
                ChatRtError.CameraError(CameraErrorCause.UNKNOWN, exception.message)
            }
            exception.message?.contains("screen", ignoreCase = true) == true ||
                exception.message?.contains("capture", ignoreCase = true) == true -> {
                ChatRtError.ScreenCaptureError(ScreenCaptureErrorCause.UNKNOWN, exception.message)
            }
            else -> {
                ChatRtError.ApiError(0, exception.message ?: "Unknown error")
            }
        }
}

/**
 * Error history entry for tracking error patterns
 */
data class ErrorHistoryEntry(
    val error: ChatRtError,
    val context: String,
    val timestamp: Long,
    val retryAttempts: Int,
)

/**
 * Recovery suggestion with action type
 */
data class RecoverySuggestion(
    val message: String,
    val type: RecoverySuggestionType,
)

enum class RecoverySuggestionType {
    USER_ACTION,
    SETTINGS,
    ACTION,
    RETRY,
}
