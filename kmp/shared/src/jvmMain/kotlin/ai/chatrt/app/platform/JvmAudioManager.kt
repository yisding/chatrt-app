package ai.chatrt.app.platform

import ai.chatrt.app.models.AudioQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.sound.sampled.AudioSystem

/** JVM/Desktop implementation of AudioManager using Java Sound API */
class JvmAudioManager : AudioManager {
    private val _audioDeviceChanges =
        MutableStateFlow(AudioDevice("default", "Default", AudioDeviceType.SPEAKER))

    override suspend fun initialize() {
        // Initialize Java Sound API
    }

    override suspend fun setupAudioRouting() {
        // Setup audio routing for desktop WebRTC
    }

    override suspend fun setAudioMode(mode: AudioMode) {
        // Desktop doesn't have the same audio mode concept as mobile
        // This would configure audio settings appropriately
    }

    override suspend fun requestAudioFocus(): Boolean {
        // Desktop doesn't have audio focus concept like Android
        return true
    }

    override suspend fun releaseAudioFocus() {
        // No-op on desktop
    }

    override suspend fun handleHeadsetConnection(connected: Boolean) {
        // Handle headset/headphone connection on desktop
    }

    override suspend fun getAvailableAudioDevices(): List<AudioDevice> {
        // Get available audio devices using Java Sound API
        val devices = mutableListOf<AudioDevice>()

        try {
            val mixerInfos = AudioSystem.getMixerInfo()
            mixerInfos.forEachIndexed { index, mixerInfo ->
                val mixer = AudioSystem.getMixer(mixerInfo)
                val deviceType =
                    when {
                        mixerInfo.name.contains("speaker", ignoreCase = true) ->
                            AudioDeviceType.SPEAKER
                        mixerInfo.name.contains("headphone", ignoreCase = true) ->
                            AudioDeviceType.WIRED_HEADSET
                        mixerInfo.name.contains("bluetooth", ignoreCase = true) ->
                            AudioDeviceType.BLUETOOTH_HEADSET
                        mixerInfo.name.contains("usb", ignoreCase = true) ->
                            AudioDeviceType.USB_HEADSET
                        else -> AudioDeviceType.UNKNOWN
                    }

                devices.add(
                    AudioDevice(
                        id = index.toString(),
                        name = mixerInfo.name,
                        type = deviceType,
                        isDefault = index == 0,
                    ),
                )
            }
        } catch (e: Exception) {
            // Fallback to default device
            devices.add(
                AudioDevice("default", "Default Audio Device", AudioDeviceType.SPEAKER, true),
            )
        }

        return devices
    }

    override suspend fun setAudioDevice(device: AudioDevice) {
        // Set active audio device
        _audioDeviceChanges.value = device
    }

    override suspend fun getCurrentAudioDevice(): AudioDevice? = _audioDeviceChanges.value

    override fun observeAudioDeviceChanges(): Flow<AudioDevice> = _audioDeviceChanges.asStateFlow()

    override suspend fun setAudioQuality(quality: AudioQuality) {
        // Set audio quality parameters for desktop
    }

    override suspend fun setNoiseSuppression(enabled: Boolean) {
        // Enable/disable noise suppression on desktop
    }

    override suspend fun setEchoCancellation(enabled: Boolean) {
        // Enable/disable echo cancellation on desktop
    }

    override suspend fun cleanup() {
        // Cleanup audio resources
    }
}

/** Factory function for creating JVM audio manager */
actual fun createAudioManager(): AudioManager = JvmAudioManager()
