package ai.chatrt.app.logging

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChatRtLoggerTest {
    @Test
    fun `logger should emit logs to flow`() =
        runTest {
            val logger = ChatRtLogger()

            logger.info("TestTag", "Test message")

            val logs = logger.getLogs().first()
            assertEquals(1, logs.size)
            assertEquals("Test message", logs.first().message)
            assertEquals("TestTag", logs.first().tag)
            assertEquals(LogLevel.INFO, logs.first().level)
        }

    @Test
    fun `logger should filter logs by level`() =
        runTest {
            val logger = ChatRtLogger()

            logger.debug("Debug", "Debug message")
            logger.info("Info", "Info message")
            logger.warning("Warning", "Warning message")
            logger.error("Error", "Error message")

            val infoAndAbove = logger.getLogsByLevel(LogLevel.INFO).first()
            assertEquals(3, infoAndAbove.size) // INFO, WARNING, ERROR
            assertTrue(infoAndAbove.all { it.level.priority >= LogLevel.INFO.priority })
        }

    @Test
    fun `logger should filter logs by category`() =
        runTest {
            val logger = ChatRtLogger()

            // Create logs with different categories by using logWebRtcEvent
            val webRtcEvent =
                WebRtcEvent(
                    type = WebRtcEventType.CONNECTION_CREATED,
                    connectionId = "test-connection",
                )
            logger.logWebRtcEvent(webRtcEvent)
            logger.info("General", "General message")

            val webRtcLogs = logger.getLogsByCategory(LogCategory.WEBRTC).first()
            assertEquals(1, webRtcLogs.size)
            assertEquals(LogCategory.WEBRTC, webRtcLogs.first().category)
        }

    @Test
    fun `logger should handle WebRTC events`() =
        runTest {
            val logger = ChatRtLogger()

            val event =
                WebRtcEvent(
                    type = WebRtcEventType.CONNECTION_CONNECTED,
                    connectionId = "test-connection-123",
                    details = mapOf("bandwidth" to "1000kbps"),
                )

            logger.logWebRtcEvent(event)

            val logs = logger.getLogs().first()
            assertEquals(1, logs.size)

            val log = logs.first()
            assertEquals(LogCategory.WEBRTC, log.category)
            assertTrue(log.message.contains("CONNECTION_CONNECTED"))
            assertEquals("test-connection-123", log.metadata["connectionId"])
        }

    @Test
    fun `logger should handle connection diagnostics`() =
        runTest {
            val logger = ChatRtLogger()

            val diagnostic =
                ConnectionDiagnostic(
                    connectionId = "test-connection",
                    type = DiagnosticType.BANDWIDTH_UP,
                    value = "500",
                    unit = "kbps",
                )

            logger.logConnectionDiagnostic(diagnostic)

            val logs = logger.getLogs().first()
            assertEquals(1, logs.size)

            val log = logs.first()
            assertEquals(LogCategory.NETWORK, log.category)
            assertTrue(log.message.contains("BANDWIDTH_UP"))
            assertTrue(log.message.contains("500 kbps"))
        }

    @Test
    fun `logger should export logs as JSON`() =
        runTest {
            val logger = ChatRtLogger()

            logger.info("Test", "Test message 1")
            logger.error("Test", "Test message 2", RuntimeException("Test error"))

            val exportedLogs = logger.exportLogs()

            assertNotNull(exportedLogs)
            assertTrue(exportedLogs.contains("Test message 1"))
            assertTrue(exportedLogs.contains("Test message 2"))
            assertTrue(exportedLogs.contains("Test error"))
            assertTrue(exportedLogs.contains("exportTimestamp"))
            assertTrue(exportedLogs.contains("totalEntries"))
        }

    @Test
    fun `logger should clear logs`() =
        runTest {
            val logger = ChatRtLogger()

            logger.info("Test", "Test message")
            assertEquals(1, logger.getLogs().first().size)

            logger.clearLogs()
            assertEquals(0, logger.getLogs().first().size)
        }

    @Test
    fun `logger should rotate logs when threshold is reached`() =
        runTest {
            val logger = ChatRtLogger(maxLogEntries = 10, rotationThreshold = 8)

            // Add logs beyond rotation threshold
            repeat(12) { i ->
                logger.info("Test", "Message $i")
            }

            // Give some time for rotation to complete
            kotlinx.coroutines.delay(100)

            val logs = logger.getLogs().first()
            assertTrue(logs.size <= 10) // Should not exceed max entries

            // Should contain a rotation log entry
            assertTrue(logs.any { it.tag == "LogRotation" })
        }

    @Test
    fun `logger should handle throwables`() =
        runTest {
            val logger = ChatRtLogger()
            val testException = RuntimeException("Test exception message")

            logger.error("ErrorTag", "Error occurred", testException)

            val logs = logger.getLogs().first()
            assertEquals(1, logs.size)

            val log = logs.first()
            assertEquals(LogLevel.ERROR, log.level)
            assertEquals("Error occurred", log.message)
            assertNotNull(log.throwable)
            assertEquals("Test exception message", log.throwable?.message)
        }

    @Test
    fun `logger should handle metadata`() =
        runTest {
            val logger = ChatRtLogger()

            val event =
                WebRtcEvent(
                    type = WebRtcEventType.SDP_OFFER_CREATED,
                    connectionId = "conn-123",
                    details =
                        mapOf(
                            "sdpLength" to 1024,
                            "codec" to "opus",
                        ),
                )

            logger.logWebRtcEvent(event)

            val logs = logger.getLogs().first()
            val log = logs.first()

            assertEquals("conn-123", log.metadata["connectionId"])
            assertEquals("SDP_OFFER_CREATED", log.metadata["eventType"])
            assertNotNull(log.metadata["details"])
        }
}
