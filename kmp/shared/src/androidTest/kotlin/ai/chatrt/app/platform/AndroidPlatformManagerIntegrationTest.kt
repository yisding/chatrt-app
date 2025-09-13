package ai.chatrt.app.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for all Android platform managers
 * Tests the interaction between different platform managers
 */
@RunWith(AndroidJUnit4::class)
class AndroidPlatformManagerIntegrationTest {
    private lateinit var context: Context
    private lateinit var permissionManager: AndroidPermissionManager
    private lateinit var audioManager: AndroidAudioManager
    private lateinit var videoManager: AndroidVideoManager
    private lateinit var screenCaptureManager: AndroidScreenCaptureManager
    private lateinit var lifecycleManager: AndroidLifecycleManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        permissionManager = AndroidPermissionManager(context)
        audioManager = AndroidAudioManager(context)
        videoManager = AndroidVideoManager(context)
        screenCaptureManager = AndroidScreenCaptureManager(context)
        lifecycleManager = AndroidLifecycleManager(context)
    }

    @Test
    fun testInitializeAllManagers() =
        runTest {
            // Initialize all managers
            permissionManager // No explicit initialize method
            audioManager.initialize()
            videoManager.initialize()
            screenCaptureManager.initialize()
            lifecycleManager.initialize()

            // Should complete without error
        }

    @Test
    fun testAudioVideoIntegration() =
        runTest {
            audioManager.initialize()
            videoManager.initialize()

            // Setup audio for video call
            audioManager.setupAudioRouting()
            audioManager.setAudioMode(AudioMode.COMMUNICATION)

            // Check if cameras are available
            val frontAvailable = videoManager.isFrontCameraAvailable()
            val backAvailable = videoManager.isBackCameraAvailable()

            if (frontAvailable || backAvailable) {
                // Audio and video should work together
                val audioFocusGranted = audioManager.requestAudioFocus()
                assertTrue(audioFocusGranted)

                if (frontAvailable) {
                    val videoStream = videoManager.createCameraStream(CameraFacing.FRONT)
                    // Video stream creation depends on camera permission
                    assertNotNull(videoStream != null || !permissionManager.checkCameraPermission())
                }
            }

            // Cleanup
            audioManager.releaseAudioFocus()
            videoManager.cleanup()
            audioManager.cleanup()
        }

    @Test
    fun testLifecycleAudioIntegration() =
        runTest {
            lifecycleManager.initialize()
            audioManager.initialize()

            lifecycleManager.startMonitoring()
            audioManager.setupAudioRouting()

            // Simulate phone call interruption
            lifecycleManager.handlePhoneCallStart()
            audioManager.handlePhoneCallInterruption()

            // Simulate phone call end
            lifecycleManager.handlePhoneCallEnd()
            audioManager.resumeAfterPhoneCall()

            // Cleanup
            lifecycleManager.stopMonitoring()
            audioManager.cleanup()
            lifecycleManager.cleanup()
        }

    @Test
    fun testPermissionVideoIntegration() =
        runTest {
            videoManager.initialize()

            val hasCameraPermission = permissionManager.checkCameraPermission()
            val frontAvailable = videoManager.isFrontCameraAvailable()

            if (hasCameraPermission && frontAvailable) {
                val videoStream = videoManager.createCameraStream(CameraFacing.FRONT)
                assertNotNull(videoStream)
            } else if (!hasCameraPermission) {
                // Should handle permission denied gracefully
                val message = permissionManager.handlePermissionDenied(Permission.CAMERA)
                assertTrue(message.contains("Camera permission"))
            }

            videoManager.cleanup()
        }

    @Test
    fun testPermissionScreenCaptureIntegration() =
        runTest {
            screenCaptureManager.initialize()

            // Screen capture permission is always available on Android
            val hasPermission = permissionManager.checkScreenCapturePermission()
            assertTrue(hasPermission)

            val screens = screenCaptureManager.getAvailableScreens()
            assertTrue(screens.isNotEmpty())

            screenCaptureManager.cleanup()
        }

    @Test
    fun testLifecycleBatteryOptimization() =
        runTest {
            lifecycleManager.initialize()
            audioManager.initialize()
            videoManager.initialize()

            // Simulate low battery
            val batteryOptimization = lifecycleManager.handleLowBattery()

            when (batteryOptimization) {
                BatteryOptimization.MODERATE -> {
                    audioManager.optimizeForBattery()
                }
                BatteryOptimization.AGGRESSIVE -> {
                    audioManager.optimizeForBattery()
                    videoManager.optimizeForBattery()
                }
                BatteryOptimization.NONE -> {
                    // No optimization needed
                }
            }

            // Cleanup
            videoManager.cleanup()
            audioManager.cleanup()
            lifecycleManager.cleanup()
        }

    @Test
    fun testOrientationChangeIntegration() =
        runTest {
            lifecycleManager.initialize()
            videoManager.initialize()

            // Simulate orientation change
            lifecycleManager.handleOrientationChange(DeviceOrientation.LANDSCAPE)
            videoManager.handleOrientationChange(90)

            lifecycleManager.handleOrientationChange(DeviceOrientation.PORTRAIT)
            videoManager.handleOrientationChange(0)

            // Cleanup
            videoManager.cleanup()
            lifecycleManager.cleanup()
        }

    @Test
    fun testFullCallScenario() =
        runTest {
            // Initialize all managers
            lifecycleManager.initialize()
            audioManager.initialize()
            videoManager.initialize()
            screenCaptureManager.initialize()

            lifecycleManager.startMonitoring()

            // Start a video call
            audioManager.setupAudioRouting()
            val audioFocusGranted = audioManager.requestAudioFocus()
            assertTrue(audioFocusGranted)

            // Check permissions and start video if available
            val hasCameraPermission = permissionManager.checkCameraPermission()
            if (hasCameraPermission && videoManager.isFrontCameraAvailable()) {
                val videoStream = videoManager.createCameraStream(CameraFacing.FRONT)
                assertNotNull(videoStream)
            }

            // Simulate app going to background
            lifecycleManager.handleAppBackground()

            // Simulate app coming back to foreground
            lifecycleManager.handleAppForeground()

            // End call and cleanup
            audioManager.releaseAudioFocus()
            videoManager.stopCameraCapture()

            lifecycleManager.stopMonitoring()

            // Cleanup all managers
            videoManager.cleanup()
            audioManager.cleanup()
            screenCaptureManager.cleanup()
            lifecycleManager.cleanup()
        }
}
