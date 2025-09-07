package ai.chatrt.app.platform

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android implementation of ScreenCaptureManager using MediaProjection API
 */
class AndroidScreenCaptureManager(
    private val context: Context
) : ScreenCaptureManager {
    
    private val _screenCaptureState = MutableStateFlow(ScreenCaptureState.IDLE)
    
    override suspend fun initialize() {
        // Initialize MediaProjection API
    }
    
    override suspend fun startScreenCapture(permissionData: Any?): VideoStream? {
        // Start screen capture using MediaProjection
        _screenCaptureState.value = ScreenCaptureState.STARTING
        
        return object : VideoStream {
            override val id: String = "screen_capture"
            override val resolution: Resolution = Resolution(1920, 1080)
            override val frameRate: Int = 30
            
            override suspend fun start() {
                _screenCaptureState.value = ScreenCaptureState.ACTIVE
            }
            
            override suspend fun stop() {
                _screenCaptureState.value = ScreenCaptureState.IDLE
            }
            
            override suspend fun pause() {
                _screenCaptureState.value = ScreenCaptureState.PAUSED
            }
            
            override suspend fun resume() {
                _screenCaptureState.value = ScreenCaptureState.ACTIVE
            }
        }
    }
    
    override suspend fun stopScreenCapture() {
        _screenCaptureState.value = ScreenCaptureState.STOPPING
        // Stop MediaProjection
        _screenCaptureState.value = ScreenCaptureState.IDLE
    }
    
    override suspend fun isScreenCaptureActive(): Boolean {
        return _screenCaptureState.value == ScreenCaptureState.ACTIVE
    }
    
    override suspend fun getAvailableScreens(): List<ScreenInfo> {
        // Android typically has one main screen
        return listOf(
            ScreenInfo(
                id = "main",
                name = "Main Display",
                resolution = Resolution(1920, 1080),
                isPrimary = true
            )
        )
    }
    
    override suspend fun setScreenCaptureQuality(quality: ScreenCaptureQuality) {
        // Set screen capture quality
    }
    
    override suspend fun showScreenCaptureNotification() {
        // Show persistent notification for screen recording
    }
    
    override suspend fun hideScreenCaptureNotification() {
        // Hide screen recording notification
    }
    
    override fun observeScreenCaptureState(): Flow<ScreenCaptureState> = _screenCaptureState.asStateFlow()
    
    override suspend fun getScreenCaptureCapabilities(): ScreenCaptureCapabilities? {
        return ScreenCaptureCapabilities(
            supportedResolutions = listOf(
                Resolution(1920, 1080),
                Resolution(1280, 720),
                Resolution(854, 480)
            ),
            supportedFrameRates = listOf(15, 30, 60),
            supportsAudioCapture = true,
            maxScreens = 1,
            requiresNotification = true
        )
    }
    
    override suspend fun cleanup() {
        // Cleanup screen capture resources
        _screenCaptureState.value = ScreenCaptureState.IDLE
    }
}

/**
 * Factory function for creating Android screen capture manager
 */
actual fun createScreenCaptureManager(): ScreenCaptureManager {
    throw IllegalStateException("Android ScreenCaptureManager requires Context. Use AndroidScreenCaptureManager(context) directly.")
}