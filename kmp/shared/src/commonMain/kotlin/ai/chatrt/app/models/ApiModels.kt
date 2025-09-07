package ai.chatrt.app.models

import kotlinx.serialization.Serializable

/**
 * Request model for creating a call
 */
@Serializable
data class CallRequest(
    val sdp: String,
    val session: SessionConfig
)

/**
 * Response model for call creation
 */
@Serializable
data class CallResponse(
    val callId: String,
    val sdpAnswer: String,
    val status: String
)

/**
 * Connection parameters for establishing a call
 */
@Serializable
data class ConnectionParams(
    val videoMode: VideoMode,
    val audioQuality: AudioQuality,
    val cameraFacing: CameraFacing = CameraFacing.FRONT
)