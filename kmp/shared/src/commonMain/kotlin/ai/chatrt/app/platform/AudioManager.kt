package ai.chatrt.app.platform

import ai.chatrt.app.models.AudioQuality
import kotlinx.coroutines.flow.Flow

/**
 * Audio manager interface for handling audio routing and device management
 */
interface AudioManager {
    /**
     * Initialize audio system
     */
    suspend fun initialize()

    /**
     * Setup audio routing for WebRTC calls
     */
    suspend fun setupAudioRouting()

    /**
     * Set audio mode (normal, call, etc.)
     */
    suspend fun setAudioMode(mode: AudioMode)

    /**
     * Handle audio focus changes
     */
    suspend fun requestAudioFocus(): Boolean

    /**
     * Release audio focus
     */
    suspend fun releaseAudioFocus()

    /**
     * Handle headset connection/disconnection
     */
    suspend fun handleHeadsetConnection(connected: Boolean)

    /**
     * Get available audio devices
     */
    suspend fun getAvailableAudioDevices(): List<AudioDevice>

    /**
     * Set active audio device
     */
    suspend fun setAudioDevice(device: AudioDevice)

    /**
     * Get current audio device
     */
    suspend fun getCurrentAudioDevice(): AudioDevice?

    /**
     * Observe audio device changes
     */
    fun observeAudioDeviceChanges(): Flow<AudioDevice>

    /**
     * Set audio quality settings
     */
    suspend fun setAudioQuality(quality: AudioQuality)

    /**
     * Enable/disable noise suppression
     */
    suspend fun setNoiseSuppression(enabled: Boolean)

    /**
     * Enable/disable echo cancellation
     */
    suspend fun setEchoCancellation(enabled: Boolean)

    /**
     * Cleanup audio resources
     */
    suspend fun cleanup()
}

/**
 * Audio modes for different scenarios
 */
enum class AudioMode {
    NORMAL,
    CALL,
    COMMUNICATION,
    RINGTONE,
}

/**
 * Audio device types
 */
data class AudioDevice(
    val id: String,
    val name: String,
    val type: AudioDeviceType,
    val isDefault: Boolean = false,
)

/**
 * Audio device types
 */
enum class AudioDeviceType {
    SPEAKER,
    EARPIECE,
    WIRED_HEADSET,
    BLUETOOTH_HEADSET,
    USB_HEADSET,
    UNKNOWN,
}

/**
 * Expected audio manager factory function
 */
expect fun createAudioManager(): AudioManager
