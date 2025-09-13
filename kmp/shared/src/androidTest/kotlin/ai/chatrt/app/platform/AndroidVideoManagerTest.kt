package ai.chatrt.app.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for AndroidVideoManager
 * Requirements: 2.2, 2.3, 2.4, 2.5
 */
@RunWith(AndroidJUnit4::class)
class AndroidVideoManagerTest {
    private lateinit var context: Context
    private lateinit var videoManager: AndroidVideoManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        videoManager = AndroidVideoManager(context)
    }

    @Test
    fun testInitialize() =
        runTest {
            videoManager.initialize()
            // Should complete without error
        }

    @Test
    fun testGetAvailableCameras() =
        runTest {
            videoManager.initialize()

            val cameras = videoManager.getAvailableCameras()
            assertNotNull(cameras)

            // Most devices should have at least one camera
            if (cameras.isNotEmpty()) {
                val camera = cameras.first()
                assertNotNull(camera.id)
                assertNotNull(camera.name)
                assertNotNull(camera.facing)
                assertTrue(camera.supportedResolutions.isNotEmpty())
                assertTrue(camera.supportedFrameRates.isNotEmpty())
            }
        }

    @Test
    fun testIsCameraAvailable() =
        runTest {
            videoManager.initialize()

            val frontAvailable = videoManager.isFrontCameraAvailable()
            val backAvailable = videoManager.isBackCameraAvailable()

            assertNotNull(frontAvailable)
            assertNotNull(backAvailable)
        }

    @Test
    fun testCreateCameraStream() =
        runTest {
            videoManager.initialize()

            // Only test if camera permission is available and cameras exist
            if (videoManager.isFrontCameraAvailable()) {
                val stream = videoManager.createCameraStream(CameraFacing.FRONT)

                if (stream != null) {
                    assertNotNull(stream.id)
                    assertTrue(stream.resolution.width > 0)
                    assertTrue(stream.resolution.height > 0)
                    assertTrue(stream.frameRate > 0)
                }
            }
        }

    @Test
    fun testObserveCameraState() =
        runTest {
            videoManager.initialize()

            val initialState = videoManager.observeCameraState().first()
            assertEquals(CameraState.IDLE, initialState)
        }

    @Test
    fun testSwitchCamera() =
        runTest {
            videoManager.initialize()

            val newFacing = videoManager.switchCamera()

            // Should return a valid facing or null if switching failed
            assertNotNull(newFacing)
        }

    @Test
    fun testSetCameraResolution() =
        runTest {
            videoManager.initialize()

            val resolution = Resolution(1280, 720)
            videoManager.setCameraResolution(resolution)

            // Should complete without error
        }

    @Test
    fun testSetCameraFrameRate() =
        runTest {
            videoManager.initialize()

            videoManager.setCameraFrameRate(30)

            // Should complete without error
        }

    @Test
    fun testSetCameraFlash() =
        runTest {
            videoManager.initialize()

            videoManager.setCameraFlash(true)
            videoManager.setCameraFlash(false)

            // Should complete without error
        }

    @Test
    fun testGetCameraCapabilities() =
        runTest {
            videoManager.initialize()

            // Create a camera stream first to set current camera
            if (videoManager.isFrontCameraAvailable()) {
                videoManager.createCameraStream(CameraFacing.FRONT)

                val capabilities = videoManager.getCameraCapabilities()

                if (capabilities != null) {
                    assertTrue(capabilities.supportedResolutions.isNotEmpty())
                    assertTrue(capabilities.supportedFrameRates.isNotEmpty())
                    assertNotNull(capabilities.hasFlash)
                    assertNotNull(capabilities.hasAutoFocus)
                    assertTrue(capabilities.maxZoom >= 1.0f)
                    assertTrue(capabilities.supportedFocusModes.isNotEmpty())
                }
            }
        }

    @Test
    fun testHandleOrientationChange() =
        runTest {
            videoManager.initialize()

            videoManager.handleOrientationChange(90)
            videoManager.handleOrientationChange(0)

            // Should complete without error
        }

    @Test
    fun testOptimizeForBattery() =
        runTest {
            videoManager.initialize()

            videoManager.optimizeForBattery()

            // Should complete without error
        }

    @Test
    fun testAdaptVideoQuality() =
        runTest {
            videoManager.initialize()

            videoManager.adaptVideoQuality(VideoQuality.HIGH)
            videoManager.adaptVideoQuality(VideoQuality.MEDIUM)
            videoManager.adaptVideoQuality(VideoQuality.LOW)

            // Should complete without error
        }

    @Test
    fun testCleanup() =
        runTest {
            videoManager.initialize()

            videoManager.cleanup()

            val finalState = videoManager.observeCameraState().first()
            assertEquals(CameraState.CLOSED, finalState)
        }
}
