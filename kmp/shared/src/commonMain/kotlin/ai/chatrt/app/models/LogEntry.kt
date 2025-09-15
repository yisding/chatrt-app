package ai.chatrt.app.models

import ai.chatrt.app.logging.LogLevel
import kotlinx.serialization.Serializable

/**
 * Represents a lightweight log entry for repository/UI use.
 */
@Serializable
data class LogEntry(
    val timestamp: Long,
    val message: String,
    val level: LogLevel = LogLevel.INFO,
)
