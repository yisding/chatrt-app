package ai.chatrt.app.repository

import ai.chatrt.app.models.CallRequest
import ai.chatrt.app.models.CallResponse
import ai.chatrt.app.models.ConnectionState
import ai.chatrt.app.models.LogEntry
import ai.chatrt.app.models.LogLevel
import ai.chatrt.app.models.VideoMode
import ai.chatrt.app.network.ChatRtApiService
import ai.chatrt.app.platform.IceConnectionState
import ai.chatrt.app.platform.WebRtcManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * Implementation of ChatRepository using ChatRtApiService and WebRtcManager
 * Manages WebRTC connection state and API communication with real-time monitoring
 */
class ChatRepositoryImpl(
    private val apiService: ChatRtApiService,
    private val webRtcManager: WebRtcManager,
) : ChatRepository {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    private val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private var currentCallId: String? = null
    private var isWebRtcInitialized = false
    private var connectionMonitoringActive = false

    init {
        // Observe WebRTC connection state changes
        webRtcManager
            .observeConnectionState()
            .onEach { webRtcState ->
                handleWebRtcStateChange(webRtcState)
            }.launchIn(repositoryScope)

        // Observe ICE connection state changes for detailed logging
        webRtcManager
            .observeIceConnectionState()
            .onEach { iceState ->
                handleIceConnectionStateChange(iceState)
            }.launchIn(repositoryScope)
    }

    override suspend fun createCall(callRequest: CallRequest): Result<CallResponse> {
        addLog("Creating call with session config: ${callRequest.session}", LogLevel.INFO)
        _connectionState.value = ConnectionState.CONNECTING

        return try {
            // Initialize WebRTC if not already done
            if (!isWebRtcInitialized) {
                addLog("Initializing WebRTC manager", LogLevel.INFO)
                webRtcManager.initialize()
                isWebRtcInitialized = true
            }

            // Create SDP offer using WebRTC manager
            addLog("Creating SDP offer", LogLevel.INFO)
            val sdpOffer = webRtcManager.createOffer()
            addLog("SDP offer created successfully", LogLevel.DEBUG)

            // Create call request with actual SDP offer
            val requestWithSdp = callRequest.copy(sdp = sdpOffer)

            // Send call request to backend
            apiService.createCall(requestWithSdp).fold(
                onSuccess = { response ->
                    currentCallId = response.callId
                    addLog("Call created successfully with ID: ${response.callId}", LogLevel.INFO)

                    // Set remote SDP description from response
                    addLog("Setting remote SDP description", LogLevel.DEBUG)
                    webRtcManager.setRemoteDescription(response.sdpAnswer)

                    // Add local media stream based on session configuration
                    val videoMode = determineVideoModeFromSession(callRequest.session)
                    addLog("Adding local media stream for mode: $videoMode", LogLevel.INFO)
                    webRtcManager.addLocalStream(videoMode)

                    Result.success(response)
                },
                onFailure = { error ->
                    addLog("Failed to create call: ${error.message}", LogLevel.ERROR)
                    _connectionState.value = ConnectionState.FAILED
                    Result.failure(error)
                },
            )
        } catch (e: Exception) {
            addLog("Exception during call creation: ${e.message}", LogLevel.ERROR)
            _connectionState.value = ConnectionState.FAILED
            Result.failure(e)
        }
    }

    override fun observeConnectionState(): Flow<ConnectionState> = connectionState

    override fun observeLogs(): Flow<List<LogEntry>> = logs

    override suspend fun startConnectionMonitoring(callId: String): Result<Unit> {
        addLog("Starting connection monitoring for call: $callId", LogLevel.INFO)

        return try {
            // Start API-level monitoring
            val apiResult = apiService.startCallMonitoring(callId)

            apiResult.fold(
                onSuccess = {
                    connectionMonitoringActive = true
                    addLog("Connection monitoring started successfully", LogLevel.INFO)

                    // Start periodic connection quality monitoring
                    startConnectionQualityMonitoring()

                    Result.success(Unit)
                },
                onFailure = { error ->
                    addLog("Failed to start connection monitoring: ${error.message}", LogLevel.ERROR)
                    Result.failure(error)
                },
            )
        } catch (e: Exception) {
            addLog("Exception during connection monitoring setup: ${e.message}", LogLevel.ERROR)
            Result.failure(e)
        }
    }

    override suspend fun stopConnection(): Result<Unit> {
        addLog("Stopping connection", LogLevel.INFO)

        return try {
            // Stop connection monitoring
            connectionMonitoringActive = false

            // Remove local media stream
            addLog("Removing local media stream", LogLevel.DEBUG)
            webRtcManager.removeLocalStream()

            // Close WebRTC connection
            addLog("Closing WebRTC connection", LogLevel.DEBUG)
            webRtcManager.close()

            // Reset state
            currentCallId = null
            _connectionState.value = ConnectionState.DISCONNECTED

            addLog("Connection stopped successfully", LogLevel.INFO)
            Result.success(Unit)
        } catch (e: Exception) {
            addLog("Error stopping connection: ${e.message}", LogLevel.ERROR)
            _connectionState.value = ConnectionState.FAILED
            Result.failure(e)
        }
    }

    override fun isConnected(): Boolean = _connectionState.value == ConnectionState.CONNECTED

    override fun getCurrentConnectionState(): ConnectionState = _connectionState.value

    /**
     * Adds a log entry with timestamp
     * @param message The log message
     * @param level The log level
     */
    private fun addLog(
        message: String,
        level: LogLevel,
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
     * Handles WebRTC connection state changes
     */
    private fun handleWebRtcStateChange(webRtcState: ConnectionState) {
        addLog("WebRTC connection state changed to: $webRtcState", LogLevel.INFO)
        _connectionState.value = webRtcState
    }

    /**
     * Handles ICE connection state changes for detailed logging
     */
    private fun handleIceConnectionStateChange(iceState: IceConnectionState) {
        val message =
            when (iceState) {
                IceConnectionState.NEW -> "ICE connection initialized"
                IceConnectionState.CHECKING -> "ICE connection checking candidates"
                IceConnectionState.CONNECTED -> "ICE connection established"
                IceConnectionState.COMPLETED -> "ICE connection completed"
                IceConnectionState.FAILED -> "ICE connection failed"
                IceConnectionState.DISCONNECTED -> "ICE connection disconnected"
                IceConnectionState.CLOSED -> "ICE connection closed"
            }

        val logLevel =
            when (iceState) {
                IceConnectionState.FAILED -> LogLevel.ERROR
                IceConnectionState.DISCONNECTED -> LogLevel.WARNING
                else -> LogLevel.DEBUG
            }

        addLog(message, logLevel)

        // Update main connection state based on ICE state
        when (iceState) {
            IceConnectionState.FAILED -> {
                if (_connectionState.value == ConnectionState.CONNECTING) {
                    _connectionState.value = ConnectionState.FAILED
                }
            }
            IceConnectionState.DISCONNECTED -> {
                if (_connectionState.value == ConnectionState.CONNECTED) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    addLog("Connection lost, attempting to reconnect...", LogLevel.WARNING)
                    // Could trigger reconnection logic here
                }
            }
            IceConnectionState.CONNECTED, IceConnectionState.COMPLETED -> {
                if (_connectionState.value == ConnectionState.CONNECTING) {
                    _connectionState.value = ConnectionState.CONNECTED
                }
            }
            else -> { /* No action needed for other states */ }
        }
    }

    /**
     * Starts periodic connection quality monitoring
     */
    private suspend fun startConnectionQualityMonitoring() {
        // Launch a coroutine to periodically check connection quality
        repositoryScope.launch {
            while (connectionMonitoringActive && isConnected()) {
                try {
                    // Get video and audio statistics
                    val videoStats = webRtcManager.getVideoStats()
                    val audioStats = webRtcManager.getAudioStats()

                    videoStats?.let { stats ->
                        addLog(
                            "Video stats - Frames: ${stats.framesSent}/${stats.framesReceived}, " +
                                "Bytes: ${stats.bytesSent}/${stats.bytesReceived}, " +
                                "Resolution: ${stats.resolution.width}x${stats.resolution.height}@${stats.frameRate}fps",
                            LogLevel.DEBUG,
                        )
                    }

                    audioStats?.let { stats ->
                        addLog(
                            "Audio stats - Packets: ${stats.packetsSent}/${stats.packetsReceived}, " +
                                "Bytes: ${stats.bytesSent}/${stats.bytesReceived}, " +
                                "Level: ${stats.audioLevel}",
                            LogLevel.DEBUG,
                        )
                    }

                    // Wait 10 seconds before next check
                    delay(10_000)
                } catch (e: Exception) {
                    addLog("Error during connection quality monitoring: ${e.message}", LogLevel.WARNING)
                    delay(30_000) // Wait longer on error
                }
            }
        }
    }

    /**
     * Determines video mode from session configuration
     */
    private fun determineVideoModeFromSession(session: Any): VideoMode {
        // This would parse the session configuration to determine the video mode
        // For now, return a default based on session type
        return VideoMode.AUDIO_ONLY // Default to audio-only for safety
    }

    /**
     * Switches camera during an active call
     */
    override suspend fun switchCamera(): Result<Unit> =
        try {
            if (isConnected()) {
                addLog("Switching camera", LogLevel.INFO)
                webRtcManager.switchCamera()
                addLog("Camera switched successfully", LogLevel.INFO)
                Result.success(Unit)
            } else {
                addLog("Cannot switch camera: not connected", LogLevel.WARNING)
                Result.failure(IllegalStateException("Not connected"))
            }
        } catch (e: Exception) {
            addLog("Error switching camera: ${e.message}", LogLevel.ERROR)
            Result.failure(e)
        }

    /**
     * Updates video mode during an active call
     */
    override suspend fun updateVideoMode(videoMode: VideoMode): Result<Unit> =
        try {
            if (isConnected()) {
                addLog("Updating video mode to: $videoMode", LogLevel.INFO)

                // Remove current stream and add new one with updated mode
                webRtcManager.removeLocalStream()
                webRtcManager.addLocalStream(videoMode)

                addLog("Video mode updated successfully", LogLevel.INFO)
                Result.success(Unit)
            } else {
                addLog("Cannot update video mode: not connected", LogLevel.WARNING)
                Result.failure(IllegalStateException("Not connected"))
            }
        } catch (e: Exception) {
            addLog("Error updating video mode: ${e.message}", LogLevel.ERROR)
            Result.failure(e)
        }

    /**
     * Clears all logs
     */
    fun clearLogs() {
        _logs.value = emptyList()
        addLog("Logs cleared", LogLevel.INFO)
    }

    /**
     * Cleanup resources when repository is no longer needed
     */
    fun cleanup() {
        connectionMonitoringActive = false
        repositoryScope.cancel()
    }
}
