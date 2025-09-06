package com.chatrt.android.data.model

/**
 * Represents a log entry with timestamp and message
 */
data class LogEntry(
    val timestamp: Long,
    val message: String,
    val level: LogLevel = LogLevel.INFO
)

enum class LogLevel {
    DEBUG, INFO, WARNING, ERROR
}