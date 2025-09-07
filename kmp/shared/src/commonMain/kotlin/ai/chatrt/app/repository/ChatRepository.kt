package ai.chatrt.app.repository

import ai.chatrt.app.models.CallRequest
import ai.chatrt.app.models.CallResponse
import ai.chatrt.app.models.ConnectionState
import ai.chatrt.app.models.LogEntry
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for ChatRT functionality
 * Handles WebRTC connection management and API communication
 */
interface ChatRepository {
    
    /**
     * Creates a new call with the ChatRT backend
     * @param callRequest The call request containing SDP offer and session config
     * @return Result containing the call response with SDP answer
     */
    suspend fun createCall(callRequest: CallRequest): Result<CallResponse>
    
    /**
     * Observes the current connection state
     * @return Flow of connection state changes
     */
    fun observeConnectionState(): Flow<ConnectionState>
    
    /**
     * Observes real-time logs from the connection
     * @return Flow of log entries with timestamps
     */
    fun observeLogs(): Flow<List<LogEntry>>
    
    /**
     * Starts monitoring the connection for the given call ID
     * @param callId The ID of the call to monitor
     */
    suspend fun startConnectionMonitoring(callId: String): Result<Unit>
    
    /**
     * Stops the current connection and cleans up resources
     */
    suspend fun stopConnection(): Result<Unit>
    
    /**
     * Checks if there's an active connection
     * @return true if connected, false otherwise
     */
    fun isConnected(): Boolean
    
    /**
     * Gets the current connection state
     * @return Current connection state
     */
    fun getCurrentConnectionState(): ConnectionState
}