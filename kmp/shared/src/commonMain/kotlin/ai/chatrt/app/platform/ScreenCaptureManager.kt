package ai.chatrt.app.platform

import kotlinx.coroutines.flow.Flow

/**
 * Screen capture manager interface for handling screen recording and sharing
 */
interface ScreenCaptureManager {
    /**
     * Initialize screen capture system
     */
    suspend fun initialize()

    /**
     * Start screen capture with the given permission data
     */
    suspend fun startScreenCapture(permissionData: Any? = null): VideoStream?

    /**
     * Stop screen capture
     */
    suspend fun stopScreenCapture()

    /**
     * Check if screen capture is currently active
     */
    suspend fun isScreenCaptureActive(): Boolean

    /**
     * Get available screens/displays for capture
     */
    suspend fun getAvailableScreens(): List<ScreenInfo>

    /**
     * Set screen capture quality
     */
    suspend fun setScreenCaptureQuality(quality: ScreenCaptureQuality)

    /**
     * Show screen capture notification (for platforms that require it)
     */
    suspend fun showScreenCaptureNotification()

    /**
     * Hide screen capture notification
     */
    suspend fun hideScreenCaptureNotification()

    /**
     * Observe screen capture state changes
     */
    fun observeScreenCaptureState(): Flow<ScreenCaptureState>

    /**
     * Get screen capture capabilities
     */
    suspend fun getScreenCaptureCapabilities(): ScreenCaptureCapabilities?

    /**
     * Cleanup screen capture resources
     */
    suspend fun cleanup()
}

/**
 * Screen information
 */
data class ScreenInfo(
    val id: String,
    val name: String,
    val resolution: Resolution,
    val isPrimary: Boolean = false,
)

/**
 * Screen capture quality settings
 */
enum class ScreenCaptureQuality {
    LOW, // 720p, lower frame rate
    MEDIUM, // 1080p, standard frame rate
    HIGH, // 1440p or higher, high frame rate
    AUTO, // Adaptive based on network/performance
}

/**
 * Screen capture state
 */
enum class ScreenCaptureState {
    IDLE,
    STARTING,
    ACTIVE,
    PAUSED,
    STOPPING,
    ERROR,
}

/**
 * Screen capture capabilities
 */
data class ScreenCaptureCapabilities(
    val supportedResolutions: List<Resolution>,
    val supportedFrameRates: List<Int>,
    val supportsAudioCapture: Boolean,
    val maxScreens: Int,
    val requiresNotification: Boolean,
)

/**
 * Expected screen capture manager factory function
 */
expect fun createScreenCaptureManager(): ScreenCaptureManager
