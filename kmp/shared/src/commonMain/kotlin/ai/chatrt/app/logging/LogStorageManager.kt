package ai.chatrt.app.logging

import kotlinx.datetime.Instant

/**
 * Interface for managing log storage and persistence
 */
interface LogStorageManager {
    suspend fun saveLogs(logs: List<LogEntry>)

    suspend fun loadLogs(): List<LogEntry>

    suspend fun deleteLogs()

    suspend fun getStorageSize(): Long

    suspend fun archiveLogs(beforeTimestamp: Instant): String

    suspend fun getArchivedLogs(): List<String>

    suspend fun deleteArchivedLogs(archiveId: String)
}

/**
 * Configuration for log storage
 */
data class LogStorageConfig(
    // 10MB
    val maxStorageSize: Long = 10 * 1024 * 1024,
    val maxArchives: Int = 5,
    val autoArchiveAfterDays: Int = 7,
    val compressionEnabled: Boolean = true,
)

/**
 * Log file metadata
 */
data class LogArchive(
    val id: String,
    val timestamp: Instant,
    val size: Long,
    val entryCount: Int,
    val compressed: Boolean,
)
