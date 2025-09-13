package ai.chatrt.app.platform

import ai.chatrt.app.models.*
import kotlinx.coroutines.flow.Flow

/**
 * Audio manager interface for handling audio routing and device management
 * Requirements: 1.5, 2.3, 5.3
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
     * Requirement: 5.3 - Headphone connection/disconnection detection with audio routing
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
     * Observe audio device changes for real-time updates
     * Requirement: 5.3 - Device state changes with appropriate UI feedback
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
 * Expected audio manager factory function
 */
expect fun createAudioManager(): AudioManager
