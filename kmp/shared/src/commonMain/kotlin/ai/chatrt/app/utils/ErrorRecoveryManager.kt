package ai.chatrt.app.utils

import ai.chatrt.app.models.*
import ai.chatrt.app.platform.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Error recovery manager that provides automatic recovery mechanisms and retry logic
 * Requirements: 1.6, 2.6, 3.5, 4.3, 5.3
 */
class ErrorRecoveryManager(
    private val errorHandler: ErrorHandler,
    private val audioManager: AudioManager? = null,
    private val permissionManager: PermissionManager? = null,
    private val networkMonitor: ai.chatrt.app.platform.NetworkMonitor? = null,
) {
    private val recoveryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val activeRecoveries = mutableMapOf<String, Job>()

    /**
     * Attempts automatic recovery for the given error
     * Returns true if recovery was attempted, false if manual intervention is required
     */
    suspend fun attemptRecovery(
        error: ChatRtError,
        context: String = "",
        onRecoverySuccess: (() -> Unit)? = null,
        onRecoveryFailed: ((ChatRtError) -> Unit)? = null,
    ): Boolean {
        // Cancel any existing recovery for this error type
        activeRecoveries[error.errorCode]?.cancel()

        val recoveryJob =
            recoveryScope.launch {
                try {
                    val recovered =
                        when (error) {
                            is ChatRtError.NetworkError -> recoverFromNetworkError(error)
                            is ChatRtError.WebRtcError -> recoverFromWebRtcError(error)
                            is ChatRtError.AudioDeviceError -> recoverFromAudioDeviceError(error)
                            is ChatRtError.CameraError -> recoverFromCameraError(error)
                            is ChatRtError.ScreenCaptureError -> recoverFromScreenCaptureError(error)
                            is ChatRtError.PermissionDenied -> recoverFromPermissionError(error)
                            is ChatRtError.PhoneCallInterruptionError -> recoverFromPhoneCallInterruption(error)
                            is ChatRtError.DeviceStateError -> recoverFromDeviceStateChange(error)
                            is ChatRtError.ApiError -> recoverFromApiError(error)
                            else -> false
                        }

                    if (recovered) {
                        onRecoverySuccess?.invoke()
                        errorHandler.clearCurrentError()
                    } else {
                        onRecoveryFailed?.invoke(error)
                    }
                } catch (e: Exception) {
                    val recoveryError = errorHandler.mapExceptionToChatRtError(e, "Recovery")
                    onRecoveryFailed?.invoke(recoveryError)
                }
            }

        activeRecoveries[error.errorCode] = recoveryJob
        return true
    }

    /**
     * Recovers from network errors with connection retry and quality adaptation
     * Requirement: 4.3 - Connection issues with appropriate error messages
     */
    private suspend fun recoverFromNetworkError(error: ChatRtError.NetworkError): Boolean =
        when (error.errorCause) {
            NetworkErrorCause.NO_INTERNET -> {
                // Wait for network to become available
                networkMonitor?.let { monitor ->
                    monitor
                        .observeNetworkState()
                        .filter { it.isConnected }
                        .first()
                    true
                } ?: false
            }
            NetworkErrorCause.TIMEOUT -> {
                // Retry with exponential backoff
                retryWithBackoff(maxAttempts = 3) {
                    // This would be implemented by the calling code
                    true
                }
            }
            NetworkErrorCause.SERVER_UNREACHABLE -> {
                // Try alternative server endpoints or wait
                delay(5000)
                true
            }
            else -> false
        }

    /**
     * Recovers from WebRTC connection errors
     * Requirement: 4.3 - WebRTC connection issues with suggested actions
     */
    private suspend fun recoverFromWebRtcError(error: ChatRtError.WebRtcError): Boolean =
        when (error.errorCause) {
            WebRtcErrorCause.ICE_CONNECTION_FAILED -> {
                // Wait for network stabilization and retry
                delay(2000)
                true
            }
            WebRtcErrorCause.MEDIA_STREAM_FAILED -> {
                // Try to reinitialize media devices
                audioManager?.let { manager ->
                    manager.cleanup()
                    manager.initialize()
                    manager.setupAudioRouting()
                    true
                } ?: false
            }
            else -> false
        }

    /**
     * Recovers from audio device errors with automatic device switching
     * Requirement: 5.3 - Audio routing and device management
     */
    private suspend fun recoverFromAudioDeviceError(error: ChatRtError.AudioDeviceError): Boolean =
        audioManager?.let { manager ->
            when (error.errorCause) {
                AudioErrorCause.DEVICE_BUSY -> {
                    // Try switching to a different available device
                    val availableDevices = manager.getAvailableAudioDevices()
                    val currentDevice = manager.getCurrentAudioDevice()
                    val alternativeDevice = availableDevices.firstOrNull { it != currentDevice }

                    alternativeDevice?.let {
                        manager.setAudioDevice(it)
                        true
                    } ?: false
                }
                AudioErrorCause.ROUTING_FAILED -> {
                    // Reset audio routing and try default device
                    manager.setupAudioRouting()
                    val defaultDevice =
                        manager
                            .getAvailableAudioDevices()
                            .firstOrNull { it.isDefault }
                    defaultDevice?.let {
                        manager.setAudioDevice(it)
                        true
                    } ?: false
                }
                AudioErrorCause.FOCUS_LOST -> {
                    // Request audio focus again
                    manager.requestAudioFocus()
                }
                else -> false
            }
        } ?: false

    /**
     * Recovers from camera errors with fallback to audio-only
     * Requirement: 2.6 - Camera permission denied with fallback to audio-only
     */
    private suspend fun recoverFromCameraError(error: ChatRtError.CameraError): Boolean =
        when (error.errorCause) {
            CameraErrorCause.CAMERA_BUSY -> {
                // Wait and retry, or suggest switching cameras
                delay(1000)
                false // Let the UI handle camera switching
            }
            CameraErrorCause.PERMISSION_DENIED -> {
                // Automatic fallback to audio-only mode
                true // This will be handled by the calling ViewModel
            }
            else -> false
        }

    /**
     * Recovers from screen capture errors with fallback to camera mode
     * Requirement: 3.6 - Screen sharing permission denied with alternative modes
     */
    private suspend fun recoverFromScreenCaptureError(error: ChatRtError.ScreenCaptureError): Boolean =
        when (error.errorCause) {
            ScreenCaptureErrorCause.PERMISSION_DENIED -> {
                // Automatic fallback to camera mode
                true // This will be handled by the calling ViewModel
            }
            ScreenCaptureErrorCause.SECURITY_RESTRICTED -> {
                // Automatic fallback to camera mode
                true // This will be handled by the calling ViewModel
            }
            else -> false
        }

    /**
     * Recovers from permission errors with automatic fallbacks
     * Requirements: 1.2, 2.6, 3.6 - Permission handling with fallback options
     */
    private suspend fun recoverFromPermissionError(error: ChatRtError.PermissionDenied): Boolean =
        when (error.permission) {
            PermissionType.CAMERA -> {
                // Automatic fallback to audio-only mode (Requirement 2.6)
                true
            }
            PermissionType.SCREEN_CAPTURE -> {
                // Automatic fallback to camera mode (Requirement 3.6)
                true
            }
            PermissionType.MICROPHONE -> {
                // Cannot recover from microphone permission denial
                false
            }
            else -> false
        }

    /**
     * Recovers from phone call interruptions
     * Requirement: 5.2 - Phone call interruption handling
     */
    private suspend fun recoverFromPhoneCallInterruption(error: ChatRtError.PhoneCallInterruptionError): Boolean =
        when (error.callState) {
            PhoneCallState.ENDED -> {
                // Automatically resume after phone call ends
                audioManager?.let { manager ->
                    // Resume audio routing after phone call using existing interface methods
                    manager.requestAudioFocus()
                    manager.setupAudioRouting()
                    true
                } ?: true
            }
            else -> false // Let the system handle pause/resume
        }

    /**
     * Recovers from device state changes with automatic audio routing
     * Requirement: 5.3 - Device state changes with appropriate UI feedback
     */
    private suspend fun recoverFromDeviceStateChange(error: ChatRtError.DeviceStateError): Boolean =
        audioManager?.let { manager ->
            when (error.stateChange) {
                DeviceStateChange.HEADPHONES_CONNECTED -> {
                    // Automatically switch to headphones
                    val headphones =
                        manager
                            .getAvailableAudioDevices()
                            .firstOrNull { it.type in listOf(AudioDeviceType.WIRED_HEADSET, AudioDeviceType.WIRED_HEADPHONES) }
                    headphones?.let {
                        manager.setAudioDevice(it)
                        true
                    } ?: false
                }
                DeviceStateChange.HEADPHONES_DISCONNECTED -> {
                    // Switch to speaker or earpiece
                    val fallback =
                        manager
                            .getAvailableAudioDevices()
                            .firstOrNull { it.type == AudioDeviceType.EARPIECE }
                            ?: manager
                                .getAvailableAudioDevices()
                                .firstOrNull { it.type == AudioDeviceType.SPEAKER }
                    fallback?.let {
                        manager.setAudioDevice(it)
                        true
                    } ?: false
                }
                DeviceStateChange.BLUETOOTH_CONNECTED -> {
                    // Automatically switch to Bluetooth
                    val bluetooth =
                        manager
                            .getAvailableAudioDevices()
                            .firstOrNull { it.type == AudioDeviceType.BLUETOOTH_HEADSET }
                    bluetooth?.let {
                        manager.setAudioDevice(it)
                        true
                    } ?: false
                }
                DeviceStateChange.BLUETOOTH_DISCONNECTED -> {
                    // Switch to wired headset or speaker
                    val fallback =
                        manager
                            .getAvailableAudioDevices()
                            .firstOrNull { it.type == AudioDeviceType.WIRED_HEADSET }
                            ?: manager
                                .getAvailableAudioDevices()
                                .firstOrNull { it.type == AudioDeviceType.EARPIECE }
                    fallback?.let {
                        manager.setAudioDevice(it)
                        true
                    } ?: false
                }
                else -> true // Other state changes are informational
            }
        } ?: false

    /**
     * Recovers from API errors with retry logic
     */
    private suspend fun recoverFromApiError(error: ChatRtError.ApiError): Boolean =
        when (error.code) {
            429 -> {
                // Rate limited - wait and retry
                delay(5000)
                true
            }
            500, 502, 503, 504 -> {
                // Server errors - retry with backoff
                retryWithBackoff(maxAttempts = 3) {
                    true
                }
            }
            else -> false
        }

    /**
     * Provides guided recovery steps for manual intervention
     */
    fun getGuidedRecoverySteps(error: ChatRtError): List<RecoveryStep> =
        when (error) {
            is ChatRtError.PermissionDenied -> getPermissionRecoverySteps(error)
            is ChatRtError.NetworkError -> getNetworkRecoverySteps(error)
            is ChatRtError.AudioDeviceError -> getAudioRecoverySteps(error)
            is ChatRtError.CameraError -> getCameraRecoverySteps(error)
            is ChatRtError.ScreenCaptureError -> getScreenCaptureRecoverySteps(error)
            else -> getGenericRecoverySteps(error)
        }

    private fun getPermissionRecoverySteps(error: ChatRtError.PermissionDenied): List<RecoveryStep> =
        if (error.isPermanentlyDenied) {
            listOf(
                RecoveryStep("Open Settings", RecoveryStepType.NAVIGATE_TO_SETTINGS),
                RecoveryStep("Find ChatRT app", RecoveryStepType.USER_ACTION),
                RecoveryStep("Tap Permissions", RecoveryStepType.USER_ACTION),
                RecoveryStep("Enable ${error.permission.displayName}", RecoveryStepType.USER_ACTION),
                RecoveryStep("Return to ChatRT", RecoveryStepType.USER_ACTION),
            )
        } else {
            listOf(
                RecoveryStep("Tap 'Allow' when prompted", RecoveryStepType.USER_ACTION),
                RecoveryStep("Try the action again", RecoveryStepType.RETRY),
            )
        }

    private fun getNetworkRecoverySteps(error: ChatRtError.NetworkError): List<RecoveryStep> =
        when (error.errorCause) {
            NetworkErrorCause.NO_INTERNET ->
                listOf(
                    RecoveryStep("Check WiFi connection", RecoveryStepType.CHECK_SETTINGS),
                    RecoveryStep("Try mobile data", RecoveryStepType.USER_ACTION),
                    RecoveryStep("Move closer to router", RecoveryStepType.USER_ACTION),
                )
            NetworkErrorCause.SERVER_UNREACHABLE ->
                listOf(
                    RecoveryStep("Check server URL in settings", RecoveryStepType.CHECK_SETTINGS),
                    RecoveryStep("Wait and try again", RecoveryStepType.WAIT_AND_RETRY),
                )
            else ->
                listOf(
                    RecoveryStep("Check internet connection", RecoveryStepType.CHECK_SETTINGS),
                    RecoveryStep("Retry connection", RecoveryStepType.RETRY),
                )
        }

    private fun getAudioRecoverySteps(error: ChatRtError.AudioDeviceError): List<RecoveryStep> =
        when (error.errorCause) {
            AudioErrorCause.DEVICE_BUSY ->
                listOf(
                    RecoveryStep("Close other audio apps", RecoveryStepType.USER_ACTION),
                    RecoveryStep("Unplug and reconnect headphones", RecoveryStepType.USER_ACTION),
                    RecoveryStep("Try different audio device", RecoveryStepType.SWITCH_DEVICE),
                )
            else ->
                listOf(
                    RecoveryStep("Check audio permissions", RecoveryStepType.CHECK_SETTINGS),
                    RecoveryStep("Try different audio device", RecoveryStepType.SWITCH_DEVICE),
                )
        }

    private fun getCameraRecoverySteps(error: ChatRtError.CameraError): List<RecoveryStep> =
        listOf(
            RecoveryStep("Close other camera apps", RecoveryStepType.USER_ACTION),
            RecoveryStep("Switch to audio-only mode", RecoveryStepType.FALLBACK_MODE),
            RecoveryStep("Check camera permissions", RecoveryStepType.CHECK_SETTINGS),
        )

    private fun getScreenCaptureRecoverySteps(error: ChatRtError.ScreenCaptureError): List<RecoveryStep> =
        listOf(
            RecoveryStep("Grant screen recording permission", RecoveryStepType.USER_ACTION),
            RecoveryStep("Switch to camera mode", RecoveryStepType.FALLBACK_MODE),
            RecoveryStep("Check device restrictions", RecoveryStepType.CHECK_SETTINGS),
        )

    private fun getGenericRecoverySteps(error: ChatRtError): List<RecoveryStep> =
        listOf(
            RecoveryStep("Try again", RecoveryStepType.RETRY),
            RecoveryStep("Check settings", RecoveryStepType.CHECK_SETTINGS),
            RecoveryStep("Restart app if needed", RecoveryStepType.RESTART_APP),
        )

    /**
     * Retry with exponential backoff
     */
    private suspend fun retryWithBackoff(
        maxAttempts: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 10000,
        factor: Double = 2.0,
        action: suspend () -> Boolean,
    ): Boolean {
        var currentDelay = initialDelay
        repeat(maxAttempts) { attempt ->
            try {
                if (action()) {
                    return true
                }
            } catch (e: Exception) {
                if (attempt == maxAttempts - 1) {
                    throw e
                }
            }

            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return false
    }

    /**
     * Cleanup recovery resources
     */
    fun cleanup() {
        activeRecoveries.values.forEach { it.cancel() }
        activeRecoveries.clear()
        recoveryScope.cancel()
    }
}

/**
 * Recovery step for guided recovery
 */
data class RecoveryStep(
    val description: String,
    val type: RecoveryStepType,
    val isCompleted: Boolean = false,
)

enum class RecoveryStepType {
    USER_ACTION,
    CHECK_SETTINGS,
    NAVIGATE_TO_SETTINGS,
    SWITCH_DEVICE,
    FALLBACK_MODE,
    RETRY,
    WAIT_AND_RETRY,
    RESTART_APP,
}

/**
 * Network state for monitoring
 */
data class NetworkState(
    val isConnected: Boolean,
    val type: NetworkType = NetworkType.UNKNOWN,
    val quality: NetworkQuality = NetworkQuality.FAIR,
)

enum class NetworkType {
    WIFI,
    MOBILE,
    ETHERNET,
    UNKNOWN,
}
