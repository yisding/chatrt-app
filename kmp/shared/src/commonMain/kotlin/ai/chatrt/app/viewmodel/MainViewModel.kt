package ai.chatrt.app.viewmodel

import ai.chatrt.app.models.*
import ai.chatrt.app.repository.ChatRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * Main ViewModel for ChatRT application
 * Manages connection state, video mode, and logging functionality
 */
class MainViewModel(
    private val chatRepository: ChatRepository
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

    // Error state management
    private val _error = MutableStateFlow<ChatRtError?>(null)
    val error: StateFlow<ChatRtError?> = _error.asStateFlow()

    // Current call ID for monitoring
    private var currentCallId: String? = null

    init {
        // Observe connection state changes from repository
        viewModelScope.launch {
            chatRepository.observeConnectionState()
                .collect { state ->
                    _connectionState.value = state
                    addLog("Connection state changed to: $state", LogLevel.INFO)
                }
        }

        // Observe logs from repository
        viewModelScope.launch {
            chatRepository.observeLogs()
                .collect { newLogs ->
                    _logs.value = newLogs
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
                        val error = mapExceptionToChatRtError(exception)
                        _error.value = error
                        addLog("Failed to create call: ${exception.message}", LogLevel.ERROR)
                    }
                )
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.FAILED
                val error = mapExceptionToChatRtError(e)
                _error.value = error
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
                        val error = mapExceptionToChatRtError(exception)
                        _error.value = error
                        addLog("Error stopping connection: ${exception.message}", LogLevel.ERROR)
                    }
                )
            } catch (e: Exception) {
                val error = mapExceptionToChatRtError(e)
                _error.value = error
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
            val optimization = PlatformOptimization(
                recommendedVideoMode = VideoMode.AUDIO_ONLY,
                recommendedAudioQuality = AudioQuality.LOW,
                disableVideoPreview = true,
                reason = OptimizationReason.POOR_NETWORK
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
            val optimization = PlatformOptimization(
                recommendedVideoMode = VideoMode.AUDIO_ONLY,
                recommendedAudioQuality = AudioQuality.LOW,
                disableVideoPreview = true,
                reason = OptimizationReason.LOW_MEMORY
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
        _error.value = null
    }

    /**
     * Adds a log entry with current timestamp
     */
    private fun addLog(message: String, level: LogLevel = LogLevel.INFO) {
        val logEntry = LogEntry(
            timestamp = Clock.System.now().toEpochMilliseconds(),
            message = message,
            level = level
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
    private fun createCallRequest(): CallRequest {
        return CallRequest(
            sdp = "", // This will be filled by WebRTC implementation
            session = SessionConfig(
                type = "realtime",
                model = "gpt-realtime",
                instructions = "You are a helpful AI assistant. Respond naturally to voice conversations.",
                audio = AudioConfig(
                    input = AudioInputConfig(
                        noiseReduction = NoiseReductionConfig(type = "near_field")
                    ),
                    output = AudioOutputConfig(voice = "marin")
                )
            )
        )
    }

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
                val error = mapExceptionToChatRtError(exception)
                _error.value = error
                addLog("Failed to start connection monitoring: ${exception.message}", LogLevel.ERROR)
            }
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
        val optimization = PlatformOptimization(
            recommendedVideoMode = VideoMode.AUDIO_ONLY,
            recommendedAudioQuality = AudioQuality.LOW,
            disableVideoPreview = true,
            reason = OptimizationReason.LOW_BATTERY
        )
        _platformOptimization.value = optimization
        addLog("Suggesting power optimization due to low battery", LogLevel.WARNING)
    }

    /**
     * Maps exceptions to ChatRtError types
     */
    private fun mapExceptionToChatRtError(exception: Throwable): ChatRtError {
        return when {
            exception.message?.contains("network", ignoreCase = true) == true ||
            exception.message?.contains("connection", ignoreCase = true) == true ||
            exception.message?.contains("host", ignoreCase = true) == true -> ChatRtError.NetworkError
            exception.message?.contains("permission", ignoreCase = true) == true ||
            exception.message?.contains("security", ignoreCase = true) == true -> ChatRtError.PermissionDenied
            else -> ChatRtError.ApiError(0, exception.message ?: "Unknown error")
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up any resources
        viewModelScope.launch {
            if (_connectionState.value == ConnectionState.CONNECTED) {
                chatRepository.stopConnection()
            }
        }
    }
}