package ai.chatrt.app.platform

import ai.chatrt.app.models.AudioQuality
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
 * Unit tests for AndroidAudioManager
 * Requirements: 1.5, 2.3, 5.3
 */
@RunWith(AndroidJUnit4::class)
class AndroidAudioManagerTest {
    private lateinit var context: Context
    private lateinit var audioManager: AndroidAudioManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        audioManager = AndroidAudioManager(context)
    }

    @Test
    fun testInitialize() =
        runTest {
            audioManager.initialize()
            // Should complete without error
        }

    @Test
    fun testSetupAudioRouting() =
        runTest {
            audioManager.initialize()
            audioManager.setupAudioRouting()
            // Should complete without error
        }

    @Test
    fun testSetAudioMode() =
        runTest {
            audioManager.initialize()

            // Test different audio modes
            audioManager.setAudioMode(AudioMode.COMMUNICATION)
            audioManager.setAudioMode(AudioMode.NORMAL)
            audioManager.setAudioMode(AudioMode.CALL)

            // Should complete without error
        }

    @Test
    fun testRequestAndReleaseAudioFocus() =
        runTest {
            audioManager.initialize()

            val focusGranted = audioManager.requestAudioFocus()
            assertTrue(focusGranted)

            audioManager.releaseAudioFocus()
            // Should complete without error
        }

    @Test
    fun testGetAvailableAudioDevices() =
        runTest {
            audioManager.initialize()

            val devices = audioManager.getAvailableAudioDevices()
            assertTrue(devices.isNotEmpty())

            // Should have at least speaker and earpiece
            val hasEarpiece = devices.any { it.type == AudioDeviceType.EARPIECE }
            val hasSpeaker = devices.any { it.type == AudioDeviceType.SPEAKER }

            assertTrue(hasEarpiece)
            assertTrue(hasSpeaker)
        }

    @Test
    fun testSetAndGetCurrentAudioDevice() =
        runTest {
            audioManager.initialize()

            val devices = audioManager.getAvailableAudioDevices()
            val speakerDevice = devices.first { it.type == AudioDeviceType.SPEAKER }

            audioManager.setAudioDevice(speakerDevice)

            val currentDevice = audioManager.getCurrentAudioDevice()
            assertNotNull(currentDevice)
            assertEquals(speakerDevice.type, currentDevice.type)
        }

    @Test
    fun testObserveAudioDeviceChanges() =
        runTest {
            audioManager.initialize()

            val devices = audioManager.getAvailableAudioDevices()
            val speakerDevice = devices.first { it.type == AudioDeviceType.SPEAKER }

            audioManager.setAudioDevice(speakerDevice)

            val deviceChange = audioManager.observeAudioDeviceChanges().first()
            assertEquals(speakerDevice.type, deviceChange.type)
        }

    @Test
    fun testSetAudioQuality() =
        runTest {
            audioManager.initialize()

            // Test different audio qualities
            audioManager.setAudioQuality(AudioQuality.HIGH)
            audioManager.setAudioQuality(AudioQuality.MEDIUM)
            audioManager.setAudioQuality(AudioQuality.LOW)

            // Should complete without error
        }

    @Test
    fun testHandleHeadsetConnection() =
        runTest {
            audioManager.initialize()
            audioManager.setupAudioRouting()

            // Test headset connection
            audioManager.handleHeadsetConnection(true)
            audioManager.handleHeadsetConnection(false)

            // Should complete without error
        }

    @Test
    fun testPhoneCallInterruption() =
        runTest {
            audioManager.initialize()
            audioManager.setupAudioRouting()

            // Test phone call interruption handling
            audioManager.handlePhoneCallInterruption()
            audioManager.resumeAfterPhoneCall()

            // Should complete without error
        }

    @Test
    fun testOptimizeForBattery() =
        runTest {
            audioManager.initialize()

            audioManager.optimizeForBattery()

            // Should complete without error
        }

    @Test
    fun testCleanup() =
        runTest {
            audioManager.initialize()
            audioManager.setupAudioRouting()

            audioManager.cleanup()

            // Should complete without error
        }
}
