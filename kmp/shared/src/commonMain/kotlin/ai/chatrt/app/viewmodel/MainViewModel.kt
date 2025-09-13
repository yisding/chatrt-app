package ai.chatrt.app.viewmodel

import ai.chatrt.app.models.*
import ai.chatrt.app.platform.*
import ai.chatrt.app.repository.ChatRepository
import ai.chatrt.app.utils.ErrorHandler
import ai.chatrt.app.utils.ErrorRecoveryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * Main ViewModel for ChatRT application
 * Manages connection state, video mode, and logging functionality with comprehensive error handling
 * Requirements: 1.6, 2.6, 3.5, 4.3, 5.3
 */
class MainViewModel(
    private val chatRepository: ChatRepository,
    private val audioManager: AudioManager? = null,
    private val permissionManager: PermissionManager? = null,
    private val networkMonitor: NetworkMonitor? = null,
) : ViewModel() {
    // Connection state management
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Video mode management
    private val _videoMode = MutableStateFlow(VideoMode.AUDIO_ONLY)
    val videoMode: StateFlow<VideoMode> = _videoMode.asStateFlow()

    // Logs management
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    // Network quality monitoring
    private val _networkQuality = MutableStateFlow(NetworkQuality.GOOD)
    val networkQuality: StateFlow<NetworkQuality> = _networkQuality.asStateFlow()

    // Platform optimization state
    private val _platformOptimization = MutableStateFlow<PlatformOptimization?>(null)
    val platformOptimization: StateFlow<PlatformOptimization?> = _platformOptimization.asStateFlow()

    // Call pause state for system interruptions
    private val _isCallPaused = MutableStateFlow(false)
    val isCallPaused: StateFlow<Boolean> = _isCallPaused.asStateFlow()

    // Error handling system
    private val errorHandler = ErrorHandler()
    private val errorRecoveryManager =
        ErrorRecoveryManager(
            errorHandler = errorHandler,
            audioManager = audioManager,
            permissionManager = permissionManager,
            networkMonitor = networkMonitor,
        )

    // Error state management
    val error: StateFlow<ChatRtError?> = errorHandler.currentError
    val errorHistory: StateFlow<List<ai.chatrt.app.utils.ErrorHistoryEntry>> = errorHandler.errorHistory

    // Current call ID for monitoring
    private var currentCallId: String? = null

    // Audio device monitoring
    private val _currentAudioDevice = MutableStateFlow<AudioDevice?>(null)
    val currentAudioDevice: StateFlow<AudioDevice?> = _currentAudioDevice.asStateFlow()

    init {
        // Observe connection state changes from repository
        viewModelScope.launch {
            chatRepository
                .observeConnectionState()
                .collect { state ->
                    _connectionState.value = state
                    addLog("Connection state changed to: $state", LogLevel.INFO)
                }
        }

        // Observe logs from repository
        viewModelScope.launch {
            chatRepository
                .observeLogs()
                .collect { newLogs ->
                    _logs.value = newLogs
                }
        }

        // Observe audio device changes
        audioManager?.let { manager ->
            viewModelScope.launch {
                manager
                    .observeAudioDeviceChanges()
                    .collect { device ->
                        _currentAudioDevice.value = device
                        handleAudioDeviceChange(device)
                    }
            }
        }

        // Observe network changes
        networkMonitor?.let { monitor ->
            viewModelScope.launch {
                monitor
                    .observeNetworkState()
                    .collect { networkState ->
                        handleNetworkStateChange(networkState)
                    }
            }
            // Observe network quality separately
            viewModelScope.launch {
                monitor
                    .observeNetworkQuality()
                    .collect { quality ->
                        handleNetworkQualityChange(quality)
                    }
            }
        }
    }

    /**
     * Starts a new ChatRT connection
     */
    fun startConnection() {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            addLog("Connection already active", LogLevel.WARNING)
            return
        }

        viewModelScope.launch {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                addLog("Starting connection with video mode: ${_videoMode.value}", LogLevel.INFO)

                // Create call request with current video mode
                val callRequest = createCallRequest()

                val result = chatRepository.createCall(callRequest)
                result.fold(
                    onSuccess = { response ->
                        currentCallId = response.callId
                        addLog("Call created successfully with ID: ${response.callId}", LogLevel.INFO)

                        // Start monitoring the connection
                        startConnectionMonitoring(response.callId)
                    },
                    onFailure = { exception ->
                        _connectionState.value = ConnectionState.FAILED
                        val error = errorHandler.mapExceptionToChatRtError(exception, "Connection")
                        handleErrorWithRecovery(error, "Connection") {
                            // Retry connection
                            startConnection()
                        }
                        addLog("Failed to create call: ${exception.message}", LogLevel.ERROR)
                    },
                )
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.FAILED
                val error = errorHandler.mapExceptionToChatRtError(e, "Connection")
                handleErrorWithRecovery(error, "Connection") {
                    // Retry connection
                    startConnection()
                }
                addLog("Unexpected error during connection: ${e.message}", LogLevel.ERROR)
            }
        }
    }

    /**
     * Stops the current ChatRT connection
     */
    fun stopConnection() {
        viewModelScope.launch {
            try {
                addLog("Stopping connection", LogLevel.INFO)

                val result = chatRepository.stopConnection()
                result.fold(
                    onSuccess = {
                        _connectionState.value = ConnectionState.DISCONNECTED
                        currentCallId = null
                        _isCallPaused.value = false
                        addLog("Connection stopped successfully", LogLevel.INFO)
                    },
                    onFailure = { exception ->
                        val error = errorHandler.mapExceptionToChatRtError(exception, "Disconnection")
                        errorHandler.handleError(error, "Disconnection")
                        addLog("Error stopping connection: ${exception.message}", LogLevel.ERROR)
                    },
                )
            } catch (e: Exception) {
                val error = errorHandler.mapExceptionToChatRtError(e, "Disconnection")
                errorHandler.handleError(error, "Disconnection")
                addLog("Unexpected error stopping connection: ${e.message}", LogLevel.ERROR)
            }
        }
    }

    /**
     * Sets the video mode for the connection
     */
    fun setVideoMode(mode: VideoMode) {
        val previousMode = _videoMode.value
        _videoMode.value = mode
        addLog("Video mode changed from $previousMode to $mode", LogLevel.INFO)

        // If we're connected, we might need to restart the connection with new mode
        if (_connectionState.value == ConnectionState.CONNECTED) {
            addLog("Restarting connection with new video mode", LogLevel.INFO)
            restartConnectionWithNewMode()
        }
    }

    /**
     * Switches camera (front/back) - only applicable in webcam mode
     */
    fun switchCamera() {
        if (_videoMode.value != VideoMode.WEBCAM) {
            addLog("Camera switch only available in webcam mode", LogLevel.WARNING)
            return
        }

        addLog("Switching camera", LogLevel.INFO)
        // This will be handled by platform-specific implementations
        // For now, just log the action
    }

    /**
     * Handles app going to background
     */
    fun handleAppBackground() {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            addLog("App going to background, maintaining connection", LogLevel.INFO)
            // Connection should continue in background via service
        }
    }

    /**
     * Handles app coming to foreground
     */
    fun handleAppForeground() {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            addLog("App returning to foreground", LogLevel.INFO)
        }
    }

    /**
     * Handles system interruptions (phone calls, etc.)
     */
    fun handleSystemInterruption(interruption: SystemInterruption) {
        when (interruption.type) {
            InterruptionType.PHONE_CALL -> {
                if (interruption.shouldPause && _connectionState.value == ConnectionState.CONNECTED) {
                    _isCallPaused.value = true
                    addLog("ChatRT call paused due to phone call", LogLevel.INFO)
                }
            }
            InterruptionType.NETWORK_LOSS -> {
                if (_connectionState.value == ConnectionState.CONNECTED) {
                    _connectionState.value = ConnectionState.RECONNECTING
                    addLog("Network lost, attempting to reconnect", LogLevel.WARNING)
                }
            }
            InterruptionType.LOW_POWER_MODE -> {
                suggestPowerOptimization()
            }
            else -> {
                addLog("System interruption: ${interruption.type}", LogLevel.INFO)
            }
        }
    }

    /**
     * Resumes after system interruption
     */
    fun resumeAfterInterruption() {
        if (_isCallPaused.value) {
            _isCallPaused.value = false
            addLog("Resuming ChatRT call after interruption", LogLevel.INFO)
        }
    }

    /**
     * Handles network quality changes
     */
    fun handleNetworkQualityChange(quality: NetworkQuality) {
        val previousQuality = _networkQuality.value
        _networkQuality.value = quality
        addLog("Network quality changed from $previousQuality to $quality", LogLevel.INFO)

        // Suggest optimization based on network quality
        if (quality == NetworkQuality.POOR) {
            val optimization =
                PlatformOptimization(
                    recommendedVideoMode = VideoMode.AUDIO_ONLY,
                    recommendedAudioQuality = AudioQuality.LOW,
                    disableVideoPreview = true,
                    reason = OptimizationReason.POOR_NETWORK,
                )
            _platformOptimization.value = optimization
            addLog("Suggesting audio-only mode due to poor network", LogLevel.WARNING)
        }
    }

    /**
     * Handles resource constraints
     */
    fun handleResourceConstraints(constraints: ResourceConstraints) {
        addLog("Resource constraints updated - Memory: ${constraints.availableMemory}MB, CPU: ${constraints.cpuUsage}%", LogLevel.DEBUG)

        // Suggest optimization based on resource constraints
        if (constraints.availableMemory < 100_000_000) { // Less than 100MB
            val optimization =
                PlatformOptimization(
                    recommendedVideoMode = VideoMode.AUDIO_ONLY,
                    recommendedAudioQuality = AudioQuality.LOW,
                    disableVideoPreview = true,
                    reason = OptimizationReason.LOW_MEMORY,
                )
            _platformOptimization.value = optimization
            addLog("Suggesting optimization due to low memory", LogLevel.WARNING)
        }
    }

    /**
     * Applies platform optimization
     */
    fun applyPlatformOptimization(optimization: PlatformOptimization) {
        addLog("Applying platform optimization: ${optimization.reason}", LogLevel.INFO)

        // Apply the recommended video mode
        if (optimization.recommendedVideoMode != _videoMode.value) {
            setVideoMode(optimization.recommendedVideoMode)
        }

        // Clear the optimization suggestion
        _platformOptimization.value = null
    }

    /**
     * Dismisses the current platform optimization suggestion
     */
    fun dismissOptimization() {
        _platformOptimization.value = null
        addLog("Platform optimization suggestion dismissed", LogLevel.INFO)
    }

    /**
     * Clears the current error state
     */
    fun clearError() {
        errorHandler.clearCurrentError()
    }

    /**
     * Retries the current error if possible
     * Requirement: 4.3 - Error handling with retry mechanisms
     */
    fun retryCurrentError() {
        val currentError = error.value
        if (currentError != null) {
            val canRetry =
                errorHandler.retryCurrentError {
                    // Determine what to retry based on error type
                    when (currentError) {
                        is ChatRtError.NetworkError, is ChatRtError.WebRtcError, is ChatRtError.ApiError -> {
                            startConnection()
                        }
                        else -> {
                            // Other error types handled by recovery manager
                        }
                    }
                }

            if (!canRetry) {
                addLog("Maximum retry attempts reached for ${currentError.errorCode}", LogLevel.WARNING)
            }
        }
    }

    /**
     * Handles errors with automatic recovery attempts
     * Requirements: 1.6, 2.6, 3.5, 4.3, 5.3
     */
    private fun handleErrorWithRecovery(
        error: ChatRtError,
        context: String,
        onRetry: (() -> Unit)? = null,
    ) {
        errorHandler.handleError(error, context, onRetry)

        // Attempt automatic recovery
        viewModelScope.launch {
            errorRecoveryManager.attemptRecovery(
                error = error,
                context = context,
                onRecoverySuccess = {
                    addLog("Automatic recovery successful for ${error.errorCode}", LogLevel.INFO)
                    onRetry?.invoke()
                },
                onRecoveryFailed = { recoveryError ->
                    addLog("Automatic recovery failed for ${error.errorCode}: ${recoveryError.userMessage}", LogLevel.WARNING)
                },
            )
        }
    }

    /**
     * Handles audio device changes with automatic routing
     * Requirement: 5.3 - Device state changes with appropriate UI feedback
     */
    private fun handleAudioDeviceChange(device: AudioDevice) {
        val deviceStateChange =
            when (device.type) {
                AudioDeviceType.WIRED_HEADSET, AudioDeviceType.WIRED_HEADPHONES -> DeviceStateChange.HEADPHONES_CONNECTED
                AudioDeviceType.BLUETOOTH_HEADSET -> DeviceStateChange.BLUETOOTH_CONNECTED
                AudioDeviceType.SPEAKER, AudioDeviceType.EARPIECE -> {
                    // Determine if this is a disconnection based on previous device
                    val previousDevice = _currentAudioDevice.value
                    when (previousDevice?.type) {
                        AudioDeviceType.WIRED_HEADSET, AudioDeviceType.WIRED_HEADPHONES -> DeviceStateChange.HEADPHONES_DISCONNECTED
                        AudioDeviceType.BLUETOOTH_HEADSET -> DeviceStateChange.BLUETOOTH_DISCONNECTED
                        else -> null
                    }
                }
                else -> null
            }

        deviceStateChange?.let { change ->
            val deviceError = ChatRtError.DeviceStateError(change, "Audio device changed to ${device.name}")
            handleErrorWithRecovery(deviceError, "Audio Device Change")
        }

        addLog("Audio device changed to: ${device.name} (${device.type})", LogLevel.INFO)
    }

    /**
     * Handles network state changes
     */
    private fun handleNetworkStateChange(networkState: ai.chatrt.app.platform.NetworkState) {
        if (!networkState.isConnected && _connectionState.value == ConnectionState.CONNECTED) {
            val networkError = ChatRtError.NetworkError(NetworkErrorCause.NO_INTERNET, "Network connection lost")
            handleErrorWithRecovery(networkError, "Network") {
                // Attempt to reconnect when network is restored
                if (networkState.isConnected) {
                    startConnection()
                }
            }
            _connectionState.value = ConnectionState.RECONNECTING
            addLog("Network connection lost, attempting to reconnect", LogLevel.WARNING)
        } else if (networkState.isConnected && _connectionState.value == ConnectionState.RECONNECTING) {
            addLog("Network connection restored", LogLevel.INFO)
            // Connection will be restored by the recovery mechanism
        }
    }

    /**
     * Handles permission errors with automatic fallbacks
     * Requirements: 1.2, 2.6, 3.6 - Permission handling with fallback options
     */
    fun handlePermissionError(
        permission: PermissionType,
        isPermanentlyDenied: Boolean = false,
    ) {
        errorHandler.handlePermissionError(
            permission = permission,
            isPermanentlyDenied = isPermanentlyDenied,
            onFallback = {
                when (permission) {
                    PermissionType.CAMERA -> {
                        // Automatic fallback to audio-only mode (Requirement 2.6)
                        setVideoMode(VideoMode.AUDIO_ONLY)
                        addLog("Switched to audio-only mode due to camera permission denial", LogLevel.INFO)
                    }
                    PermissionType.SCREEN_CAPTURE -> {
                        // Automatic fallback to camera mode (Requirement 3.6)
                        setVideoMode(VideoMode.WEBCAM)
                        addLog("Switched to camera mode due to screen capture permission denial", LogLevel.INFO)
                    }
                    else -> {
                        // Handle other permission types
                    }
                }
            },
        )
    }

    /**
     * Handles camera errors with fallback to audio-only
     * Requirement: 2.6 - Camera permission denied with fallback to audio-only
     */
    fun handleCameraError(
        cause: CameraErrorCause,
        cameraId: String? = null,
    ) {
        errorHandler.handleCameraError(
            cause = cause,
            cameraId = cameraId,
            onFallbackToAudio = {
                setVideoMode(VideoMode.AUDIO_ONLY)
                addLog("Switched to audio-only mode due to camera error", LogLevel.INFO)
            },
        )
    }

    /**
     * Handles screen capture errors with fallback to camera mode
     * Requirement: 3.6 - Screen sharing permission denied with alternative modes
     */
    fun handleScreenCaptureError(
        cause: ScreenCaptureErrorCause,
        details: String? = null,
    ) {
        errorHandler.handleScreenCaptureError(
            cause = cause,
            details = details,
            onFallbackToCamera = {
                setVideoMode(VideoMode.WEBCAM)
                addLog("Switched to camera mode due to screen capture error", LogLevel.INFO)
            },
        )
    }

    /**
     * Handles phone call interruptions
     * Requirement: 5.2 - Phone call interruption handling
     */
    fun handlePhoneCallInterruption(callState: PhoneCallState) {
        errorHandler.handlePhoneCallInterruption(
            callState = callState,
            onPause = {
                _isCallPaused.value = true
                addLog("ChatRT call paused due to phone call", LogLevel.INFO)
            },
            onResume = {
                _isCallPaused.value = false
                addLog("ChatRT call resumed after phone call", LogLevel.INFO)
            },
        )
    }

    /**
     * Gets recovery suggestions for the current error
     */
    fun getRecoverySuggestions(): List<ai.chatrt.app.utils.RecoverySuggestion> {
        val currentError = error.value
        return if (currentError != null) {
            errorRecoveryManager
                .getGuidedRecoverySteps(currentError)
                .map { step ->
                    ai.chatrt.app.utils.RecoverySuggestion(
                        message = step.description,
                        type =
                            when (step.type) {
                                ai.chatrt.app.utils.RecoveryStepType.RETRY -> ai.chatrt.app.utils.RecoverySuggestionType.RETRY
                                ai.chatrt.app.utils.RecoveryStepType.CHECK_SETTINGS,
                                ai.chatrt.app.utils.RecoveryStepType.NAVIGATE_TO_SETTINGS,
                                -> ai.chatrt.app.utils.RecoverySuggestionType.SETTINGS
                                ai.chatrt.app.utils.RecoveryStepType.SWITCH_DEVICE,
                                ai.chatrt.app.utils.RecoveryStepType.FALLBACK_MODE,
                                -> ai.chatrt.app.utils.RecoverySuggestionType.ACTION
                                else -> ai.chatrt.app.utils.RecoverySuggestionType.USER_ACTION
                            },
                    )
                }
        } else {
            emptyList()
        }
    }

    /**
     * Adds a log entry with current timestamp
     */
    private fun addLog(
        message: String,
        level: LogLevel = LogLevel.INFO,
    ) {
        val logEntry =
            LogEntry(
                timestamp = Clock.System.now().toEpochMilliseconds(),
                message = message,
                level = level,
            )

        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(logEntry)

        // Keep only the last 100 log entries to prevent memory issues
        if (currentLogs.size > 100) {
            currentLogs.removeAt(0)
        }

        _logs.value = currentLogs
    }

    /**
     * Creates a call request based on current settings
     */
    private fun createCallRequest(): CallRequest =
        CallRequest(
            sdp = "", // This will be filled by WebRTC implementation
            session =
                SessionConfig(
                    type = "realtime",
                    model = "gpt-realtime",
                    instructions = "You are a helpful AI assistant. Respond naturally to voice conversations.",
                    audio =
                        AudioConfig(
                            input =
                                AudioInputConfig(
                                    noiseReduction = NoiseReductionConfig(type = "near_field"),
                                ),
                            output = AudioOutputConfig(voice = "marin"),
                        ),
                ),
        )

    /**
     * Starts monitoring the connection for the given call ID
     */
    private suspend fun startConnectionMonitoring(callId: String) {
        val result = chatRepository.startConnectionMonitoring(callId)
        result.fold(
            onSuccess = {
                addLog("Connection monitoring started for call: $callId", LogLevel.INFO)
            },
            onFailure = { exception ->
                val error = errorHandler.mapExceptionToChatRtError(exception, "Connection Monitoring")
                errorHandler.handleError(error, "Connection Monitoring")
                addLog("Failed to start connection monitoring: ${exception.message}", LogLevel.ERROR)
            },
        )
    }

    /**
     * Restarts connection with new video mode
     */
    private fun restartConnectionWithNewMode() {
        viewModelScope.launch {
            // Stop current connection
            stopConnection()

            // Wait a moment for cleanup
            kotlinx.coroutines.delay(1000)

            // Start new connection with updated mode
            startConnection()
        }
    }

    /**
     * Suggests power optimization based on current state
     */
    private fun suggestPowerOptimization() {
        val optimization =
            PlatformOptimization(
                recommendedVideoMode = VideoMode.AUDIO_ONLY,
                recommendedAudioQuality = AudioQuality.LOW,
                disableVideoPreview = true,
                reason = OptimizationReason.LOW_BATTERY,
            )
        _platformOptimization.value = optimization
        addLog("Suggesting power optimization due to low battery", LogLevel.WARNING)
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up any resources
        viewModelScope.launch {
            if (_connectionState.value == ConnectionState.CONNECTED) {
                chatRepository.stopConnection()
            }
        }

        // Cleanup error handling resources
        errorRecoveryManager.cleanup()
        audioManager?.let { manager ->
            viewModelScope.launch {
                manager.cleanup()
            }
        }
    }
}
