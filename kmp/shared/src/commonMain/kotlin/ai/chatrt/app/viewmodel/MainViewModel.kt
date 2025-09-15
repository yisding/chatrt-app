package ai.chatrt.app.viewmodel

import ai.chatrt.app.logging.DebugInfoCollector
import ai.chatrt.app.logging.Logger
import ai.chatrt.app.logging.WebRtcEventLogger
import ai.chatrt.app.models.*
import ai.chatrt.app.platform.*
import ai.chatrt.app.repository.ChatRepository
import ai.chatrt.app.utils.ErrorHandler
import ai.chatrt.app.utils.ErrorRecoveryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Main ViewModel for ChatRT application
 * Manages connection state, video mode, and logging functionality with comprehensive error handling
 * Requirements: 1.6, 2.6, 3.5, 4.3, 5.3
 */
class MainViewModel(
    private val chatRepository: ChatRepository,
    private val logger: Logger,
    private val webRtcEventLogger: WebRtcEventLogger,
    private val debugInfoCollector: DebugInfoCollector,
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

    // Logs management - using comprehensive logging system
    val logs =
        logger.getLogs().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    // WebRTC events and diagnostics
    val webRtcEvents =
        webRtcEventLogger.eventsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val connectionDiagnostics =
        webRtcEventLogger.diagnosticsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    // Debug information
    val debugInfo =
        debugInfoCollector.collectDebugInfo().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

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
                    logger.info("Connection", "Connection state changed to: $state")
                }
        }

        // Initialize logging
        logger.info("MainViewModel", "ChatRT MainViewModel initialized")

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
            logger.warning("Connection", "Connection already active")
            return
        }

        viewModelScope.launch {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                logger.info("Connection", "Starting connection with video mode: ${_videoMode.value}")

                // Create call request with current video mode
                val callRequest = createCallRequest()

                val result = chatRepository.createCall(callRequest)
                result.fold(
                    onSuccess = { response ->
                        currentCallId = response.callId
                        logger.info("Connection", "Call created successfully with ID: ${response.callId}")
                        webRtcEventLogger.logConnectionCreated(response.callId)

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
                        logger.error("Connection", "Failed to create call: ${exception.message}", exception)
                    },
                )
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.FAILED
                val error = errorHandler.mapExceptionToChatRtError(e, "Connection")
                handleErrorWithRecovery(error, "Connection") {
                    // Retry connection
                    startConnection()
                }
                logger.error("Connection", "Unexpected error during connection: ${e.message}", e)
            }
        }
    }

    /**
     * Stops the current ChatRT connection
     */
    fun stopConnection() {
        viewModelScope.launch {
            try {
                logger.info("Connection", "Stopping connection")

                val result = chatRepository.stopConnection()
                result.fold(
                    onSuccess = {
                        _connectionState.value = ConnectionState.DISCONNECTED
                        currentCallId = null
                        _isCallPaused.value = false
                        logger.info("Connection", "Connection stopped successfully")
                        currentCallId?.let { webRtcEventLogger.logConnectionClosed(it) }
                    },
                    onFailure = { exception ->
                        val error = errorHandler.mapExceptionToChatRtError(exception, "Disconnection")
                        errorHandler.handleError(error, "Disconnection")
                        logger.error("Connection", "Error stopping connection: ${exception.message}", exception)
                    },
                )
            } catch (e: Exception) {
                val error = errorHandler.mapExceptionToChatRtError(e, "Disconnection")
                errorHandler.handleError(error, "Disconnection")
                logger.error("Connection", "Unexpected error stopping connection: ${e.message}", e)
            }
        }
    }

    /**
     * Sets the video mode for the connection
     */
    fun setVideoMode(mode: VideoMode) {
        val previousMode = _videoMode.value
        _videoMode.value = mode
        logger.info("VideoMode", "Video mode changed from $previousMode to $mode")

        // If we're connected, we might need to restart the connection with new mode
        if (_connectionState.value == ConnectionState.CONNECTED) {
            logger.info("VideoMode", "Restarting connection with new video mode")
            restartConnectionWithNewMode()
        }
    }

    /**
     * Switches camera (front/back) - only applicable in webcam mode
     */
    fun switchCamera() {
        if (_videoMode.value != VideoMode.WEBCAM) {
            logger.warning("Camera", "Camera switch only available in webcam mode")
            return
        }

        logger.info("Camera", "Switching camera")
        // This will be handled by platform-specific implementations
        // For now, just log the action
    }

    /**
     * Handles app going to background
     */
    fun handleAppBackground() {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            logger.info("Lifecycle", "App going to background, maintaining connection")
            // Connection should continue in background via service
        }
    }

    /**
     * Handles app coming to foreground
     */
    fun handleAppForeground() {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            logger.info("Lifecycle", "App returning to foreground")
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
                    logger.info("Interruption", "ChatRT call paused due to phone call")
                }
            }
            InterruptionType.NETWORK_LOSS -> {
                if (_connectionState.value == ConnectionState.CONNECTED) {
                    _connectionState.value = ConnectionState.RECONNECTING
                    logger.warning("Network", "Network lost, attempting to reconnect")
                }
            }
            InterruptionType.LOW_POWER_MODE -> {
                suggestPowerOptimization()
            }
            else -> {
                logger.info("Interruption", "System interruption: ${interruption.type}")
            }
        }
    }

    /**
     * Resumes after system interruption
     */
    fun resumeAfterInterruption() {
        if (_isCallPaused.value) {
            _isCallPaused.value = false
            logger.info("Interruption", "Resuming ChatRT call after interruption")
        }
    }

    /**
     * Handles network quality changes
     */
    fun handleNetworkQualityChange(quality: NetworkQuality) {
        val previousQuality = _networkQuality.value
        _networkQuality.value = quality
        logger.info("Network", "Network quality changed from $previousQuality to $quality")

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
            logger.warning("Optimization", "Suggesting audio-only mode due to poor network")
        }
    }

    /**
     * Handles resource constraints
     */
    fun handleResourceConstraints(constraints: ResourceConstraints) {
        logger.debug("System", "Resource constraints updated - Memory: ${constraints.availableMemory}MB, CPU: ${constraints.cpuUsage}%")

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
            logger.warning("Optimization", "Suggesting optimization due to low memory")
        }
    }

    /**
     * Applies platform optimization
     */
    fun applyPlatformOptimization(optimization: PlatformOptimization) {
        logger.info("Optimization", "Applying platform optimization: ${optimization.reason}")

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
        logger.info("Optimization", "Platform optimization suggestion dismissed")
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
                logger.warning("Error", "Maximum retry attempts reached for ${currentError.errorCode}")
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
                    logger.info("Recovery", "Automatic recovery successful for ${error.errorCode}")
                    onRetry?.invoke()
                },
                onRecoveryFailed = { recoveryError ->
                    logger.warning("Recovery", "Automatic recovery failed for ${error.errorCode}: ${recoveryError.userMessage}")
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

        logger.info("Audio", "Audio device changed to: ${device.name} (${device.type})")
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
            logger.warning("Network", "Network connection lost, attempting to reconnect")
        } else if (networkState.isConnected && _connectionState.value == ConnectionState.RECONNECTING) {
            logger.info("Network", "Network connection restored")
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
                        logger.info("Permission", "Switched to audio-only mode due to camera permission denial")
                    }
                    PermissionType.SCREEN_CAPTURE -> {
                        // Automatic fallback to camera mode (Requirement 3.6)
                        setVideoMode(VideoMode.WEBCAM)
                        logger.info("Permission", "Switched to camera mode due to screen capture permission denial")
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
                logger.info("Camera", "Switched to audio-only mode due to camera error")
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
                logger.info("ScreenCapture", "Switched to camera mode due to screen capture error")
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
                logger.info("PhoneCall", "ChatRT call paused due to phone call")
            },
            onResume = {
                _isCallPaused.value = false
                logger.info("PhoneCall", "ChatRT call resumed after phone call")
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
     * Exports debug information including logs, WebRTC events, and diagnostics
     */
    suspend fun exportDebugInfo(): String = debugInfoCollector.exportDebugInfo()

    /**
     * Clears all logs
     */
    suspend fun clearLogs() {
        logger.clearLogs()
    }

    /**
     * Rotates logs to manage storage
     */
    suspend fun rotateLogs() {
        logger.rotateLogs()
    }

    /**
     * Creates a call request based on current settings
     */
    private fun createCallRequest(): CallRequest =
        CallRequest(
            // This will be filled by WebRTC implementation
            sdp = "",
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
                logger.info("Monitoring", "Connection monitoring started for call: $callId")
            },
            onFailure = { exception ->
                val error = errorHandler.mapExceptionToChatRtError(exception, "Connection Monitoring")
                errorHandler.handleError(error, "Connection Monitoring")
                logger.error("Monitoring", "Failed to start connection monitoring: ${exception.message}", exception)
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
        logger.warning("Battery", "Suggesting power optimization due to low battery")
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
