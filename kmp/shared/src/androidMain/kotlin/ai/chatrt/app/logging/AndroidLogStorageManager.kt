@file:OptIn(kotlin.time.ExperimentalTime::class)

package ai.chatrt.app.logging

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream
import kotlin.time.Duration.Companion.days
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Android-specific implementation of LogStorageManager
 * Uses internal storage for log persistence
 */
@OptIn(ExperimentalUuidApi::class)
class AndroidLogStorageManager(
    private val context: Context,
    private val config: LogStorageConfig = LogStorageConfig(),
) : LogStorageManager {
    private val json =
        Json {
            prettyPrint = false
            ignoreUnknownKeys = true
        }

    private val logsDir = File(context.filesDir, "logs")
    private val archiveDir = File(logsDir, "archives")
    private val currentLogFile = File(logsDir, "current_logs.json")

    init {
        logsDir.mkdirs()
        archiveDir.mkdirs()
    }

    override suspend fun saveLogs(logs: List<LogEntry>): Unit =
        withContext(Dispatchers.IO) {
            try {
                val serializedLogs =
                    logs.map { entry ->
                        mapOf(
                            "id" to entry.id,
                            "timestamp" to entry.timestamp.toString(),
                            "level" to entry.level.name,
                            "category" to entry.category.name,
                            "tag" to entry.tag,
                            "message" to entry.message,
                            "throwable" to entry.throwable?.message,
                            "metadata" to entry.metadata,
                        )
                    }

                val jsonString = json.encodeToString(serializedLogs)
                currentLogFile.writeText(jsonString)

                // Check if we need to archive old logs
                if (getStorageSize() > config.maxStorageSize) {
                    archiveOldLogs()
                }
            } catch (e: Exception) {
                // Log to Android system log as fallback
                android.util.Log.e("LogStorage", "Failed to save logs", e)
            }
        }

    override suspend fun loadLogs(): List<LogEntry> =
        withContext(Dispatchers.IO) {
            try {
                if (!currentLogFile.exists()) {
                    return@withContext emptyList()
                }

                val jsonString = currentLogFile.readText()
                val serializedLogs = json.decodeFromString<List<Map<String, Any?>>>(jsonString)

                serializedLogs.mapNotNull { logMap ->
                    try {
                        LogEntry(
                            id = logMap["id"] as String,
                            timestamp = Instant.parse(logMap["timestamp"] as String),
                            level = LogLevel.valueOf(logMap["level"] as String),
                            category = LogCategory.valueOf(logMap["category"] as String),
                            tag = logMap["tag"] as String,
                            message = logMap["message"] as String,
                            throwable = (logMap["throwable"] as String?)?.let { RuntimeException(it) },
                            metadata = (logMap["metadata"] as? Map<String, Any>) ?: emptyMap(),
                        )
                    } catch (e: Exception) {
                        android.util.Log.w("LogStorage", "Failed to deserialize log entry", e)
                        null
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LogStorage", "Failed to load logs", e)
                emptyList()
            }
        }

    override suspend fun deleteLogs(): Unit =
        withContext(Dispatchers.IO) {
            try {
                currentLogFile.delete()
            } catch (e: Exception) {
                android.util.Log.e("LogStorage", "Failed to delete logs", e)
            }
        }

    override suspend fun getStorageSize(): Long =
        withContext(Dispatchers.IO) {
            try {
                var totalSize = 0L
                logsDir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        totalSize += file.length()
                    }
                }
                totalSize
            } catch (e: Exception) {
                android.util.Log.e("LogStorage", "Failed to calculate storage size", e)
                0L
            }
        }

    override suspend fun archiveLogs(beforeTimestamp: Instant): String =
        withContext(Dispatchers.IO) {
            try {
                val logs = loadLogs()
                val cutoff = beforeTimestamp.toEpochMilliseconds()
                val logsToArchive = logs.filter { it.timestamp.toEpochMilliseconds() < cutoff }
                val remainingLogs = logs.filter { it.timestamp.toEpochMilliseconds() >= cutoff }

                if (logsToArchive.isEmpty()) {
                    return@withContext ""
                }

                val archiveId = Uuid.random().toString()
                val archiveFile = File(archiveDir, "$archiveId.json.gz")

                // Compress and save archived logs
                val serializedLogs =
                    json.encodeToString(
                        logsToArchive.map { entry ->
                            mapOf(
                                "id" to entry.id,
                                "timestamp" to entry.timestamp.toString(),
                                "level" to entry.level.name,
                                "category" to entry.category.name,
                                "tag" to entry.tag,
                                "message" to entry.message,
                                "throwable" to entry.throwable?.message,
                                "metadata" to entry.metadata,
                            )
                        },
                    )

                if (config.compressionEnabled) {
                    GZIPOutputStream(FileOutputStream(archiveFile)).use { gzipOut ->
                        gzipOut.write(serializedLogs.toByteArray())
                    }
                } else {
                    archiveFile.writeText(serializedLogs)
                }

                // Save remaining logs
                saveLogs(remainingLogs)

                // Clean up old archives if needed
                cleanupOldArchives()

                archiveId
            } catch (e: Exception) {
                android.util.Log.e("LogStorage", "Failed to archive logs", e)
                ""
            }
        }

    override suspend fun getArchivedLogs(): List<String> =
        withContext(Dispatchers.IO) {
            try {
                archiveDir.listFiles()?.map { it.nameWithoutExtension } ?: emptyList()
            } catch (e: Exception) {
                android.util.Log.e("LogStorage", "Failed to get archived logs", e)
                emptyList()
            }
        }

    override suspend fun deleteArchivedLogs(archiveId: String): Unit =
        withContext(Dispatchers.IO) {
            try {
                val archiveFile = File(archiveDir, "$archiveId.json.gz")
                archiveFile.delete()
            } catch (e: Exception) {
                android.util.Log.e("LogStorage", "Failed to delete archived logs", e)
            }
        }

    private suspend fun archiveOldLogs() {
        val millisOffset = config.autoArchiveAfterDays * 24L * 60 * 60 * 1000
        val nowMs = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val cutoffTime = kotlinx.datetime.Instant.fromEpochMilliseconds(nowMs - millisOffset)
        archiveLogs(cutoffTime)
    }

    private suspend fun cleanupOldArchives() {
        try {
            val archives = archiveDir.listFiles()?.sortedByDescending { it.lastModified() }
            if (archives != null && archives.size > config.maxArchives) {
                archives.drop(config.maxArchives).forEach { it.delete() }
            }
        } catch (e: Exception) {
            android.util.Log.e("LogStorage", "Failed to cleanup old archives", e)
        }
    }
}
