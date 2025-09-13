package ai.chatrt.app.platform

import ai.chatrt.app.models.ConnectionState
import ai.chatrt.app.models.VideoMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for WebRTC connection lifecycle
 */
class WebRtcManagerTest {
    @Test
    fun testInitialConnectionState() =
        runTest {
            val webRtcManager = createMockWebRtcManager()

            val initialState = webRtcManager.observeConnectionState().first()
            assertEquals(ConnectionState.DISCONNECTED, initialState)
        }

    @Test
    fun testInitialIceConnectionState() =
        runTest {
            val webRtcManager = createMockWebRtcManager()

            val initialState = webRtcManager.observeIceConnectionState().first()
            assertEquals(IceConnectionState.NEW, initialState)
        }

    @Test
    fun testCreateOfferReturnsValidSdp() =
        runTest {
            val webRtcManager = createMockWebRtcManager()
            webRtcManager.initialize()

            val sdpOffer = webRtcManager.createOffer()

            assertNotNull(sdpOffer)
            assertTrue(sdpOffer.isNotEmpty())
            assertTrue(sdpOffer.startsWith("v=0"))
        }

    @Test
    fun testSetRemoteDescriptionSucceeds() =
        runTest {
            val webRtcManager = createMockWebRtcManager()
            webRtcManager.initialize()

            val mockSdp = "v=0\r\no=- 0 0 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\n"

            // Should not throw exception
            webRtcManager.setRemoteDescription(mockSdp)
        }

    @Test
    fun testAddLocalStreamAudioOnly() =
        runTest {
            val webRtcManager = createMockWebRtcManager()
            webRtcManager.initialize()

            // Should not throw exception
            webRtcManager.addLocalStream(VideoMode.AUDIO_ONLY)
        }

    @Test
    fun testAddLocalStreamWebcam() =
        runTest {
            val webRtcManager = createMockWebRtcManager()
            webRtcManager.initialize()

            // Should not throw exception
            webRtcManager.addLocalStream(VideoMode.WEBCAM)
        }

    @Test
    fun testAddLocalStreamScreenShare() =
        runTest {
            val webRtcManager = createMockWebRtcManager()
            webRtcManager.initialize()

            // Should not throw exception
            webRtcManager.addLocalStream(VideoMode.SCREEN_SHARE)
        }

    @Test
    fun testRemoveLocalStream() =
        runTest {
            val webRtcManager = createMockWebRtcManager()
            webRtcManager.initialize()

            webRtcManager.addLocalStream(VideoMode.WEBCAM)

            // Should not throw exception
            webRtcManager.removeLocalStream()
        }

    @Test
    fun testSwitchCamera() =
        runTest {
            val webRtcManager = createMockWebRtcManager()
            webRtcManager.initialize()

            webRtcManager.addLocalStream(VideoMode.WEBCAM)

            // Should not throw exception
            webRtcManager.switchCamera()
        }

    @Test
    fun testCloseConnection() =
        runTest {
            val webRtcManager = createMockWebRtcManager()
            webRtcManager.initialize()

            webRtcManager.close()

            val finalConnectionState = webRtcManager.observeConnectionState().first()
            val finalIceState = webRtcManager.observeIceConnectionState().first()

            assertEquals(ConnectionState.DISCONNECTED, finalConnectionState)
            assertEquals(IceConnectionState.CLOSED, finalIceState)
        }

    @Test
    fun testConnectionLifecycle() =
        runTest {
            val webRtcManager = createMockWebRtcManager()

            // Initialize
            webRtcManager.initialize()

            // Add local stream
            webRtcManager.addLocalStream(VideoMode.AUDIO_ONLY)

            // Create offer
            val offer = webRtcManager.createOffer()
            assertNotNull(offer)

            // Set remote description (simulating answer)
            val mockAnswer = "v=0\r\no=- 0 0 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\n"
            webRtcManager.setRemoteDescription(mockAnswer)

            // Remove stream
            webRtcManager.removeLocalStream()

            // Close connection
            webRtcManager.close()

            val finalState = webRtcManager.observeConnectionState().first()
            assertEquals(ConnectionState.DISCONNECTED, finalState)
        }

    private fun createMockWebRtcManager(): WebRtcManager = MockWebRtcManager()
}
