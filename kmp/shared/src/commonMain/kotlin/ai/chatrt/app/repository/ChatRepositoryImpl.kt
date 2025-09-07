package ai.chatrt.app.repository

import ai.chatrt.app.models.CallRequest
import ai.chatrt.app.models.CallResponse
import ai.chatrt.app.models.ConnectionState
import ai.chatrt.app.models.LogEntry
import ai.chatrt.app.models.LogLevel
import ai.chatrt.app.network.ChatRtApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

/**
 * Implementation of ChatRepository using ChatRtApiService
 * Manages WebRTC connection state and API communication
 */
class ChatRepositoryImpl(
    private val apiService: ChatRtApiService
) : ChatRepository {
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    private val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()
    
    private var currentCallId: String? = null
    
    override suspend fun createCall(callRequest: CallRequest): Result<CallResponse> {
        addLog("Creating call with video mode: ${callRequest.session}", LogLevel.INFO)
        _connectionState.value = ConnectionState.CONNECTING
        
        return apiService.createCall(callRequest).fold(
            onSuccess = { response ->
                currentCallId = response.callId
                addLog("Call created successfully with ID: ${response.callId}", LogLevel.INFO)
                _connectionState.value = ConnectionState.CONNECTED
                Result.success(response)
            },
            onFailure = { error ->
                addLog("Failed to create call: ${error.message}", LogLevel.ERROR)
                _connectionState.value = ConnectionState.FAILED
                Result.failure(error)
            }
        )
    }
    
    override fun observeConnectionState(): Flow<ConnectionState> {
        return connectionState
    }
    
    override fun observeLogs(): Flow<List<LogEntry>> {
        return logs
    }
    
    override suspend fun startConnectionMonitoring(callId: String): Result<Unit> {
        addLog("Starting connection monitoring for call: $callId", LogLevel.INFO)
        
        return apiService.startCallMonitoring(callId).fold(
            onSuccess = {
                addLog("Connection monitoring started successfully", LogLevel.INFO)
                Result.success(Unit)
            },
            onFailure = { error ->
                addLog("Failed to start connection monitoring: ${error.message}", LogLevel.ERROR)
                Result.failure(error)
            }
        )
    }
    
    override suspend fun stopConnection(): Result<Unit> {
        addLog("Stopping connection", LogLevel.INFO)
        
        return try {
            currentCallId = null
            _connectionState.value = ConnectionState.DISCONNECTED
            addLog("Connection stopped successfully", LogLevel.INFO)
            Result.success(Unit)
        } catch (e: Exception) {
            addLog("Error stopping connection: ${e.message}", LogLevel.ERROR)
            Result.failure(e)
        }
    }
    
    override fun isConnected(): Boolean {
        return _connectionState.value == ConnectionState.CONNECTED
    }
    
    override fun getCurrentConnectionState(): ConnectionState {
        return _connectionState.value
    }
    
    /**
     * Adds a log entry with timestamp
     * @param message The log message
     * @param level The log level
     */
    private fun addLog(message: String, level: LogLevel) {
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
     * Simulates connection state changes for testing
     * This would be replaced by actual WebRTC state monitoring
     */
    fun simulateConnectionStateChange(newState: ConnectionState) {
        _connectionState.value = newState
        addLog("Connection state changed to: $newState", LogLevel.INFO)
    }
    
    /**
     * Clears all logs
     */
    fun clearLogs() {
        _logs.value = emptyList()
        addLog("Logs cleared", LogLevel.INFO)
    }
}