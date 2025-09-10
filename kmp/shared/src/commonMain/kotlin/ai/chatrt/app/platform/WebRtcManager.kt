package ai.chatrt.app.platform

import ai.chatrt.app.models.ConnectionState
import ai.chatrt.app.models.VideoMode
import kotlinx.coroutines.flow.Flow

/**
 * WebRTC manager interface for handling peer-to-peer connections
 */
interface WebRtcManager {
    /**
     * Initialize the WebRTC manager
     */
    suspend fun initialize()

    /**
     * Create an SDP offer for establishing connection
     */
    suspend fun createOffer(): String

    /**
     * Set the remote SDP description received from the server
     */
    suspend fun setRemoteDescription(sdp: String)

    /**
     * Add local media stream (audio/video/screen)
     */
    suspend fun addLocalStream(videoMode: VideoMode)

    /**
     * Remove local media stream
     */
    suspend fun removeLocalStream()

    /**
     * Set up remote audio sink for receiving audio
     */
    suspend fun setRemoteAudioSink(audioSink: AudioSink)

    /**
     * Close the WebRTC connection and cleanup resources
     */
    suspend fun close()

    /**
     * Observe connection state changes
     */
    fun observeConnectionState(): Flow<ConnectionState>

    /**
     * Observe ICE connection state changes
     */
    fun observeIceConnectionState(): Flow<IceConnectionState>

    /**
     * Switch camera (front/back) during video call
     */
    suspend fun switchCamera()

    /**
     * Get current video stream statistics
     */
    suspend fun getVideoStats(): VideoStats?

    /**
     * Get current audio stream statistics
     */
    suspend fun getAudioStats(): AudioStats?
}

/**
 * Audio sink interface for receiving remote audio
 */
interface AudioSink {
    fun onAudioFrame(
        audioData: ByteArray,
        sampleRate: Int,
        channels: Int,
    )
}

/**
 * ICE connection states
 */
enum class IceConnectionState {
    NEW,
    CHECKING,
    CONNECTED,
    COMPLETED,
    FAILED,
    DISCONNECTED,
    CLOSED,
}

/**
 * Video stream statistics
 */
data class VideoStats(
    val bytesReceived: Long,
    val bytesSent: Long,
    val framesReceived: Int,
    val framesSent: Int,
    val resolution: Resolution,
    val frameRate: Int,
)

/**
 * Audio stream statistics
 */
data class AudioStats(
    val bytesReceived: Long,
    val bytesSent: Long,
    val packetsReceived: Int,
    val packetsSent: Int,
    val audioLevel: Float,
)

/**
 * Video resolution
 */
data class Resolution(
    val width: Int,
    val height: Int,
)

/**
 * Expected WebRTC manager factory function
 */
expect fun createWebRtcManager(): WebRtcManager
