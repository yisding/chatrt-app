@file:Suppress("PropertyName")

package ai.chatrt.app.platform

import ai.chatrt.app.models.ConnectionState
import ai.chatrt.app.models.VideoMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mock implementation of WebRtcManager for testing
 */
class MockWebRtcManager : WebRtcManager {
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val _iceConnectionState = MutableStateFlow(IceConnectionState.NEW)

    private var isInitialized = false
    private var hasLocalStream = false
    private var currentVideoMode: VideoMode? = null

    override suspend fun initialize() {
        isInitialized = true
    }

    override suspend fun createOffer(): String {
        if (!isInitialized) {
            throw IllegalStateException("WebRTC manager not initialized")
        }

        _connectionState.value = ConnectionState.CONNECTING
        _iceConnectionState.value = IceConnectionState.CHECKING

        return "v=0\r\n" +
            "o=- 4611731400430051336 2 IN IP4 127.0.0.1\r\n" +
            "s=-\r\n" +
            "t=0 0\r\n" +
            "a=group:BUNDLE 0 1\r\n" +
            "a=extmap-allow-mixed\r\n" +
            "a=msid-semantic: WMS\r\n" +
            "m=audio 9 UDP/TLS/RTP/SAVPF 111 63 103 104 9 0 8 106 105 13 110 112 113 126\r\n" +
            "c=IN IP4 0.0.0.0\r\n" +
            "a=rtcp:9 IN IP4 0.0.0.0\r\n" +
            "a=ice-ufrag:mock\r\n" +
            "a=ice-pwd:mockpassword\r\n" +
            "a=ice-options:trickle\r\n" +
            "a=fingerprint:sha-256 00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00\r\n" +
            "a=setup:actpass\r\n" +
            "a=mid:0\r\n" +
            "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
            "a=sendrecv\r\n" +
            "a=msid:- mock-audio-track\r\n" +
            "a=rtcp-mux\r\n" +
            "a=rtpmap:111 opus/48000/2\r\n" +
            if (currentVideoMode != VideoMode.AUDIO_ONLY) {
                "m=video 9 UDP/TLS/RTP/SAVPF 96 97 98 99 100 101 102 121 127 120 125 107 108 109 124 119 123 118 114 115 116\r\n" +
                    "c=IN IP4 0.0.0.0\r\n" +
                    "a=rtcp:9 IN IP4 0.0.0.0\r\n" +
                    "a=ice-ufrag:mock\r\n" +
                    "a=ice-pwd:mockpassword\r\n" +
                    "a=ice-options:trickle\r\n" +
                    "a=fingerprint:sha-256 00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00\r\n" +
                    "a=setup:actpass\r\n" +
                    "a=mid:1\r\n" +
                    "a=extmap:14 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                    "a=sendrecv\r\n" +
                    "a=msid:- mock-video-track\r\n" +
                    "a=rtcp-mux\r\n" +
                    "a=rtcp-rsize\r\n" +
                    "a=rtpmap:96 VP8/90000\r\n"
            } else {
                ""
            }
    }

    override suspend fun setRemoteDescription(sdp: String) {
        if (!isInitialized) {
            throw IllegalStateException("WebRTC manager not initialized")
        }

        if (sdp.isEmpty()) {
            throw IllegalArgumentException("SDP cannot be empty")
        }

        // Simulate successful connection
        _connectionState.value = ConnectionState.CONNECTED
        _iceConnectionState.value = IceConnectionState.CONNECTED
    }

    override suspend fun addLocalStream(videoMode: VideoMode) {
        if (!isInitialized) {
            throw IllegalStateException("WebRTC manager not initialized")
        }

        hasLocalStream = true
        currentVideoMode = videoMode
    }

    override suspend fun removeLocalStream() {
        hasLocalStream = false
        currentVideoMode = null
    }

    override suspend fun setRemoteAudioSink(audioSink: AudioSink) {
        if (!isInitialized) {
            throw IllegalStateException("WebRTC manager not initialized")
        }
        // Mock implementation - no actual audio processing
    }

    override suspend fun close() {
        isInitialized = false
        hasLocalStream = false
        currentVideoMode = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _iceConnectionState.value = IceConnectionState.CLOSED
    }

    override fun observeConnectionState(): Flow<ConnectionState> = _connectionState.asStateFlow()

    override fun observeIceConnectionState(): Flow<IceConnectionState> = _iceConnectionState.asStateFlow()

    override suspend fun switchCamera() {
        if (currentVideoMode != VideoMode.WEBCAM) {
            throw IllegalStateException("Camera switching only available in webcam mode")
        }
        // Mock implementation - no actual camera switching
    }

    override suspend fun getVideoStats(): VideoStats? =
        if (hasLocalStream && currentVideoMode != VideoMode.AUDIO_ONLY) {
            VideoStats(
                bytesReceived = 1024L,
                bytesSent = 2048L,
                framesReceived = 30,
                framesSent = 30,
                resolution = Resolution(1280, 720),
                frameRate = 30,
            )
        } else {
            null
        }

    override suspend fun getAudioStats(): AudioStats? =
        if (hasLocalStream) {
            AudioStats(
                bytesReceived = 512L,
                bytesSent = 1024L,
                packetsReceived = 100,
                packetsSent = 100,
                audioLevel = 0.5f,
            )
        } else {
            null
        }
}
