package ai.chatrt.app.logging

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Default implementation of the Logger interface
 * Provides structured logging with rotation and export capabilities
 */
@OptIn(ExperimentalUuidApi::class)
class ChatRtLogger(
    private val maxLogEntries: Int = 1000,
    private val rotationThreshold: Int = 800,
) : Logger {
    private val logEntries = mutableListOf<LogEntry>()
    private val logFlow = MutableSharedFlow<List<LogEntry>>(replay = 1)
    private val mutex = Mutex()

    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

    init {
        // Initialize with empty log list
        logFlow.tryEmit(emptyList())
    }

    override fun debug(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        log(LogLevel.DEBUG, LogCategory.GENERAL, tag, message, throwable)
    }

    override fun info(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        log(LogLevel.INFO, LogCategory.GENERAL, tag, message, throwable)
    }

    override fun warning(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        log(LogLevel.WARNING, LogCategory.GENERAL, tag, message, throwable)
    }

    override fun error(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        log(LogLevel.ERROR, LogCategory.GENERAL, tag, message, throwable)
    }

    override fun logWebRtcEvent(event: WebRtcEvent) {
        val message = "WebRTC Event: ${event.type.name}"
        val metadata =
            mapOf(
                "connectionId" to event.connectionId,
                "eventType" to event.type.name,
                "details" to event.details,
            )

        log(
            level = LogLevel.INFO,
            category = LogCategory.WEBRTC,
            tag = "WebRTC",
            message = message,
            metadata = metadata,
        )
    }

    override fun logConnectionDiagnostic(diagnostic: ConnectionDiagnostic) {
        val message = "${diagnostic.type.name}: ${diagnostic.value}${diagnostic.unit?.let { " $it" } ?: ""}"
        val metadata =
            mapOf(
                "connectionId" to diagnostic.connectionId,
                "diagnosticType" to diagnostic.type.name,
                "value" to diagnostic.value,
                "unit" to (diagnostic.unit ?: ""),
            )

        log(
            level = LogLevel.DEBUG,
            category = LogCategory.NETWORK,
            tag = "Diagnostics",
            message = message,
            metadata = metadata,
        )
    }

    override fun getLogs(): Flow<List<LogEntry>> = logFlow.asSharedFlow()

    override fun getLogsByLevel(level: LogLevel): Flow<List<LogEntry>> =
        flow {
            logFlow.collect { logs ->
                emit(logs.filter { it.level.priority >= level.priority })
            }
        }

    override fun getLogsByCategory(category: LogCategory): Flow<List<LogEntry>> =
        flow {
            logFlow.collect { logs ->
                emit(logs.filter { it.category == category })
            }
        }

    override suspend fun exportLogs(): String =
        mutex.withLock {
            val exportData =
                mapOf(
                    "exportTimestamp" to Clock.System.now().toString(),
                    "totalEntries" to logEntries.size,
                    "logs" to
                        logEntries.map { entry ->
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

            json.encodeToString(exportData)
        }

    override suspend fun clearLogs() {
        mutex.withLock {
            logEntries.clear()
            logFlow.tryEmit(logEntries.toList())
        }
    }

    override suspend fun rotateLogs() {
        mutex.withLock {
            if (logEntries.size >= rotationThreshold) {
                // Keep only the most recent entries
                val keepCount = maxLogEntries / 2
                val toKeep = logEntries.takeLast(keepCount)
                logEntries.clear()
                logEntries.addAll(toKeep)

                // Log the rotation event
                val rotationEntry =
                    LogEntry(
                        id = Uuid.random().toString(),
                        timestamp = Clock.System.now(),
                        level = LogLevel.INFO,
                        category = LogCategory.SYSTEM,
                        tag = "LogRotation",
                        message = "Log rotation completed. Kept $keepCount entries.",
                        metadata = mapOf("keptEntries" to keepCount),
                    )
                logEntries.add(rotationEntry)

                logFlow.tryEmit(logEntries.toList())
            }
        }
    }

    private fun log(
        level: LogLevel,
        category: LogCategory,
        tag: String,
        message: String,
        throwable: Throwable? = null,
        metadata: Map<String, Any> = emptyMap(),
    ) {
        val entry =
            LogEntry(
                id = Uuid.random().toString(),
                timestamp = Clock.System.now(),
                level = level,
                category = category,
                tag = tag,
                message = message,
                throwable = throwable,
                metadata = metadata,
            )

        // Use a coroutine to handle the async operations
        GlobalScope.launch {
            mutex.withLock {
                logEntries.add(entry)

                // Auto-rotate if we exceed the max entries
                if (logEntries.size > maxLogEntries) {
                    rotateLogs()
                } else {
                    logFlow.tryEmit(logEntries.toList())
                }
            }
        }

        // Also log to platform-specific logging system
        logToPlatform(level, tag, message, throwable)
    }

    /**
     * Platform-specific logging - to be implemented in expect/actual
     */
    private fun logToPlatform(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        // This will be implemented in platform-specific code
        println("[$tag] ${level.name}: $message${throwable?.let { " - ${it.message}" } ?: ""}")
    }
}

// Extension functions for easier logging
fun Logger.debugWebRtc(
    message: String,
    connectionId: String = "",
    details: Map<String, Any> = emptyMap(),
) {
    debug("WebRTC", message)
    if (connectionId.isNotEmpty()) {
        logWebRtcEvent(WebRtcEvent(WebRtcEventType.CONNECTION_CREATED, connectionId, details))
    }
}

fun Logger.infoNetwork(message: String) {
    info("Network", message)
}

fun Logger.warningAudio(
    message: String,
    throwable: Throwable? = null,
) {
    warning("Audio", message, throwable)
}

fun Logger.errorVideo(
    message: String,
    throwable: Throwable? = null,
) {
    error("Video", message, throwable)
}
