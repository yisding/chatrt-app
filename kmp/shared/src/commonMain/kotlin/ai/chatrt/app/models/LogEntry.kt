package ai.chatrt.app.models

import kotlinx.serialization.Serializable

/**
 * Represents a log entry with timestamp and message
 */
@Serializable
data class LogEntry(
    val timestamp: Long,
    val message: String,
    val level: LogLevel = LogLevel.INFO,
)

/**
 * Log levels for categorizing log entries
 */
@Serializable
enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
}
