package ai.chatrt.app.platform

import ai.chatrt.app.models.*
import android.content.Context

/**
 * Android implementation of PlatformManager
 */
class AndroidPlatformManager(
    private val context: Context,
) : PlatformManager {
    private val permissionManager by lazy { AndroidPermissionManager(context) }
    private val webRtcManager by lazy { AndroidWebRtcManager(context) }
    private val audioManager by lazy { AndroidAudioManager(context) }
    private val videoManager by lazy { AndroidVideoManager(context) }
    private val screenCaptureManager by lazy { AndroidScreenCaptureManager(context) }
    private val networkMonitor by lazy { AndroidNetworkMonitor(context) }
    private val batteryMonitor by lazy { AndroidBatteryMonitor(context) }
    private val lifecycleManager by lazy { AndroidLifecycleManager(context) }

    override suspend fun requestPermissions(permissions: List<Permission>): PermissionResult {
        val results = permissionManager.requestPermissions(permissions)
        return PermissionResult(
            granted = results.mapValues { it.value == PermissionStatus.GRANTED },
        )
    }

    override fun createWebRtcManager(): WebRtcManager = webRtcManager

    override fun createAudioManager(): AudioManager = audioManager

    override fun createVideoManager(): VideoManager = videoManager

    override fun createScreenCaptureManager(): ScreenCaptureManager = screenCaptureManager

    override fun createPermissionManager(): PermissionManager = permissionManager

    override fun createNetworkMonitor(): NetworkMonitor = networkMonitor

    override fun createBatteryMonitor(): BatteryMonitor = batteryMonitor

    override fun createLifecycleManager(): LifecycleManager = lifecycleManager

    override suspend fun handleSystemInterruption(): SystemInterruption? {
        // Get the most recent system interruption if any
        // In a real implementation, this would check current system state
        return null
    }

    override suspend fun getResourceConstraints(): ResourceConstraints {
        val runtime = Runtime.getRuntime()
        val batteryState = batteryMonitor.getBatteryState()
        val networkState = networkMonitor.getCurrentNetworkState()

        return ResourceConstraints(
            availableMemory = runtime.freeMemory(),
            cpuUsage = 0.0f, // Would need more complex implementation
            networkBandwidth = if (networkState.isConnected) 1000000L else 0L,
            platformSpecific =
                mapOf(
                    "batteryLevel" to batteryState.level.toString(),
                    "isCharging" to batteryState.isCharging.toString(),
                    "networkType" to networkState.networkType.name,
                ),
        )
    }

    override suspend fun createPlatformOptimization(): PlatformOptimization? {
        val batteryState = batteryMonitor.getBatteryState()
        val networkQuality = networkMonitor.getCurrentNetworkQuality()
        val resourceConstraints = getResourceConstraints()

        return when {
            batteryState.level < 20 && !batteryState.isCharging -> {
                PlatformOptimization(
                    recommendedVideoMode = VideoMode.AUDIO_ONLY,
                    recommendedAudioQuality = AudioQuality.LOW,
                    disableVideoPreview = true,
                    reason = OptimizationReason.LOW_BATTERY,
                )
            }
            networkQuality == NetworkQuality.POOR -> {
                PlatformOptimization(
                    recommendedVideoMode = VideoMode.AUDIO_ONLY,
                    recommendedAudioQuality = AudioQuality.LOW,
                    disableVideoPreview = false,
                    reason = OptimizationReason.POOR_NETWORK,
                )
            }
            resourceConstraints.availableMemory < 100 * 1024 * 1024 -> { // Less than 100MB
                PlatformOptimization(
                    recommendedVideoMode = VideoMode.AUDIO_ONLY,
                    recommendedAudioQuality = AudioQuality.MEDIUM,
                    disableVideoPreview = true,
                    reason = OptimizationReason.LOW_MEMORY,
                )
            }
            else -> null
        }
    }
}

/**
 * Factory function for creating Android platform manager
 */
actual fun createPlatformManager(): PlatformManager {
    // This would typically get the context from a dependency injection framework
    // For now, we'll throw an error indicating proper initialization is needed
    throw IllegalStateException("Android PlatformManager requires Context. Use AndroidPlatformManager(context) directly.")
}
