package ai.chatrt.app.platform

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android implementation of VideoManager using Camera2 API
 */
class AndroidVideoManager(
    private val context: Context,
) : VideoManager {
    private val _cameraState = MutableStateFlow(CameraState.IDLE)
    private var currentCameraFacing = CameraFacing.FRONT

    override suspend fun initialize() {
        // Initialize Camera2 API
    }

    override suspend fun createCameraStream(facing: CameraFacing): VideoStream? {
        // Create camera stream using Camera2 API
        currentCameraFacing = facing
        _cameraState.value = CameraState.OPENING

        return object : VideoStream {
            override val id: String = "camera_${facing.name.lowercase()}"
            override val resolution: Resolution = Resolution(1280, 720)
            override val frameRate: Int = 30

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
        currentCameraFacing =
            when (currentCameraFacing) {
                CameraFacing.FRONT -> CameraFacing.BACK
                CameraFacing.BACK -> CameraFacing.FRONT
                CameraFacing.EXTERNAL -> CameraFacing.FRONT
            }
        return currentCameraFacing
    }

    override suspend fun isFrontCameraAvailable(): Boolean {
        // Check if front camera is available
        return true
    }

    override suspend fun isBackCameraAvailable(): Boolean {
        // Check if back camera is available
        return true
    }

    override suspend fun getAvailableCameras(): List<CameraDevice> {
        // Get available camera devices
        return listOf(
            CameraDevice(
                id = "0",
                name = "Back Camera",
                facing = CameraFacing.BACK,
                supportedResolutions =
                    listOf(
                        Resolution(1920, 1080),
                        Resolution(1280, 720),
                        Resolution(640, 480),
                    ),
                supportedFrameRates = listOf(15, 30, 60),
            ),
            CameraDevice(
                id = "1",
                name = "Front Camera",
                facing = CameraFacing.FRONT,
                supportedResolutions =
                    listOf(
                        Resolution(1280, 720),
                        Resolution(640, 480),
                    ),
                supportedFrameRates = listOf(15, 30),
            ),
        )
    }

    override suspend fun setCameraResolution(resolution: Resolution) {
        // Set camera resolution
    }

    override suspend fun setCameraFrameRate(frameRate: Int) {
        // Set camera frame rate
    }

    override suspend fun setCameraFlash(enabled: Boolean) {
        // Enable/disable camera flash
    }

    override fun observeCameraState(): Flow<CameraState> = _cameraState.asStateFlow()

    override suspend fun getCameraCapabilities(): CameraCapabilities? {
        // Get camera capabilities
        return CameraCapabilities(
            supportedResolutions =
                listOf(
                    Resolution(1920, 1080),
                    Resolution(1280, 720),
                    Resolution(640, 480),
                ),
            supportedFrameRates = listOf(15, 30, 60),
            hasFlash = true,
            hasAutoFocus = true,
            maxZoom = 10.0f,
            supportedFocusModes = listOf(FocusMode.AUTO, FocusMode.CONTINUOUS_VIDEO),
        )
    }

    override suspend fun cleanup() {
        // Cleanup camera resources
        _cameraState.value = CameraState.CLOSED
    }
}

/**
 * Factory function for creating Android video manager
 */
actual fun createVideoManager(): VideoManager =
    throw IllegalStateException("Android VideoManager requires Context. Use AndroidVideoManager(context) directly.")
