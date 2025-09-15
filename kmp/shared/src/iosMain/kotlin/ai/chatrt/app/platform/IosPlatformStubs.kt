package ai.chatrt.app.platform

import ai.chatrt.app.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

private class IosAudioManager : AudioManager {
    private val deviceFlow = MutableSharedFlow<AudioDevice>(replay = 0)

    override suspend fun initialize() {}

    override suspend fun setupAudioRouting() {}

    override suspend fun setAudioMode(mode: AudioMode) {}

    override suspend fun requestAudioFocus(): Boolean = true

    override suspend fun releaseAudioFocus() {}

    override suspend fun handleHeadsetConnection(connected: Boolean) {}

    override suspend fun getAvailableAudioDevices(): List<AudioDevice> = emptyList()

    override suspend fun setAudioDevice(device: AudioDevice) {}

    override suspend fun getCurrentAudioDevice(): AudioDevice? = null

    override fun observeAudioDeviceChanges(): Flow<AudioDevice> = deviceFlow.asSharedFlow()

    override suspend fun setAudioQuality(quality: AudioQuality) {}

    override suspend fun setNoiseSuppression(enabled: Boolean) {}

    override suspend fun setEchoCancellation(enabled: Boolean) {}

    override suspend fun cleanup() {}
}

private class IosVideoManager : VideoManager {
    private val cameraStateFlow = MutableSharedFlow<CameraState>(replay = 0)

    override suspend fun initialize() {}

    override suspend fun createCameraStream(facing: CameraFacing): VideoStream? = null

    override suspend fun stopCameraCapture() {}

    override suspend fun switchCamera(): CameraFacing? = facingDefault

    private val facingDefault = CameraFacing.FRONT

    override suspend fun isFrontCameraAvailable(): Boolean = false

    override suspend fun isBackCameraAvailable(): Boolean = false

    override suspend fun getAvailableCameras(): List<CameraDevice> = emptyList()

    override suspend fun setCameraResolution(resolution: Resolution) {}

    override suspend fun setCameraFrameRate(frameRate: Int) {}

    override suspend fun setCameraFlash(enabled: Boolean) {}

    override fun observeCameraState(): Flow<CameraState> = cameraStateFlow.asSharedFlow()

    override suspend fun getCameraCapabilities(): CameraCapabilities? = null

    override suspend fun cleanup() {}
}

private class IosScreenCaptureManager : ScreenCaptureManager {
    private val stateFlow = MutableSharedFlow<ScreenCaptureState>(replay = 0)

    override suspend fun initialize() {}

    override suspend fun startScreenCapture(permissionData: Any?): VideoStream? = null

    override suspend fun stopScreenCapture() {}

    override suspend fun isScreenCaptureActive(): Boolean = false

    override suspend fun getAvailableScreens(): List<ScreenInfo> = emptyList()

    override suspend fun setScreenCaptureQuality(quality: ScreenCaptureQuality) {}

    override suspend fun showScreenCaptureNotification() {}

    override suspend fun hideScreenCaptureNotification() {}

    override fun observeScreenCaptureState(): Flow<ScreenCaptureState> = stateFlow.asSharedFlow()

    override suspend fun getScreenCaptureCapabilities(): ScreenCaptureCapabilities? = null

    override suspend fun cleanup() {}
}

private class IosPermissionManager : PermissionManager {
    private val changeFlow = MutableSharedFlow<PermissionChange>(replay = 0)

    override suspend fun checkPermission(permission: PermissionType): Boolean = false

    override suspend fun requestPermission(permission: PermissionType): Boolean = false

    override suspend fun requestMultiplePermissions(permissions: List<PermissionType>): Map<PermissionType, Boolean> =
        permissions.associateWith {
            false
        }

    override fun shouldShowRationale(permission: PermissionType): Boolean = false

    override fun openAppSettings() {}

    override fun observePermissionChanges(): Flow<PermissionChange> = changeFlow.asSharedFlow()
}

private class IosNetworkMonitor : NetworkMonitor {
    private val stateFlow = MutableSharedFlow<NetworkState>(replay = 0)
    private val qualityFlow = MutableSharedFlow<NetworkQuality>(replay = 0)

    override suspend fun initialize() {}

    override suspend fun startMonitoring() {}

    override suspend fun stopMonitoring() {}

    override suspend fun getCurrentNetworkState(): NetworkState = NetworkState(false, NetworkType.UNKNOWN)

    override suspend fun getCurrentNetworkQuality(): NetworkQuality = NetworkQuality.GOOD

    override fun observeNetworkState(): Flow<NetworkState> = stateFlow.asSharedFlow()

    override fun observeNetworkQuality(): Flow<NetworkQuality> = qualityFlow.asSharedFlow()

    override suspend fun getNetworkCapabilities(): NetworkCapabilities? = null

    override suspend fun testConnectivity(
        host: String,
        port: Int,
    ): Boolean = false

    override suspend fun measureBandwidth(): BandwidthInfo? = null

    override suspend fun cleanup() {}
}

private class IosLifecycleManager : LifecycleManager {
    private val lifecycleFlow = MutableSharedFlow<AppLifecycleState>(replay = 0)
    private val interruptionFlow = MutableSharedFlow<SystemInterruption>(replay = 0)

    override suspend fun initialize() {}

    override suspend fun startMonitoring() {}

    override suspend fun stopMonitoring() {}

    override suspend fun handleAppBackground() {}

    override suspend fun handleAppForeground() {}

    override suspend fun handleAppPause() {}

    override suspend fun handleAppResume() {}

    override suspend fun handleAppDestroy() {}

    override suspend fun handleOrientationChange(orientation: DeviceOrientation) {}

    override fun observeLifecycleState(): Flow<AppLifecycleState> = lifecycleFlow.asSharedFlow()

    override fun observeSystemInterruptions(): Flow<SystemInterruption> = interruptionFlow.asSharedFlow()

    override suspend fun getCurrentLifecycleState(): AppLifecycleState = AppLifecycleState.CREATED

    override suspend fun registerSystemInterruptionCallbacks() {}

    override suspend fun unregisterSystemInterruptionCallbacks() {}

    override suspend fun cleanup() {}
}

private class IosWebRtcManager : WebRtcManager {
    private val connStateFlow = MutableSharedFlow<ConnectionState>(replay = 0)
    private val iceStateFlow = MutableSharedFlow<IceConnectionState>(replay = 0)

    override suspend fun initialize() {}

    override suspend fun createOffer(): String = ""

    override suspend fun setRemoteDescription(sdp: String) {}

    override suspend fun addLocalStream(videoMode: VideoMode) {}

    override suspend fun removeLocalStream() {}

    override suspend fun setRemoteAudioSink(audioSink: AudioSink) {}

    override suspend fun close() {}

    override fun observeConnectionState(): Flow<ConnectionState> = connStateFlow.asSharedFlow()

    override fun observeIceConnectionState(): Flow<IceConnectionState> = iceStateFlow.asSharedFlow()

    override suspend fun switchCamera() {}

    override suspend fun getVideoStats(): VideoStats? = null

    override suspend fun getAudioStats(): AudioStats? = null
}

private class IosBatteryMonitor : BatteryMonitor {
    private val batteryFlow = MutableSharedFlow<BatteryState>(replay = 0)

    override suspend fun initialize() {}

    override suspend fun startMonitoring() {}

    override suspend fun stopMonitoring() {}

    override suspend fun getCurrentBatteryLevel(): Int = 100

    override suspend fun isBatteryLow(): Boolean = false

    override suspend fun isCharging(): Boolean = false

    override suspend fun getBatteryState(): BatteryState =
        BatteryState(
            level = 100,
            isCharging = false,
            chargingType = ChargingType.UNKNOWN,
            temperature = 0f,
            voltage = 0f,
            health = BatteryHealth.UNKNOWN,
            powerSavingMode = PowerSavingMode.NONE,
        )

    override fun observeBatteryState(): Flow<BatteryState> = batteryFlow.asSharedFlow()

    override suspend fun getPowerSavingRecommendation(): PowerSavingRecommendation? = null

    override suspend fun applyPowerSavingMode(mode: PowerSavingMode) {}

    override suspend fun isPowerSavingModeActive(): Boolean = false

    override suspend fun requestBatteryOptimizationExemption(): Boolean = false

    override suspend fun cleanup() {}
}

private class IosPlatformManager : PlatformManager {
    override suspend fun requestPermissions(permissions: List<Permission>): PermissionResult =
        PermissionResult(permissions.associateWith { false })

    override fun createWebRtcManager(): WebRtcManager = IosWebRtcManager()

    override fun createAudioManager(): AudioManager = IosAudioManager()

    override fun createVideoManager(): VideoManager = IosVideoManager()

    override fun createScreenCaptureManager(): ScreenCaptureManager = IosScreenCaptureManager()

    override fun createPermissionManager(): PermissionManager = IosPermissionManager()

    override fun createNetworkMonitor(): NetworkMonitor = IosNetworkMonitor()

    override fun createBatteryMonitor(): BatteryMonitor = IosBatteryMonitor()

    override fun createLifecycleManager(): LifecycleManager = IosLifecycleManager()

    override suspend fun handleSystemInterruption(): SystemInterruption? = null

    override suspend fun getResourceConstraints(): ResourceConstraints =
        ResourceConstraints(availableMemory = 0, cpuUsage = 0f, networkBandwidth = 0L, platformSpecific = emptyMap())

    override suspend fun createPlatformOptimization(): PlatformOptimization? = null
}

actual fun createAudioManager(): AudioManager = IosAudioManager()

actual fun createVideoManager(): VideoManager = IosVideoManager()

actual fun createScreenCaptureManager(): ScreenCaptureManager = IosScreenCaptureManager()

actual fun createPermissionManager(): PermissionManager = IosPermissionManager()

actual fun createNetworkMonitor(): NetworkMonitor = IosNetworkMonitor()

actual fun createLifecycleManager(): LifecycleManager = IosLifecycleManager()

actual fun createWebRtcManager(): WebRtcManager = IosWebRtcManager()

actual fun createBatteryMonitor(): BatteryMonitor = IosBatteryMonitor()

actual fun createPlatformManager(): PlatformManager = IosPlatformManager()
