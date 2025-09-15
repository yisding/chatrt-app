@file:Suppress("PropertyName")

package ai.chatrt.app.platform

import ai.chatrt.app.models.AudioMode
import ai.chatrt.app.models.ConnectionState
import ai.chatrt.app.models.VideoMode
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simplified Android WebRTC manager stub for build stability.
 * Provides the interface implementation without binding to the WebRTC SDK.
 */
class AndroidWebRtcManager(
    private val context: Context,
    private val audioManager: AndroidAudioManager,
    private val videoManager: AndroidVideoManager,
    private val screenCaptureManager: AndroidScreenCaptureManager,
) : WebRtcManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val _iceConnectionState = MutableStateFlow(IceConnectionState.NEW)

    private var currentVideoMode = VideoMode.AUDIO_ONLY
    private var isInitialized = false

    override suspend fun initialize() {
        // Initialize audio/video managers and configure audio routing
        audioManager.initialize()
        videoManager.initialize()
        audioManager.setupAudioRouting()
        audioManager.setAudioMode(AudioMode.COMMUNICATION)
        isInitialized = true
    }

    override suspend fun createOffer(): String {
        check(isInitialized) { "WebRTC manager not initialized" }
        _connectionState.value = ConnectionState.CONNECTING
        _iceConnectionState.value = IceConnectionState.CHECKING
        // Return a placeholder SDP for now
        return "v=0\no=- 0 0 IN IP4 127.0.0.1\ns=ChatRT\nt=0 0\nm=audio 9 RTP/AVP 0\na=rtpmap:0 PCMU/8000"
    }

    override suspend fun setRemoteDescription(sdp: String) {
        // No-op in stub
        _connectionState.value = ConnectionState.CONNECTED
        _iceConnectionState.value = IceConnectionState.CONNECTED
    }

    override suspend fun addLocalStream(videoMode: VideoMode) {
        currentVideoMode = videoMode
        // No-op in stub
    }

    override suspend fun removeLocalStream() {
        // No-op in stub
    }

    override suspend fun setRemoteAudioSink(audioSink: AudioSink) {
        // No-op in stub
    }

    override suspend fun close() {
        _connectionState.value = ConnectionState.DISCONNECTED
        _iceConnectionState.value = IceConnectionState.CLOSED
    }

    override fun observeConnectionState(): Flow<ConnectionState> = _connectionState.asStateFlow()

    override fun observeIceConnectionState(): Flow<IceConnectionState> = _iceConnectionState.asStateFlow()

    override suspend fun switchCamera() {
        // No-op in stub
    }

    override suspend fun getVideoStats(): VideoStats? = null

    override suspend fun getAudioStats(): AudioStats? = null

    // Convenience controls
    fun muteAudio(mute: Boolean) {
        Log.d("AndroidWebRtcManager", "muteAudio=$mute")
    }

    fun muteVideo(mute: Boolean) {
        Log.d("AndroidWebRtcManager", "muteVideo=$mute")
    }
}

actual fun createWebRtcManager(): WebRtcManager =
    throw IllegalStateException(
        "Android WebRtcManager requires Context and platform managers. Use AndroidWebRtcManager(context, audioManager, videoManager, screenCaptureManager) directly.",
    )
