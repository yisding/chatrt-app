package ai.chatrt.app.platform

import ai.chatrt.app.models.ConnectionState
import ai.chatrt.app.models.VideoMode
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Android-specific unit tests for AndroidWebRtcManager
 */
@RunWith(AndroidJUnit4::class)
class AndroidWebRtcManagerTest {
    private lateinit var context: Context
    private lateinit var audioManager: AndroidAudioManager
    private lateinit var videoManager: AndroidVideoManager
    private lateinit var screenCaptureManager: AndroidScreenCaptureManager
    private lateinit var webRtcManager: AndroidWebRtcManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        audioManager = AndroidAudioManager(context)
        videoManager = AndroidVideoManager(context)
        screenCaptureManager = AndroidScreenCaptureManager(context)
        webRtcManager = AndroidWebRtcManager(context, audioManager, videoManager, screenCaptureManager)
    }

    @Test
    fun testInitialization() =
        runTest {
            // Should not throw exception
            webRtcManager.initialize()
        }

    @Test
    fun testInitialConnectionState() =
        runTest {
            val initialState = webRtcManager.observeConnectionState().first()
            assertEquals(ConnectionState.DISCONNECTED, initialState)
        }

    @Test
    fun testInitialIceConnectionState() =
        runTest {
            val initialState = webRtcManager.observeIceConnectionState().first()
            assertEquals(IceConnectionState.NEW, initialState)
        }

    @Test
    fun testCreateOfferAfterInitialization() =
        runTest {
            webRtcManager.initialize()

            val sdpOffer = webRtcManager.createOffer()

            assertNotNull(sdpOffer)
            assertTrue(sdpOffer.isNotEmpty())
            assertTrue(sdpOffer.startsWith("v=0"))
            assertTrue(sdpOffer.contains("m=audio"))
        }

    @Test
    fun testCreateOfferWithVideoMode() =
        runTest {
            webRtcManager.initialize()
            webRtcManager.addLocalStream(VideoMode.WEBCAM)

            val sdpOffer = webRtcManager.createOffer()

            assertNotNull(sdpOffer)
            assertTrue(sdpOffer.contains("m=video"))
        }

    @Test
    fun testAddAudioOnlyStream() =
        runTest {
            webRtcManager.initialize()

            // Should not throw exception
            webRtcManager.addLocalStream(VideoMode.AUDIO_ONLY)
        }

    @Test
    fun testAddWebcamStream() =
        runTest {
            webRtcManager.initialize()

            // Should not throw exception
            webRtcManager.addLocalStream(VideoMode.WEBCAM)
        }

    @Test
    fun testRemoveLocalStream() =
        runTest {
            webRtcManager.initialize()
            webRtcManager.addLocalStream(VideoMode.WEBCAM)

            // Should not throw exception
            webRtcManager.removeLocalStream()
        }

    @Test
    fun testSwitchCameraInWebcamMode() =
        runTest {
            webRtcManager.initialize()
            webRtcManager.addLocalStream(VideoMode.WEBCAM)

            // Should not throw exception
            webRtcManager.switchCamera()
        }

    @Test
    fun testCloseConnection() =
        runTest {
            webRtcManager.initialize()
            webRtcManager.addLocalStream(VideoMode.AUDIO_ONLY)

            webRtcManager.close()

            val finalConnectionState = webRtcManager.observeConnectionState().first()
            val finalIceState = webRtcManager.observeIceConnectionState().first()

            assertEquals(ConnectionState.DISCONNECTED, finalConnectionState)
            assertEquals(IceConnectionState.CLOSED, finalIceState)
        }

    @Test
    fun testFullConnectionLifecycle() =
        runTest {
            // Initialize
            webRtcManager.initialize()

            // Add local stream
            webRtcManager.addLocalStream(VideoMode.AUDIO_ONLY)

            // Create offer
            val offer = webRtcManager.createOffer()
            assertNotNull(offer)
            assertTrue(offer.isNotEmpty())

            // Simulate setting remote description (answer)
            val mockAnswer = createMockSdpAnswer()
            webRtcManager.setRemoteDescription(mockAnswer)

            // Remove stream
            webRtcManager.removeLocalStream()

            // Close connection
            webRtcManager.close()

            val finalState = webRtcManager.observeConnectionState().first()
            assertEquals(ConnectionState.DISCONNECTED, finalState)
        }

    @Test
    fun testVideoModeSwitch() =
        runTest {
            webRtcManager.initialize()

            // Start with audio only
            webRtcManager.addLocalStream(VideoMode.AUDIO_ONLY)

            // Switch to webcam
            webRtcManager.addLocalStream(VideoMode.WEBCAM)

            // Switch back to audio only
            webRtcManager.addLocalStream(VideoMode.AUDIO_ONLY)

            // Clean up
            webRtcManager.close()
        }

    private fun createMockSdpAnswer(): String =
        "v=0\r\n" +
            "o=- 4611731400430051336 2 IN IP4 127.0.0.1\r\n" +
            "s=-\r\n" +
            "t=0 0\r\n" +
            "a=group:BUNDLE 0\r\n" +
            "a=extmap-allow-mixed\r\n" +
            "a=msid-semantic: WMS\r\n" +
            "m=audio 9 UDP/TLS/RTP/SAVPF 111\r\n" +
            "c=IN IP4 0.0.0.0\r\n" +
            "a=rtcp:9 IN IP4 0.0.0.0\r\n" +
            "a=ice-ufrag:test\r\n" +
            "a=ice-pwd:testpassword\r\n" +
            "a=ice-options:trickle\r\n" +
            "a=fingerprint:sha-256 00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00\r\n" +
            "a=setup:active\r\n" +
            "a=mid:0\r\n" +
            "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
            "a=recvonly\r\n" +
            "a=rtcp-mux\r\n" +
            "a=rtpmap:111 opus/48000/2\r\n"
}
