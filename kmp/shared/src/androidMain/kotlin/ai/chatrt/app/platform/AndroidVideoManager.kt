package ai.chatrt.app.platform

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simplified Android video manager stub for build stability.
 */
class AndroidVideoManager(
    private val context: Context,
) : VideoManager {
    private val _cameraState = MutableStateFlow(CameraState.IDLE)
    private var currentFacing = CameraFacing.FRONT
    private var currentResolution = Resolution(1280, 720)
    private var currentFrameRate = 30

    override suspend fun initialize() {
        _cameraState.value = CameraState.IDLE
    }

    override suspend fun createCameraStream(facing: CameraFacing): VideoStream? {
        currentFacing = facing
        _cameraState.value = CameraState.OPENED
        return object : VideoStream {
            override val id: String = "camera_${facing.name.lowercase()}"
            override val resolution: Resolution = currentResolution
            override val frameRate: Int = currentFrameRate

            override suspend fun start() {
                _cameraState.value = CameraState.CAPTURING
            }

            override suspend fun stop() {
                _cameraState.value = CameraState.CLOSED
            }

            override suspend fun pause() {
                _cameraState.value = CameraState.OPENED
            }

            override suspend fun resume() {
                _cameraState.value = CameraState.CAPTURING
            }
        }
    }

    override suspend fun stopCameraCapture() {
        _cameraState.value = CameraState.CLOSED
    }

    override suspend fun switchCamera(): CameraFacing? {
        currentFacing = if (currentFacing == CameraFacing.FRONT) CameraFacing.BACK else CameraFacing.FRONT
        return currentFacing
    }

    override suspend fun isFrontCameraAvailable(): Boolean = true

    override suspend fun isBackCameraAvailable(): Boolean = true

    override suspend fun getAvailableCameras(): List<CameraDevice> =
        listOf(
            CameraDevice("0", "Front Camera", CameraFacing.FRONT, emptyList(), emptyList()),
            CameraDevice("1", "Back Camera", CameraFacing.BACK, emptyList(), emptyList()),
        )

    override suspend fun setCameraResolution(resolution: Resolution) {
        currentResolution = resolution
    }

    override suspend fun setCameraFrameRate(frameRate: Int) {
        currentFrameRate = frameRate
    }

    override suspend fun setCameraFlash(enabled: Boolean) { /* no-op */ }

    override fun observeCameraState(): Flow<CameraState> = _cameraState.asStateFlow()

    override suspend fun getCameraCapabilities(): CameraCapabilities? = null

    override suspend fun cleanup() {
        _cameraState.value = CameraState.CLOSED
    }
}

actual fun createVideoManager(): VideoManager =
    throw IllegalStateException("Android VideoManager requires Context. Use AndroidVideoManager(context) directly.")
