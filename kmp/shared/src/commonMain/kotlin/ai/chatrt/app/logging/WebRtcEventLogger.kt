package ai.chatrt.app.logging

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Specialized logger for WebRTC events and connection diagnostics
 */
class WebRtcEventLogger(
    private val logger: Logger,
) {
    private val diagnosticsList = mutableListOf<ConnectionDiagnostic>()
    private val _diagnosticsFlow = MutableSharedFlow<List<ConnectionDiagnostic>>(replay = 1)
    val diagnosticsFlow: SharedFlow<List<ConnectionDiagnostic>> = _diagnosticsFlow.asSharedFlow()

    private val eventsList = mutableListOf<WebRtcEvent>()
    private val _eventsFlow = MutableSharedFlow<List<WebRtcEvent>>(replay = 1)
    val eventsFlow: SharedFlow<List<WebRtcEvent>> = _eventsFlow.asSharedFlow()

    init {
        _diagnosticsFlow.tryEmit(emptyList())
        _eventsFlow.tryEmit(emptyList())
    }

    // Connection lifecycle events
    fun logConnectionCreated(
        connectionId: String,
        config: Map<String, Any> = emptyMap(),
    ) {
        val event =
            WebRtcEvent(
                type = WebRtcEventType.CONNECTION_CREATED,
                connectionId = connectionId,
                details = config,
            )
        logEvent(event)
    }

    fun logConnectionConnecting(connectionId: String) {
        val event =
            WebRtcEvent(
                type = WebRtcEventType.CONNECTION_CONNECTING,
                connectionId = connectionId,
            )
        logEvent(event)
    }

    fun logConnectionConnected(
        connectionId: String,
        stats: Map<String, Any> = emptyMap(),
    ) {
        val event =
            WebRtcEvent(
                type = WebRtcEventType.CONNECTION_CONNECTED,
                connectionId = connectionId,
                details = stats,
            )
        logEvent(event)
    }

    fun logConnectionDisconnected(
        connectionId: String,
        reason: String = "",
    ) {
        val event =
            WebRtcEvent(
                type = WebRtcEventType.CONNECTION_DISCONNECTED,
                connectionId = connectionId,
                details = mapOf("reason" to reason),
            )
        logEvent(event)
    }

    fun logConnectionFailed(
        connectionId: String,
        error: String,
    ) {
        val event =
            WebRtcEvent(
                type = WebRtcEventType.CONNECTION_FAILED,
                connectionId = connectionId,
                details = mapOf("error" to error),
            )
        logEvent(event)
    }

    fun logConnectionClosed(connectionId: String) {
        val event =
            WebRtcEvent(
                type = WebRtcEventType.CONNECTION_CLOSED,
                connectionId = connectionId,
            )
        logEvent(event)
    }

    // ICE events
    fun logIceCandidateAdded(
        connectionId: String,
        candidate: String,
        sdpMid: String?,
    ) {
        val event =
            WebRtcEvent(
                type = WebRtcEventType.ICE_CANDIDATE_ADDED,
                connectionId = connectionId,
                details =
                    mapOf(
                        "candidate" to candidate,
                        "sdpMid" to (sdpMid ?: ""),
                    ),
            )
        logEvent(event)
    }

    fun logIceGatheringComplete(
        connectionId: String,
        candidateCount: Int,
    ) {
        val event =
            WebRtcEvent(
                type = WebRtcEventType.ICE_GATHERING_COMPLETE,
                connectionId = connectionId,
                details = mapOf("candidateCount" to candidateCount),
            )
        logEvent(event)
    }

    // SDP events
    fun logSdpOfferCreated(
        connectionId: String,
        sdp: String,
    ) {
        val event =
            WebRtcEvent(
                type = WebRtcEventType.SDP_OFFER_CREATED,
                connectionId = connectionId,
                details = mapOf("sdpLength" to sdp.length),
            )
        logEvent(event)
    }

    fun logSdpAnswerReceived(
        connectionId: String,
        sdp: String,
    ) {
        val event =
            WebRtcEvent(
                type = WebRtcEventType.SDP_ANSWER_RECEIVED,
                connectionId = connectionId,
                details = mapOf("sdpLength" to sdp.length),
            )
        logEvent(event)
    }

    // Media events
    fun logMediaStreamAdded(
        connectionId: String,
        streamId: String,
        trackCount: Int,
    ) {
        val event =
            WebRtcEvent(
                type = WebRtcEventType.MEDIA_STREAM_ADDED,
                connectionId = connectionId,
                details =
                    mapOf(
                        "streamId" to streamId,
                        "trackCount" to trackCount,
                    ),
            )
        logEvent(event)
    }

    fun logMediaStreamRemoved(
        connectionId: String,
        streamId: String,
    ) {
        val event =
            WebRtcEvent(
                type = WebRtcEventType.MEDIA_STREAM_REMOVED,
                connectionId = connectionId,
                details = mapOf("streamId" to streamId),
            )
        logEvent(event)
    }

    // Data channel events
    fun logDataChannelOpened(
        connectionId: String,
        channelLabel: String,
    ) {
        val event =
            WebRtcEvent(
                type = WebRtcEventType.DATA_CHANNEL_OPENED,
                connectionId = connectionId,
                details = mapOf("channelLabel" to channelLabel),
            )
        logEvent(event)
    }

    fun logDataChannelClosed(
        connectionId: String,
        channelLabel: String,
    ) {
        val event =
            WebRtcEvent(
                type = WebRtcEventType.DATA_CHANNEL_CLOSED,
                connectionId = connectionId,
                details = mapOf("channelLabel" to channelLabel),
            )
        logEvent(event)
    }

    // Statistics logging
    fun logStatsUpdate(
        connectionId: String,
        stats: Map<String, Any>,
    ) {
        val event =
            WebRtcEvent(
                type = WebRtcEventType.STATS_UPDATED,
                connectionId = connectionId,
                details = stats,
            )
        logEvent(event)

        // Extract key diagnostics from stats
        extractDiagnosticsFromStats(connectionId, stats)
    }

    // Connection diagnostics
    fun logBandwidth(
        connectionId: String,
        uploadKbps: Double,
        downloadKbps: Double,
    ) {
        logDiagnostic(
            ConnectionDiagnostic(
                connectionId = connectionId,
                type = DiagnosticType.BANDWIDTH_UP,
                value = uploadKbps.toString(),
                unit = "kbps",
            ),
        )

        logDiagnostic(
            ConnectionDiagnostic(
                connectionId = connectionId,
                type = DiagnosticType.BANDWIDTH_DOWN,
                value = downloadKbps.toString(),
                unit = "kbps",
            ),
        )
    }

    fun logPacketLoss(
        connectionId: String,
        lossPercentage: Double,
    ) {
        logDiagnostic(
            ConnectionDiagnostic(
                connectionId = connectionId,
                type = DiagnosticType.PACKET_LOSS,
                value = lossPercentage.toString(),
                unit = "%",
            ),
        )
    }

    fun logRoundTripTime(
        connectionId: String,
        rttMs: Double,
    ) {
        logDiagnostic(
            ConnectionDiagnostic(
                connectionId = connectionId,
                type = DiagnosticType.ROUND_TRIP_TIME,
                value = rttMs.toString(),
                unit = "ms",
            ),
        )
    }

    fun logJitter(
        connectionId: String,
        jitterMs: Double,
    ) {
        logDiagnostic(
            ConnectionDiagnostic(
                connectionId = connectionId,
                type = DiagnosticType.JITTER,
                value = jitterMs.toString(),
                unit = "ms",
            ),
        )
    }

    fun logAudioLevel(
        connectionId: String,
        level: Double,
    ) {
        logDiagnostic(
            ConnectionDiagnostic(
                connectionId = connectionId,
                type = DiagnosticType.AUDIO_LEVEL,
                value = level.toString(),
                unit = "dB",
            ),
        )
    }

    fun logVideoResolution(
        connectionId: String,
        width: Int,
        height: Int,
    ) {
        logDiagnostic(
            ConnectionDiagnostic(
                connectionId = connectionId,
                type = DiagnosticType.VIDEO_RESOLUTION,
                value = "${width}x$height",
            ),
        )
    }

    fun logFrameRate(
        connectionId: String,
        fps: Double,
    ) {
        logDiagnostic(
            ConnectionDiagnostic(
                connectionId = connectionId,
                type = DiagnosticType.FRAME_RATE,
                value = fps.toString(),
                unit = "fps",
            ),
        )
    }

    private fun logEvent(event: WebRtcEvent) {
        logger.logWebRtcEvent(event)
        eventsList.add(event)
        // Keep only the last 100 events
        if (eventsList.size > 100) {
            eventsList.removeAt(0)
        }
        _eventsFlow.tryEmit(eventsList.toList())
    }

    private fun logDiagnostic(diagnostic: ConnectionDiagnostic) {
        logger.logConnectionDiagnostic(diagnostic)
        diagnosticsList.add(diagnostic)
        // Keep only the last 100 diagnostics
        if (diagnosticsList.size > 100) {
            diagnosticsList.removeAt(0)
        }
        _diagnosticsFlow.tryEmit(diagnosticsList.toList())
    }

    private fun extractDiagnosticsFromStats(
        connectionId: String,
        stats: Map<String, Any>,
    ) {
        // Extract common WebRTC statistics and convert to diagnostics
        stats["bytesReceived"]?.let { bytes ->
            // Calculate bandwidth if we have timing info
            stats["timestamp"]?.let { timestamp ->
                // This would need more sophisticated calculation with previous values
                // For now, just log the raw bytes
            }
        }

        stats["packetsLost"]?.let { lost ->
            stats["packetsReceived"]?.let { received ->
                val lostCount = (lost as? Number)?.toDouble() ?: 0.0
                val receivedCount = (received as? Number)?.toDouble() ?: 0.0
                val total = lostCount + receivedCount
                if (total > 0) {
                    val lossPercentage = (lostCount / total) * 100
                    logPacketLoss(connectionId, lossPercentage)
                }
            }
        }

        stats["roundTripTime"]?.let { rtt ->
            (rtt as? Number)?.toDouble()?.let { rttMs ->
                logRoundTripTime(connectionId, rttMs * 1000) // Convert to ms
            }
        }

        stats["jitter"]?.let { jitter ->
            (jitter as? Number)?.toDouble()?.let { jitterMs ->
                logJitter(connectionId, jitterMs * 1000) // Convert to ms
            }
        }

        stats["frameWidth"]?.let { width ->
            stats["frameHeight"]?.let { height ->
                val w = (width as? Number)?.toInt() ?: 0
                val h = (height as? Number)?.toInt() ?: 0
                if (w > 0 && h > 0) {
                    logVideoResolution(connectionId, w, h)
                }
            }
        }

        stats["framesPerSecond"]?.let { fps ->
            (fps as? Number)?.toDouble()?.let { fpsValue ->
                logFrameRate(connectionId, fpsValue)
            }
        }
    }
}
