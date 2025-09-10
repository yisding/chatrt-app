package ai.chatrt.app.repository

import ai.chatrt.app.models.ConnectionState
import ai.chatrt.app.models.VideoMode
import ai.chatrt.app.platform.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mock implementation of WebRtcManager for testing
 */
class MockWebRtcManager : WebRtcManager {
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val _iceConnectionState = MutableStateFlow(IceConnectionState.NEW)

    var initializeCalled = false
    var createOfferCalled = false
    var setRemoteDescriptionCalled = false
    var addLocalStreamCalled = false
    var removeLocalStreamCalled = false
    var closeCalled = false
    var switchCameraCalled = false

    var mockSdpOffer = "v=0\r\no=- 0 0 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\n"
    var shouldThrowOnInitialize = false
    var shouldThrowOnCreateOffer = false
    var shouldThrowOnSetRemoteDescription = false

    override suspend fun initialize() {
        initializeCalled = true
        if (shouldThrowOnInitialize) {
            throw RuntimeException("Mock initialize error")
        }
    }

    override suspend fun createOffer(): String {
        if (shouldThrowOnCreateOffer) {
            throw RuntimeException("Mock create offer error")
        }
        createOfferCalled = true
        return mockSdpOffer
    }

    override suspend fun setRemoteDescription(sdp: String) {
        if (shouldThrowOnSetRemoteDescription) {
            throw RuntimeException("Mock set remote description error")
        }
        setRemoteDescriptionCalled = true
    }

    override suspend fun addLocalStream(videoMode: VideoMode) {
        addLocalStreamCalled = true
    }

    override suspend fun removeLocalStream() {
        removeLocalStreamCalled = true
    }

    override suspend fun setRemoteAudioSink(audioSink: AudioSink) {
        // Mock implementation
    }

    override suspend fun close() {
        closeCalled = true
        _connectionState.value = ConnectionState.DISCONNECTED
        _iceConnectionState.value = IceConnectionState.CLOSED
    }

    override fun observeConnectionState(): Flow<ConnectionState> = _connectionState.asStateFlow()

    override fun observeIceConnectionState(): Flow<IceConnectionState> = _iceConnectionState.asStateFlow()

    override suspend fun switchCamera() {
        switchCameraCalled = true
    }

    override suspend fun getVideoStats(): VideoStats? =
        VideoStats(
            bytesReceived = 1000,
            bytesSent = 1000,
            framesReceived = 30,
            framesSent = 30,
            resolution = Resolution(1280, 720),
            frameRate = 30,
        )

    override suspend fun getAudioStats(): AudioStats? =
        AudioStats(
            bytesReceived = 500,
            bytesSent = 500,
            packetsReceived = 100,
            packetsSent = 100,
            audioLevel = 0.5f,
        )

    // Test helper methods
    fun simulateConnectionStateChange(state: ConnectionState) {
        _connectionState.value = state
    }

    fun simulateIceConnectionStateChange(state: IceConnectionState) {
        _iceConnectionState.value = state
    }

    fun reset() {
        initializeCalled = false
        createOfferCalled = false
        setRemoteDescriptionCalled = false
        addLocalStreamCalled = false
        removeLocalStreamCalled = false
        closeCalled = false
        switchCameraCalled = false
        shouldThrowOnInitialize = false
        shouldThrowOnCreateOffer = false
        shouldThrowOnSetRemoteDescription = false
        _connectionState.value = ConnectionState.DISCONNECTED
        _iceConnectionState.value = IceConnectionState.NEW
    }
}
