package ai.chatrt.app.platform

import ai.chatrt.app.models.*

/**
 * JVM/Desktop implementation of PlatformManager
 */
class JvmPlatformManager : PlatformManager {
    
    private val permissionManager by lazy { JvmPermissionManager() }
    private val webRtcManager by lazy { JvmWebRtcManager() }
    private val audioManager by lazy { JvmAudioManager() }
    private val videoManager by lazy { JvmVideoManager() }
    private val screenCaptureManager by lazy { JvmScreenCaptureManager() }
    private val networkMonitor by lazy { JvmNetworkMonitor() }
    private val batteryMonitor by lazy { JvmBatteryMonitor() }
    private val lifecycleManager by lazy { JvmLifecycleManager() }
    
    override suspend fun requestPermissions(permissions: List<Permission>): PermissionResult {
        return permissionManager.requestPermissions(permissions)
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
        val networkState = networkMonitor.getCurrentNetworkState()
        
        return ResourceConstraints(
            availableMemory = runtime.freeMemory(),
            cpuUsage = getCpuUsage(),
            networkBandwidth = if (networkState.isConnected) 100000000L else 0L, // 100 Mbps default for desktop
            platformSpecific = mapOf(
                "javaVersion" to System.getProperty("java.version"),
                "osName" to System.getProperty("os.name"),
                "osArch" to System.getProperty("os.arch"),
                "availableProcessors" to Runtime.getRuntime().availableProcessors().toString()
            )
        )
    }
    
    override suspend fun createPlatformOptimization(): PlatformOptimization? {
        val networkQuality = networkMonitor.getCurrentNetworkQuality()
        val resourceConstraints = getResourceConstraints()
        
        return when {
            resourceConstraints.cpuUsage > 80.0f -> {
                PlatformOptimization(
                    recommendedVideoMode = VideoMode.AUDIO_ONLY,
                    recommendedAudioQuality = AudioQuality.MEDIUM,
                    disableVideoPreview = true,
                    reason = OptimizationReason.HIGH_CPU_USAGE
                )
            }
            networkQuality == NetworkQuality.POOR -> {
                PlatformOptimization(
                    recommendedVideoMode = VideoMode.AUDIO_ONLY,
                    recommendedAudioQuality = AudioQuality.LOW,
                    disableVideoPreview = false,
                    reason = OptimizationReason.POOR_NETWORK
                )
            }
            resourceConstraints.availableMemory < 200 * 1024 * 1024 -> { // Less than 200MB
                PlatformOptimization(
                    recommendedVideoMode = VideoMode.AUDIO_ONLY,
                    recommendedAudioQuality = AudioQuality.MEDIUM,
                    disableVideoPreview = true,
                    reason = OptimizationReason.LOW_MEMORY
                )
            }
            else -> null
        }
    }
    
    private fun getCpuUsage(): Float {
        // Simplified CPU usage calculation for JVM
        // In a real implementation, this would use JMX or other system monitoring
        return 0.0f
    }
}

/**
 * Factory function for creating JVM platform manager
 */
actual fun createPlatformManager(): PlatformManager = JvmPlatformManager()