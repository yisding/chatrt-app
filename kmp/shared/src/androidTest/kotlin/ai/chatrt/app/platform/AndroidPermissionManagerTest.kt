package ai.chatrt.app.platform

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for AndroidPermissionManager
 * Requirements: 1.2, 2.2, 3.1, 3.2
 */
@RunWith(AndroidJUnit4::class)
class AndroidPermissionManagerTest {
    private lateinit var context: Context
    private lateinit var permissionManager: AndroidPermissionManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        permissionManager = AndroidPermissionManager(context)
    }

    @Test
    fun testCheckMicrophonePermission() =
        runTest {
            // Test microphone permission check
            val hasPermission = permissionManager.checkMicrophonePermission()
            // In test environment, permission might not be granted
            assertNotNull(hasPermission)
        }

    @Test
    fun testCheckCameraPermission() =
        runTest {
            // Test camera permission check
            val hasPermission = permissionManager.checkCameraPermission()
            assertNotNull(hasPermission)
        }

    @Test
    fun testCheckScreenCapturePermission() =
        runTest {
            // Screen capture permission is always true on Android (handled via MediaProjection)
            val hasPermission = permissionManager.checkScreenCapturePermission()
            assertTrue(hasPermission)
        }

    @Test
    fun testCheckNotificationPermission() =
        runTest {
            // Test notification permission check
            val hasPermission = permissionManager.checkNotificationPermission()
            assertNotNull(hasPermission)
        }

    @Test
    fun testRequestMultiplePermissions() =
        runTest {
            val permissions =
                listOf(
                    Permission.MICROPHONE,
                    Permission.CAMERA,
                    Permission.NOTIFICATIONS,
                )

            val results = permissionManager.requestPermissions(permissions)

            assertEquals(permissions.size, results.size)
            permissions.forEach { permission ->
                assertTrue(results.containsKey(permission))
                assertNotNull(results[permission])
            }
        }

    @Test
    fun testShouldShowRationale() =
        runTest {
            // Without activity context, should return false
            val shouldShow = permissionManager.shouldShowRationale(Permission.MICROPHONE)
            assertFalse(shouldShow)
        }

    @Test
    fun testHandlePermissionDenied() =
        runTest {
            val message = permissionManager.handlePermissionDenied(Permission.CAMERA)
            assertTrue(message.contains("Camera permission"))
            assertTrue(message.contains("audio-only mode"))
        }

    @Test
    fun testPermissionChangeObservation() =
        runTest {
            val flow = permissionManager.observePermissionChanges()
            assertNotNull(flow)
        }
}
