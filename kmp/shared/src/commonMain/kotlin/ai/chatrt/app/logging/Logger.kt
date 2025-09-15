package ai.chatrt.app.logging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Comprehensive logging system for ChatRT application
 * Supports structured logging with different levels and categories
 */
interface Logger {
    fun debug(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )

    fun info(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )

    fun warning(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )

    fun error(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )

    fun logWebRtcEvent(event: WebRtcEvent)

    fun logConnectionDiagnostic(diagnostic: ConnectionDiagnostic)

    fun getLogs(): Flow<List<LogEntry>>

    fun getLogsByLevel(level: LogLevel): Flow<List<LogEntry>>

    fun getLogsByCategory(category: LogCategory): Flow<List<LogEntry>>

    suspend fun exportLogs(): String

    suspend fun clearLogs()

    suspend fun rotateLogs()
}

/**
 * Log levels for structured logging
 */
enum class LogLevel(
    val priority: Int,
) {
    DEBUG(0),
    INFO(1),
    WARNING(2),
    ERROR(3),
}

/**
 * Log categories for organized logging
 */
enum class LogCategory {
    WEBRTC,
    NETWORK,
    AUDIO,
    VIDEO,
    PERMISSIONS,
    LIFECYCLE,
    UI,
    SYSTEM,
    API,
    GENERAL,
}

/**
 * Structured log entry with metadata
 */
data class LogEntry(
    val id: String,
    val timestamp: Instant,
    val level: LogLevel,
    val category: LogCategory,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
    val metadata: Map<String, Any> = emptyMap(),
)

/**
 * WebRTC-specific event logging
 */
data class WebRtcEvent(
    val type: WebRtcEventType,
    val connectionId: String,
    val details: Map<String, Any> = emptyMap(),
    val timestamp: Instant = Clock.System.now(),
)

enum class WebRtcEventType {
    CONNECTION_CREATED,
    CONNECTION_CONNECTING,
    CONNECTION_CONNECTED,
    CONNECTION_DISCONNECTED,
    CONNECTION_FAILED,
    CONNECTION_CLOSED,
    ICE_CANDIDATE_ADDED,
    ICE_GATHERING_COMPLETE,
    SDP_OFFER_CREATED,
    SDP_ANSWER_RECEIVED,
    MEDIA_STREAM_ADDED,
    MEDIA_STREAM_REMOVED,
    DATA_CHANNEL_OPENED,
    DATA_CHANNEL_CLOSED,
    STATS_UPDATED,
}

/**
 * Connection diagnostic information
 */
data class ConnectionDiagnostic(
    val connectionId: String,
    val type: DiagnosticType,
    val value: String,
    val unit: String? = null,
    val timestamp: Instant = Clock.System.now(),
)

enum class DiagnosticType {
    BANDWIDTH_UP,
    BANDWIDTH_DOWN,
    PACKET_LOSS,
    ROUND_TRIP_TIME,
    JITTER,
    AUDIO_LEVEL,
    VIDEO_RESOLUTION,
    FRAME_RATE,
    CPU_USAGE,
    MEMORY_USAGE,
    NETWORK_TYPE,
}
