package ai.chatrt.app.models

import kotlinx.serialization.Serializable

/**
 * Application settings and preferences
 */
@Serializable
data class AppSettings(
    val defaultVideoMode: VideoMode = VideoMode.AUDIO_ONLY,
    val audioQuality: AudioQuality = AudioQuality.MEDIUM,
    val debugLogging: Boolean = false,
    val serverUrl: String = "",
    val defaultCamera: CameraFacing = CameraFacing.FRONT,
)

/**
 * Audio quality settings
 */
@Serializable
enum class AudioQuality {
    LOW,
    MEDIUM,
    HIGH,
}

/**
 * Camera facing direction
 */
@Serializable
enum class CameraFacing {
    FRONT,
    BACK,
}
