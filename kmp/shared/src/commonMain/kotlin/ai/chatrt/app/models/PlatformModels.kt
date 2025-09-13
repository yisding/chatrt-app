package ai.chatrt.app.models

import kotlinx.serialization.Serializable

/**
 * Platform-agnostic optimization recommendations
 */
@Serializable
data class PlatformOptimization(
    val recommendedVideoMode: VideoMode,
    val recommendedAudioQuality: AudioQuality,
    val disableVideoPreview: Boolean = false,
    val reason: OptimizationReason,
)

/**
 * Reasons for platform optimization
 */
@Serializable
enum class OptimizationReason {
    LOW_BATTERY, // Android-specific
    HIGH_CPU_USAGE, // Desktop-specific
    LOW_MEMORY, // Both platforms
    POOR_NETWORK, // Both platforms
}

/**
 * System interruption information
 */
@Serializable
data class SystemInterruption(
    val type: InterruptionType,
    val shouldPause: Boolean,
    val canResume: Boolean = true,
)

/**
 * Types of system interruptions
 */
@Serializable
enum class InterruptionType {
    PHONE_CALL, // Android-specific
    SYSTEM_CALL, // Desktop-specific (e.g., Skype, Teams)
    LOW_POWER_MODE, // Both platforms
    NETWORK_LOSS, // Both platforms
}

/**
 * Resource constraints information
 */
@Serializable
data class ResourceConstraints(
    val availableMemory: Long,
    val cpuUsage: Float,
    val networkBandwidth: Long,
    val platformSpecific: Map<String, String> = emptyMap(),
)

/**
 * Network quality levels
 */
@Serializable
enum class NetworkQuality {
    POOR,
    FAIR,
    GOOD,
    EXCELLENT,
}

/**
 * Power saving modes
 */
@Serializable
enum class PowerSavingMode {
    NONE,
    MODERATE,
    AGGRESSIVE,
}

/**
 * Power saving recommendations
 */
@Serializable
data class PowerSavingRecommendation(
    val mode: PowerSavingMode,
    val reason: String,
    val estimatedBatteryGain: String,
)

/**
 * Audio device information
 */
@Serializable
data class AudioDevice(
    val id: String,
    val name: String,
    val type: AudioDeviceType,
    val isDefault: Boolean = false,
)

/**
 * Types of audio devices
 */
@Serializable
enum class AudioDeviceType {
    SPEAKER,
    EARPIECE,
    WIRED_HEADSET,
    WIRED_HEADPHONES,
    BLUETOOTH_HEADSET,
    USB_HEADSET,
    UNKNOWN,
}

/**
 * Audio device information for diagnostics
 */
@Serializable
data class AudioDeviceInfo(
    val currentDevice: AudioDevice?,
    val availableDevices: List<AudioDevice>,
    val isBluetoothScoOn: Boolean = false,
    val isSpeakerphoneOn: Boolean = false,
    val isWiredHeadsetOn: Boolean = false,
    val audioMode: Int = 0,
)

/**
 * Audio modes for different scenarios
 */
@Serializable
enum class AudioMode {
    NORMAL,
    CALL,
    COMMUNICATION,
    RINGTONE,
}
