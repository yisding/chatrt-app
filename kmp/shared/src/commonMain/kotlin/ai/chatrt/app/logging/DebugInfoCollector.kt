@file:OptIn(kotlin.time.ExperimentalTime::class)

package ai.chatrt.app.logging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Collects and formats debug information for display and export
 */
class DebugInfoCollector(
    private val logger: Logger,
    private val webRtcEventLogger: WebRtcEventLogger,
) {
    data class DebugInfo(
        val timestamp: Instant,
        val systemInfo: SystemInfo,
        val connectionInfo: ConnectionInfo,
        val recentLogs: List<LogEntry>,
        val recentEvents: List<WebRtcEvent>,
        val recentDiagnostics: List<ConnectionDiagnostic>,
    )

    data class SystemInfo(
        val platform: String,
        val version: String,
        val memoryUsage: String,
        val cpuUsage: String,
        val networkType: String,
        val batteryLevel: String,
    )

    data class ConnectionInfo(
        val activeConnections: Int,
        val connectionStates: Map<String, String>,
        val lastConnectionError: String?,
        val totalBytesTransferred: Long,
        val averageLatency: Double,
    )

    fun collectDebugInfo(): Flow<DebugInfo> =
        combine(
            logger.getLogs(),
            webRtcEventLogger.eventsFlow,
            webRtcEventLogger.diagnosticsFlow,
        ) { logs, events, diagnostics ->

            val recentLogs = logs.takeLast(50)
            val recentEvents = events.takeLast(20)
            val recentDiagnostics = diagnostics.takeLast(30)

            DebugInfo(
                timestamp = Clock.System.now(),
                systemInfo = collectSystemInfo(),
                connectionInfo = collectConnectionInfo(recentEvents, recentDiagnostics),
                recentLogs = recentLogs,
                recentEvents = recentEvents,
                recentDiagnostics = recentDiagnostics,
            )
        }

    suspend fun exportDebugInfo(): String {
        val debugInfo = collectDebugInfo()

        // This would need to be collected synchronously for export
        // For now, return a formatted string with available information
        return buildString {
            appendLine("=== ChatRT Debug Information ===")
            appendLine("Generated: ${Clock.System.now()}")
            appendLine()

            appendLine("=== System Information ===")
            val systemInfo = collectSystemInfo()
            appendLine("Platform: ${systemInfo.platform}")
            appendLine("Version: ${systemInfo.version}")
            appendLine("Memory Usage: ${systemInfo.memoryUsage}")
            appendLine("CPU Usage: ${systemInfo.cpuUsage}")
            appendLine("Network Type: ${systemInfo.networkType}")
            appendLine("Battery Level: ${systemInfo.batteryLevel}")
            appendLine()

            appendLine("=== Recent Logs ===")
            val logs = logger.getLogs()
            // Note: This is a simplified version - in practice you'd need to collect this properly
            appendLine("(Recent logs would be included here)")
            appendLine()

            appendLine("=== WebRTC Events ===")
            appendLine("(Recent WebRTC events would be included here)")
            appendLine()

            appendLine("=== Connection Diagnostics ===")
            appendLine("(Recent diagnostics would be included here)")
        }
    }

    private fun collectSystemInfo(): SystemInfo =
        SystemInfo(
            platform = getPlatformName(),
            version = getAppVersion(),
            memoryUsage = getMemoryUsage(),
            cpuUsage = getCpuUsage(),
            networkType = getNetworkType(),
            batteryLevel = getBatteryLevel(),
        )

    private fun collectConnectionInfo(
        events: List<WebRtcEvent>,
        diagnostics: List<ConnectionDiagnostic>,
    ): ConnectionInfo {
        val connectionIds = events.map { it.connectionId }.distinct()
        val connectionStates = mutableMapOf<String, String>()

        // Determine current state of each connection based on recent events
        connectionIds.forEach { connectionId ->
            val connectionEvents = events.filter { it.connectionId == connectionId }
            val lastEvent = connectionEvents.lastOrNull()
            connectionStates[connectionId] =
                when (lastEvent?.type) {
                    WebRtcEventType.CONNECTION_CONNECTED -> "Connected"
                    WebRtcEventType.CONNECTION_CONNECTING -> "Connecting"
                    WebRtcEventType.CONNECTION_DISCONNECTED -> "Disconnected"
                    WebRtcEventType.CONNECTION_FAILED -> "Failed"
                    WebRtcEventType.CONNECTION_CLOSED -> "Closed"
                    else -> "Unknown"
                }
        }

        val lastError =
            events
                .filter { it.type == WebRtcEventType.CONNECTION_FAILED }
                .lastOrNull()
                ?.details
                ?.get("error") as? String

        val latencyDiagnostics =
            diagnostics
                .filter { it.type == DiagnosticType.ROUND_TRIP_TIME }
                .mapNotNull { it.value.toDoubleOrNull() }

        val averageLatency =
            if (latencyDiagnostics.isNotEmpty()) {
                latencyDiagnostics.average()
            } else {
                0.0
            }

        return ConnectionInfo(
            activeConnections = connectionStates.values.count { it == "Connected" },
            connectionStates = connectionStates,
            lastConnectionError = lastError,
            // Would need to be calculated from stats
            totalBytesTransferred = 0L,
            averageLatency = averageLatency,
        )
    }

    // Platform-specific implementations would be provided via expect/actual
    private fun getPlatformName(): String = "Unknown"

    private fun getAppVersion(): String = "1.0.0"

    private fun getMemoryUsage(): String = "Unknown"

    private fun getCpuUsage(): String = "Unknown"

    private fun getNetworkType(): String = "Unknown"

    private fun getBatteryLevel(): String = "Unknown"
}
