package ai.chatrt.app.platform

import ai.chatrt.app.models.*

/**
 * Main platform manager interface that provides access to all platform-specific functionality
 */
interface PlatformManager {
    /**
     * Request permissions for the specified list of permissions
     */
    suspend fun requestPermissions(permissions: List<Permission>): PermissionResult

    /**
     * Create a WebRTC manager instance
     */
    fun createWebRtcManager(): WebRtcManager

    /**
     * Create an audio manager instance
     */
    fun createAudioManager(): AudioManager

    /**
     * Create a video manager instance
     */
    fun createVideoManager(): VideoManager

    /**
     * Create a screen capture manager instance
     */
    fun createScreenCaptureManager(): ScreenCaptureManager

    /**
     * Create a permission manager instance
     */
    fun createPermissionManager(): PermissionManager

    /**
     * Create a network monitor instance
     */
    fun createNetworkMonitor(): NetworkMonitor

    /**
     * Create a battery monitor instance
     */
    fun createBatteryMonitor(): BatteryMonitor

    /**
     * Create a lifecycle manager instance
     */
    fun createLifecycleManager(): LifecycleManager

    /**
     * Handle system interruptions
     */
    suspend fun handleSystemInterruption(): SystemInterruption?

    /**
     * Get current resource constraints
     */
    suspend fun getResourceConstraints(): ResourceConstraints

    /**
     * Create platform optimization recommendations
     */
    suspend fun createPlatformOptimization(): PlatformOptimization?
}

/**
 * Permission types that can be requested
 */
enum class Permission {
    MICROPHONE,
    CAMERA,
    SCREEN_CAPTURE,
    NOTIFICATIONS,
}

/**
 * Result of permission request
 */
data class PermissionResult(
    val granted: Map<Permission, Boolean>,
    val shouldShowRationale: Map<Permission, Boolean> = emptyMap(),
)

/**
 * Expected platform manager factory function
 */
expect fun createPlatformManager(): PlatformManager
