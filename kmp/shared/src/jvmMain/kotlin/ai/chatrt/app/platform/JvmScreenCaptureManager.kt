package ai.chatrt.app.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit

/**
 * JVM/Desktop implementation of ScreenCaptureManager using Java AWT
 */
class JvmScreenCaptureManager : ScreenCaptureManager {
    
    private val _screenCaptureState = MutableStateFlow(ScreenCaptureState.IDLE)
    private var robot: Robot? = null
    
    override suspend fun initialize() {
        // Initialize desktop screen capture
        try {
            robot = Robot()
        } catch (e: Exception) {
            // Handle robot creation failure
        }
    }
    
    override suspend fun startScreenCapture(permissionData: Any?): VideoStream? {
        // Start screen capture using Java AWT Robot
        _screenCaptureState.value = ScreenCaptureState.STARTING
        
        return object : VideoStream {
            override val id: String = "desktop_screen_capture"
            override val resolution: Resolution = getScreenResolution()
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
        _screenCaptureState.value = ScreenCaptureState.IDLE
    }
    
    override suspend fun isScreenCaptureActive(): Boolean {
        return _screenCaptureState.value == ScreenCaptureState.ACTIVE
    }
    
    override suspend fun getAvailableScreens(): List<ScreenInfo> {
        // Get available screens/displays
        val screens = mutableListOf<ScreenInfo>()
        
        try {
            val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
            val devices = ge.screenDevices
            
            devices.forEachIndexed { index, device ->
                val bounds = device.defaultConfiguration.bounds
                screens.add(
                    ScreenInfo(
                        id = index.toString(),
                        name = "Display ${index + 1}",
                        resolution = Resolution(bounds.width, bounds.height),
                        isPrimary = index == 0
                    )
                )
            }
        } catch (e: Exception) {
            // Fallback to primary screen
            val screenSize = Toolkit.getDefaultToolkit().screenSize
            screens.add(
                ScreenInfo(
                    id = "0",
                    name = "Primary Display",
                    resolution = Resolution(screenSize.width, screenSize.height),
                    isPrimary = true
                )
            )
        }
        
        return screens
    }
    
    override suspend fun setScreenCaptureQuality(quality: ScreenCaptureQuality) {
        // Set screen capture quality
    }
    
    override suspend fun showScreenCaptureNotification() {
        // Desktop might show system tray notification
    }
    
    override suspend fun hideScreenCaptureNotification() {
        // Hide screen capture notification
    }
    
    override fun observeScreenCaptureState(): Flow<ScreenCaptureState> = _screenCaptureState.asStateFlow()
    
    override suspend fun getScreenCaptureCapabilities(): ScreenCaptureCapabilities? {
        val screens = getAvailableScreens()
        val maxResolution = screens.maxByOrNull { it.resolution.width * it.resolution.height }?.resolution
            ?: Resolution(1920, 1080)
        
        return ScreenCaptureCapabilities(
            supportedResolutions = listOf(
                maxResolution,
                Resolution(1920, 1080),
                Resolution(1280, 720),
                Resolution(854, 480)
            ),
            supportedFrameRates = listOf(15, 30, 60),
            supportsAudioCapture = false, // Java AWT Robot doesn't capture audio
            maxScreens = screens.size,
            requiresNotification = false
        )
    }
    
    override suspend fun cleanup() {
        // Cleanup screen capture resources
        _screenCaptureState.value = ScreenCaptureState.IDLE
        robot = null
    }
    
    private fun getScreenResolution(): Resolution {
        return try {
            val screenSize = Toolkit.getDefaultToolkit().screenSize
            Resolution(screenSize.width, screenSize.height)
        } catch (e: Exception) {
            Resolution(1920, 1080) // Default fallback
        }
    }
}

/**
 * Factory function for creating JVM screen capture manager
 */
actual fun createScreenCaptureManager(): ScreenCaptureManager = JvmScreenCaptureManager()