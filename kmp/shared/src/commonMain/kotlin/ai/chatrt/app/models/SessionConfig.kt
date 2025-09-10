package ai.chatrt.app.models

import kotlinx.serialization.Serializable

/**
 * Configuration for ChatRT session
 */
@Serializable
data class SessionConfig(
    val type: String = "realtime",
    val model: String = "gpt-realtime",
    val instructions: String,
    val audio: AudioConfig,
)

/**
 * Audio configuration for the session
 */
@Serializable
data class AudioConfig(
    val input: AudioInputConfig,
    val output: AudioOutputConfig,
)

/**
 * Audio input configuration
 */
@Serializable
data class AudioInputConfig(
    val noiseReduction: NoiseReductionConfig,
)

/**
 * Noise reduction configuration
 */
@Serializable
data class NoiseReductionConfig(
    val type: String = "near_field",
)

/**
 * Audio output configuration
 */
@Serializable
data class AudioOutputConfig(
    val voice: String = "marin",
)
