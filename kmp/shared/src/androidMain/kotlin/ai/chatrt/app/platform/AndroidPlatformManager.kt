package ai.chatrt.app.platform

import ai.chatrt.app.models.*
import android.content.Context

/**
 * Android implementation of PlatformManager
 */
class AndroidPlatformManager(
    private val context: Context,
) : PlatformManager {
    private val audioManager by lazy { AndroidAudioManager(context) }
    private val videoManager by lazy { AndroidVideoManager(context) }
    private val screenCaptureManager by lazy { AndroidScreenCaptureManager(context) }
    private val webRtcManager by lazy { AndroidWebRtcManager(context, audioManager, videoManager, screenCaptureManager) }
    private val permissionManager by lazy { createPermissionManager() }
    private val networkMonitor by lazy { AndroidNetworkMonitor(context) }
    private val batteryMonitor by lazy { AndroidBatteryMonitor(context) }
    private val lifecycleManager by lazy { AndroidLifecycleManager(context) }

    override suspend fun requestPermissions(permissions: List<Permission>): PermissionResult {
        // Minimal stub: report all permissions as granted=false by default
        val granted = permissions.associateWith { false }
        return PermissionResult(granted = granted, shouldShowRationale = permissions.associateWith { false })
    }

    override fun createWebRtcManager(): WebRtcManager = webRtcManager

    override fun createAudioManager(): AudioManager = audioManager

    override fun createVideoManager(): VideoManager = videoManager

    override fun createScreenCaptureManager(): ScreenCaptureManager = screenCaptureManager

    override fun createPermissionManager(): PermissionManager = permissionManager

    override fun createNetworkMonitor(): NetworkMonitor = networkMonitor

    override fun createBatteryMonitor(): BatteryMonitor = batteryMonitor

    override fun createLifecycleManager(): LifecycleManager = lifecycleManager

    override suspend fun handleSystemInterruption(): SystemInterruption? = null

    override suspend fun getResourceConstraints(): ResourceConstraints {
        val runtime = Runtime.getRuntime()
        val net = networkMonitor.getCurrentNetworkState()
        return ResourceConstraints(
            availableMemory = runtime.freeMemory(),
            cpuUsage = 0f,
            networkBandwidth = if (net.isConnected) 1_000_000L else 0L,
            platformSpecific = emptyMap(),
        )
    }

    override suspend fun createPlatformOptimization(): PlatformOptimization? = null
}

/**
 * Factory function for creating Android platform manager
 */
actual fun createPlatformManager(): PlatformManager {
    // This would typically get the context from a dependency injection framework
    // For now, we'll throw an error indicating proper initialization is needed
    throw IllegalStateException("Android PlatformManager requires Context. Use AndroidPlatformManager(context) directly.")
}
