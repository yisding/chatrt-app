@file:Suppress("PropertyName")

package ai.chatrt.app.platform

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Simplified Android screen capture manager stub for build stability. */
class AndroidScreenCaptureManager(
    private val context: Context,
) : ScreenCaptureManager {
    private val _state = MutableStateFlow(ScreenCaptureState.IDLE)
    private var currentResolution = Resolution(1280, 720)
    private var currentFrameRate = 30

    override suspend fun initialize() {
        // no-op
    }

    override suspend fun startScreenCapture(permissionData: Any?): VideoStream? {
        _state.value = ScreenCaptureState.ACTIVE
        return object : VideoStream {
            override val id: String = "screen_capture"
            override val resolution: Resolution = currentResolution
            override val frameRate: Int = currentFrameRate

            override suspend fun start() {
                _state.value = ScreenCaptureState.ACTIVE
            }

            override suspend fun stop() {
                _state.value = ScreenCaptureState.IDLE
            }

            override suspend fun pause() {
                _state.value = ScreenCaptureState.PAUSED
            }

            override suspend fun resume() {
                _state.value = ScreenCaptureState.ACTIVE
            }
        }
    }

    override suspend fun stopScreenCapture() {
        _state.value = ScreenCaptureState.IDLE
    }

    override suspend fun isScreenCaptureActive(): Boolean = _state.value == ScreenCaptureState.ACTIVE

    override suspend fun getAvailableScreens(): List<ScreenInfo> = listOf(ScreenInfo("main", "Main Display", currentResolution, true))

    override suspend fun setScreenCaptureQuality(quality: ScreenCaptureQuality) {
        // no-op
    }

    override suspend fun showScreenCaptureNotification() {
        // no-op
    }

    override suspend fun hideScreenCaptureNotification() {
        // no-op
    }

    override fun observeScreenCaptureState(): Flow<ScreenCaptureState> = _state.asStateFlow()

    override suspend fun getScreenCaptureCapabilities(): ScreenCaptureCapabilities? = null

    override suspend fun cleanup() {
        _state.value = ScreenCaptureState.IDLE
    }
}

actual fun createScreenCaptureManager(): ScreenCaptureManager =
    throw IllegalStateException(
        "Android ScreenCaptureManager requires Context. Use AndroidScreenCaptureManager(context) directly.",
    )
