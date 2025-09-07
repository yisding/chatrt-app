package ai.chatrt.app.models

import kotlinx.serialization.Serializable

/**
 * Sealed class hierarchy for ChatRT error types
 */
sealed class ChatRtError : Exception() {
    
    /**
     * Network-related errors
     */
    object NetworkError : ChatRtError() {
        override val message: String = "Network connection error"
    }
    
    /**
     * Permission denied errors
     */
    object PermissionDenied : ChatRtError() {
        override val message: String = "Required permissions were denied"
    }
    
    /**
     * WebRTC connection errors
     */
    object WebRtcError : ChatRtError() {
        override val message: String = "WebRTC connection error"
    }
    
    /**
     * Audio device errors
     */
    object AudioDeviceError : ChatRtError() {
        override val message: String = "Audio device error"
    }
    
    /**
     * Camera-related errors
     */
    object CameraError : ChatRtError() {
        override val message: String = "Camera access error"
    }
    
    /**
     * Screen capture errors
     */
    object ScreenCaptureError : ChatRtError() {
        override val message: String = "Screen capture error"
    }
    
    /**
     * Service connection errors
     */
    object ServiceConnectionError : ChatRtError() {
        override val message: String = "Service connection error"
    }
    
    /**
     * Phone call interruption errors
     */
    object PhoneCallInterruptionError : ChatRtError() {
        override val message: String = "Phone call interruption error"
    }
    
    /**
     * Battery optimization errors
     */
    object BatteryOptimizationError : ChatRtError() {
        override val message: String = "Battery optimization error"
    }
    
    /**
     * Network quality errors
     */
    object NetworkQualityError : ChatRtError() {
        override val message: String = "Network quality error"
    }
    
    /**
     * API errors with specific code and message
     */
    @Serializable
    data class ApiError(
        val code: Int,
        override val message: String
    ) : ChatRtError()
}