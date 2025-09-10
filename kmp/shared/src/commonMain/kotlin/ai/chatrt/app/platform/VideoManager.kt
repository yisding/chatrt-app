package ai.chatrt.app.platform

import ai.chatrt.app.models.VideoMode
import kotlinx.coroutines.flow.Flow

/**
 * Video manager interface for handling camera and video capture
 */
interface VideoManager {
    /**
     * Initialize video system
     */
    suspend fun initialize()

    /**
     * Create camera stream for video calls
     */
    suspend fun createCameraStream(facing: CameraFacing = CameraFacing.FRONT): VideoStream?

    /**
     * Stop camera capture
     */
    suspend fun stopCameraCapture()

    /**
     * Switch between front and back cameras
     */
    suspend fun switchCamera(): CameraFacing?

    /**
     * Check if front camera is available
     */
    suspend fun isFrontCameraAvailable(): Boolean

    /**
     * Check if back camera is available
     */
    suspend fun isBackCameraAvailable(): Boolean

    /**
     * Get available camera devices
     */
    suspend fun getAvailableCameras(): List<CameraDevice>

    /**
     * Set camera resolution
     */
    suspend fun setCameraResolution(resolution: Resolution)

    /**
     * Set camera frame rate
     */
    suspend fun setCameraFrameRate(frameRate: Int)

    /**
     * Enable/disable camera flash
     */
    suspend fun setCameraFlash(enabled: Boolean)

    /**
     * Observe camera state changes
     */
    fun observeCameraState(): Flow<CameraState>

    /**
     * Get current camera capabilities
     */
    suspend fun getCameraCapabilities(): CameraCapabilities?

    /**
     * Cleanup video resources
     */
    suspend fun cleanup()
}

/**
 * Camera facing direction
 */
enum class CameraFacing {
    FRONT,
    BACK,
    EXTERNAL,
}

/**
 * Video stream representation
 */
interface VideoStream {
    val id: String
    val resolution: Resolution
    val frameRate: Int

    suspend fun start()

    suspend fun stop()

    suspend fun pause()

    suspend fun resume()
}

/**
 * Camera device information
 */
data class CameraDevice(
    val id: String,
    val name: String,
    val facing: CameraFacing,
    val supportedResolutions: List<Resolution>,
    val supportedFrameRates: List<Int>,
)

/**
 * Camera state
 */
enum class CameraState {
    IDLE,
    OPENING,
    OPENED,
    CAPTURING,
    CLOSED,
    ERROR,
}

/**
 * Camera capabilities
 */
data class CameraCapabilities(
    val supportedResolutions: List<Resolution>,
    val supportedFrameRates: List<Int>,
    val hasFlash: Boolean,
    val hasAutoFocus: Boolean,
    val maxZoom: Float,
    val supportedFocusModes: List<FocusMode>,
)

/**
 * Camera focus modes
 */
enum class FocusMode {
    AUTO,
    CONTINUOUS_VIDEO,
    CONTINUOUS_PICTURE,
    MANUAL,
    FIXED,
}

/**
 * Expected video manager factory function
 */
expect fun createVideoManager(): VideoManager
