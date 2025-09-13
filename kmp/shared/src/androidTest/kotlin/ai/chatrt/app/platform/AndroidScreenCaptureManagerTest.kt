package ai.chatrt.app.platform

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for AndroidScreenCaptureManager
 * Requirements: 3.1, 3.2, 3.3, 3.4
 */
@RunWith(AndroidJUnit4::class)
class AndroidScreenCaptureManagerTest {
    private lateinit var context: Context
    private lateinit var screenCaptureManager: AndroidScreenCaptureManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        screenCaptureManager = AndroidScreenCaptureManager(context)
    }

    @Test
    fun testInitialize() =
        runTest {
            screenCaptureManager.initialize()
            // Should complete without error
        }

    @Test
    fun testObserveScreenCaptureState() =
        runTest {
            screenCaptureManager.initialize()

            val initialState = screenCaptureManager.observeScreenCaptureState().first()
            assertEquals(ScreenCaptureState.IDLE, initialState)
        }

    @Test
    fun testIsScreenCaptureActive() =
        runTest {
            screenCaptureManager.initialize()

            val isActive = screenCaptureManager.isScreenCaptureActive()
            assertFalse(isActive)
        }

    @Test
    fun testGetAvailableScreens() =
        runTest {
            screenCaptureManager.initialize()

            val screens = screenCaptureManager.getAvailableScreens()
            assertTrue(screens.isNotEmpty())

            val mainScreen = screens.first()
            assertEquals("main", mainScreen.id)
            assertEquals("Main Display", mainScreen.name)
            assertTrue(mainScreen.isPrimary)
            assertTrue(mainScreen.resolution.width > 0)
            assertTrue(mainScreen.resolution.height > 0)
        }

    @Test
    fun testGetScreenCaptureCapabilities() =
        runTest {
            screenCaptureManager.initialize()

            val capabilities = screenCaptureManager.getScreenCaptureCapabilities()
            assertNotNull(capabilities)

            assertTrue(capabilities!!.supportedResolutions.isNotEmpty())
            assertTrue(capabilities.supportedFrameRates.isNotEmpty())
            assertTrue(capabilities.supportsAudioCapture)
            assertEquals(1, capabilities.maxScreens)
            assertTrue(capabilities.requiresNotification)
        }

    @Test
    fun testSetScreenCaptureQuality() =
        runTest {
            screenCaptureManager.initialize()

            screenCaptureManager.setScreenCaptureQuality(ScreenCaptureQuality.HIGH)
            screenCaptureManager.setScreenCaptureQuality(ScreenCaptureQuality.MEDIUM)
            screenCaptureManager.setScreenCaptureQuality(ScreenCaptureQuality.LOW)
            screenCaptureManager.setScreenCaptureQuality(ScreenCaptureQuality.AUTO)

            // Should complete without error
        }

    @Test
    fun testStartScreenCaptureWithoutPermission() =
        runTest {
            screenCaptureManager.initialize()

            // Without proper MediaProjection intent, should return null
            val stream = screenCaptureManager.startScreenCapture(null)
            assertEquals(null, stream)
        }

    @Test
    fun testStartScreenCaptureWithInvalidIntent() =
        runTest {
            screenCaptureManager.initialize()

            // With invalid intent, should return null
            val invalidIntent = Intent()
            val stream = screenCaptureManager.startScreenCapture(invalidIntent)
            assertEquals(null, stream)
        }

    @Test
    fun testShowAndHideScreenCaptureNotification() =
        runTest {
            screenCaptureManager.initialize()

            screenCaptureManager.showScreenCaptureNotification()
            screenCaptureManager.hideScreenCaptureNotification()

            // Should complete without error
        }

    @Test
    fun testStopScreenCapture() =
        runTest {
            screenCaptureManager.initialize()

            screenCaptureManager.stopScreenCapture()

            val state = screenCaptureManager.observeScreenCaptureState().first()
            assertEquals(ScreenCaptureState.IDLE, state)

            val isActive = screenCaptureManager.isScreenCaptureActive()
            assertFalse(isActive)
        }

    @Test
    fun testEnableDisableBackgroundScreenSharing() =
        runTest {
            screenCaptureManager.initialize()

            screenCaptureManager.enableBackgroundScreenSharing()
            screenCaptureManager.disableBackgroundScreenSharing()

            // Should complete without error
        }

    @Test
    fun testHandlePermissionDenied() =
        runTest {
            screenCaptureManager.initialize()

            val message = screenCaptureManager.handlePermissionDenied()
            assertTrue(message.contains("Screen capture permission"))
            assertTrue(message.contains("camera mode"))
        }

    @Test
    fun testCleanup() =
        runTest {
            screenCaptureManager.initialize()

            screenCaptureManager.cleanup()

            val finalState = screenCaptureManager.observeScreenCaptureState().first()
            assertEquals(ScreenCaptureState.IDLE, finalState)
        }
}
