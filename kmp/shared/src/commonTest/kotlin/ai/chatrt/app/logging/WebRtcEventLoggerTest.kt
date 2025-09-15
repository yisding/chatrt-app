package ai.chatrt.app.logging

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebRtcEventLoggerTest {
    private fun createMockLogger(): Logger = ChatRtLogger()

    @Test
    fun `should log connection lifecycle events`() =
        runTest {
            val logger = createMockLogger()
            val webRtcLogger = WebRtcEventLogger(logger)

            webRtcLogger.logConnectionCreated("conn-123", mapOf("iceServers" to 2))
            webRtcLogger.logConnectionConnecting("conn-123")
            webRtcLogger.logConnectionConnected("conn-123", mapOf("bandwidth" to "1000kbps"))

            val events = webRtcLogger.eventsFlow.first()
            assertEquals(3, events.size)

            assertEquals(WebRtcEventType.CONNECTION_CREATED, events[0].type)
            assertEquals(WebRtcEventType.CONNECTION_CONNECTING, events[1].type)
            assertEquals(WebRtcEventType.CONNECTION_CONNECTED, events[2].type)

            assertTrue(events.all { it.connectionId == "conn-123" })
        }

    @Test
    fun `should log ICE candidate events`() =
        runTest {
            val logger = createMockLogger()
            val webRtcLogger = WebRtcEventLogger(logger)

            webRtcLogger.logIceCandidateAdded("conn-123", "candidate:1234567890", "audio")
            webRtcLogger.logIceGatheringComplete("conn-123", 5)

            val events = webRtcLogger.eventsFlow.first()
            assertEquals(2, events.size)

            assertEquals(WebRtcEventType.ICE_CANDIDATE_ADDED, events[0].type)
            assertEquals(WebRtcEventType.ICE_GATHERING_COMPLETE, events[1].type)

            assertEquals("candidate:1234567890", events[0].details["candidate"])
            assertEquals("audio", events[0].details["sdpMid"])
            assertEquals(5, events[1].details["candidateCount"])
        }

    @Test
    fun `should log SDP events`() =
        runTest {
            val logger = createMockLogger()
            val webRtcLogger = WebRtcEventLogger(logger)

            val sdpOffer = "v=0\r\no=- 123456789 2 IN IP4 127.0.0.1\r\n..."
            val sdpAnswer = "v=0\r\no=- 987654321 2 IN IP4 192.168.1.1\r\n..."

            webRtcLogger.logSdpOfferCreated("conn-123", sdpOffer)
            webRtcLogger.logSdpAnswerReceived("conn-123", sdpAnswer)

            val events = webRtcLogger.eventsFlow.first()
            assertEquals(2, events.size)

            assertEquals(WebRtcEventType.SDP_OFFER_CREATED, events[0].type)
            assertEquals(WebRtcEventType.SDP_ANSWER_RECEIVED, events[1].type)

            assertEquals(sdpOffer.length, events[0].details["sdpLength"])
            assertEquals(sdpAnswer.length, events[1].details["sdpLength"])
        }

    @Test
    fun `should log media stream events`() =
        runTest {
            val logger = createMockLogger()
            val webRtcLogger = WebRtcEventLogger(logger)

            webRtcLogger.logMediaStreamAdded("conn-123", "stream-456", 2)
            webRtcLogger.logMediaStreamRemoved("conn-123", "stream-456")

            val events = webRtcLogger.eventsFlow.first()
            assertEquals(2, events.size)

            assertEquals(WebRtcEventType.MEDIA_STREAM_ADDED, events[0].type)
            assertEquals(WebRtcEventType.MEDIA_STREAM_REMOVED, events[1].type)

            assertEquals("stream-456", events[0].details["streamId"])
            assertEquals(2, events[0].details["trackCount"])
            assertEquals("stream-456", events[1].details["streamId"])
        }

    @Test
    fun `should log connection diagnostics`() =
        runTest {
            val logger = createMockLogger()
            val webRtcLogger = WebRtcEventLogger(logger)

            webRtcLogger.logBandwidth("conn-123", 1000.0, 500.0)
            webRtcLogger.logPacketLoss("conn-123", 2.5)
            webRtcLogger.logRoundTripTime("conn-123", 50.0)

            val diagnostics = webRtcLogger.diagnosticsFlow.first()
            assertEquals(3, diagnostics.size)

            assertEquals(DiagnosticType.BANDWIDTH_UP, diagnostics[0].type)
            assertEquals("1000.0", diagnostics[0].value)
            assertEquals("kbps", diagnostics[0].unit)

            assertEquals(DiagnosticType.PACKET_LOSS, diagnostics[1].type)
            assertEquals("2.5", diagnostics[1].value)
            assertEquals("%", diagnostics[1].unit)

            assertEquals(DiagnosticType.ROUND_TRIP_TIME, diagnostics[2].type)
            assertEquals("50.0", diagnostics[2].value)
            assertEquals("ms", diagnostics[2].unit)
        }

    @Test
    fun `should log audio and video diagnostics`() =
        runTest {
            val logger = createMockLogger()
            val webRtcLogger = WebRtcEventLogger(logger)

            webRtcLogger.logAudioLevel("conn-123", -20.5)
            webRtcLogger.logVideoResolution("conn-123", 1920, 1080)
            webRtcLogger.logFrameRate("conn-123", 30.0)

            val diagnostics = webRtcLogger.diagnosticsFlow.first()
            assertEquals(3, diagnostics.size)

            assertEquals(DiagnosticType.AUDIO_LEVEL, diagnostics[0].type)
            assertEquals("-20.5", diagnostics[0].value)
            assertEquals("dB", diagnostics[0].unit)

            assertEquals(DiagnosticType.VIDEO_RESOLUTION, diagnostics[1].type)
            assertEquals("1920x1080", diagnostics[1].value)

            assertEquals(DiagnosticType.FRAME_RATE, diagnostics[2].type)
            assertEquals("30.0", diagnostics[2].value)
            assertEquals("fps", diagnostics[2].unit)
        }

    @Test
    fun `should extract diagnostics from WebRTC stats`() =
        runTest {
            val logger = createMockLogger()
            val webRtcLogger = WebRtcEventLogger(logger)

            val stats =
                mapOf(
                    "packetsLost" to 10,
                    "packetsReceived" to 1000,
                    "roundTripTime" to 0.05, // 50ms in seconds
                    "jitter" to 0.002, // 2ms in seconds
                    "frameWidth" to 1280,
                    "frameHeight" to 720,
                    "framesPerSecond" to 25.0,
                )

            webRtcLogger.logStatsUpdate("conn-123", stats)

            val events = webRtcLogger.eventsFlow.first()
            assertEquals(1, events.size)
            assertEquals(WebRtcEventType.STATS_UPDATED, events[0].type)

            // Check that diagnostics were extracted
            val diagnostics = webRtcLogger.diagnosticsFlow.first()
            assertTrue(diagnostics.isNotEmpty())

            // Should have packet loss diagnostic
            val packetLossDiagnostic = diagnostics.find { it.type == DiagnosticType.PACKET_LOSS }
            assertEquals("0.99", packetLossDiagnostic?.value) // (10 / 1010) * 100 ≈ 0.99%

            // Should have RTT diagnostic
            val rttDiagnostic = diagnostics.find { it.type == DiagnosticType.ROUND_TRIP_TIME }
            assertEquals("50.0", rttDiagnostic?.value) // 0.05 * 1000 = 50ms

            // Should have video resolution diagnostic
            val resolutionDiagnostic = diagnostics.find { it.type == DiagnosticType.VIDEO_RESOLUTION }
            assertEquals("1280x720", resolutionDiagnostic?.value)
        }

    @Test
    fun `should handle connection failures`() =
        runTest {
            val logger = createMockLogger()
            val webRtcLogger = WebRtcEventLogger(logger)

            webRtcLogger.logConnectionFailed("conn-123", "ICE connection failed")
            webRtcLogger.logConnectionClosed("conn-123")

            val events = webRtcLogger.eventsFlow.first()
            assertEquals(2, events.size)

            assertEquals(WebRtcEventType.CONNECTION_FAILED, events[0].type)
            assertEquals("ICE connection failed", events[0].details["error"])

            assertEquals(WebRtcEventType.CONNECTION_CLOSED, events[1].type)
        }

    @Test
    fun `should log data channel events`() =
        runTest {
            val logger = createMockLogger()
            val webRtcLogger = WebRtcEventLogger(logger)

            webRtcLogger.logDataChannelOpened("conn-123", "chat")
            webRtcLogger.logDataChannelClosed("conn-123", "chat")

            val events = webRtcLogger.eventsFlow.first()
            assertEquals(2, events.size)

            assertEquals(WebRtcEventType.DATA_CHANNEL_OPENED, events[0].type)
            assertEquals("chat", events[0].details["channelLabel"])

            assertEquals(WebRtcEventType.DATA_CHANNEL_CLOSED, events[1].type)
            assertEquals("chat", events[1].details["channelLabel"])
        }
}
