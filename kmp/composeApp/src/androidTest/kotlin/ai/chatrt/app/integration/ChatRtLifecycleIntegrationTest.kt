package ai.chatrt.app.integration

import ai.chatrt.app.models.ConnectionState
import ai.chatrt.app.models.VideoMode
import ai.chatrt.app.platform.AndroidLifecycleManager
import ai.chatrt.app.service.ChatRtService
import ai.chatrt.app.service.ChatRtServiceManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests for ChatRT lifecycle and background behavior
 * Requirements: 5.1, 5.2, 5.3, 5.6
 */
@RunWith(AndroidJUnit4::class)
class ChatRtLifecycleIntegrationTest : KoinTest {
    @get:Rule
    val serviceRule = ServiceTestRule()

    private lateinit var context: Context
    private lateinit var serviceManager: ChatRtServiceManager
    private val lifecycleManager: AndroidLifecycleManager by inject()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        serviceManager = ChatRtServiceManager(context)

        // Initialize Koin for real dependencies
        startKoin {
            modules(
                // Include actual modules for integration testing
                // This would include the real Android module
            )
        }
    }

    @After
    fun tearDown() {
        serviceManager.unbindService()
        stopKoin()
    }

    /**
     * Test complete call lifecycle with background continuation
     * Requirements: 5.1, 5.2
     */
    @Test
    fun testCompleteCallLifecycle() =
        runTest {
            // Start call
            serviceManager.startCall(VideoMode.AUDIO_ONLY, "test-sdp-offer")

            // Bind to service to observe state
            serviceManager.bindService()

            // Wait for service binding
            delay(1000)

            val stateFlows = serviceManager.getServiceStateFlows()
            assertNotNull(stateFlows)

            // Verify call is active
            assertTrue(stateFlows.isCallActive.first())

            // Simulate app going to background
            lifecycleManager.handleAppBackground()

            // Call should continue in background
            assertTrue(stateFlows.isCallActive.first())

            // Simulate phone call interruption
            lifecycleManager.handlePhoneCallStart()

            // Call should be paused
            assertTrue(stateFlows.isCallPaused.first())

            // Simulate phone call end
            lifecycleManager.handlePhoneCallEnd()

            // Call should resume
            assertFalse(stateFlows.isCallPaused.first())

            // End call
            serviceManager.endCall()

            // Wait for cleanup
            delay(500)

            // Call should be inactive
            assertFalse(stateFlows.isCallActive.first())
            assertEquals(ConnectionState.DISCONNECTED, stateFlows.connectionState.first())
        }

    /**
     * Test screen sharing with background continuation
     * Requirements: 3.3, 5.1
     */
    @Test
    fun testScreenSharingBackgroundContinuation() =
        runTest {
            // Start screen sharing call
            serviceManager.startCall(VideoMode.SCREEN_SHARE, "test-sdp-offer")

            // Bind to service
            serviceManager.bindService()
            delay(1000)

            val stateFlows = serviceManager.getServiceStateFlows()
            assertNotNull(stateFlows)

            // Verify call is active
            assertTrue(stateFlows.isCallActive.first())

            // Simulate app going to background
            lifecycleManager.handleAppBackground()

            // Screen sharing should continue in background with notification
            assertTrue(stateFlows.isCallActive.first())

            // End call
            serviceManager.endCall()
            delay(500)

            // Verify cleanup
            assertFalse(stateFlows.isCallActive.first())
        }

    /**
     * Test battery optimization during video call
     * Requirement: 5.6
     */
    @Test
    fun testBatteryOptimizationDuringVideoCall() =
        runTest {
            // Start video call
            serviceManager.startCall(VideoMode.WEBCAM, "test-sdp-offer")

            // Bind to service
            serviceManager.bindService()
            delay(1000)

            val stateFlows = serviceManager.getServiceStateFlows()
            assertNotNull(stateFlows)

            // Verify call is active
            assertTrue(stateFlows.isCallActive.first())

            // Simulate low battery condition
            val lowBatteryOptimization = lifecycleManager.handleLowBattery()

            // Should recommend battery optimization
            assertTrue(lowBatteryOptimization != ai.chatrt.app.platform.BatteryOptimization.NONE)

            // End call
            serviceManager.endCall()
            delay(500)

            assertFalse(stateFlows.isCallActive.first())
        }

    /**
     * Test service restart after system kill
     * Requirement: 5.1
     */
    @Test
    fun testServiceRestartAfterSystemKill() =
        runTest {
            // Start call
            val serviceIntent =
                Intent(context, ChatRtService::class.java).apply {
                    action = ChatRtService.ACTION_START_CALL
                    putExtra(ChatRtService.EXTRA_VIDEO_MODE, VideoMode.AUDIO_ONLY)
                    putExtra(ChatRtService.EXTRA_SDP_OFFER, "test-sdp-offer")
                }

            // Start service
            val binder = serviceRule.bindService(serviceIntent) as ChatRtService.ChatRtBinder
            val service = binder.getService()

            // Verify service is active
            assertTrue(service.isCallActive.first())

            // Simulate system kill and restart by calling onStartCommand again
            val restartResult = service.onStartCommand(serviceIntent, 0, 1)

            // Service should return START_STICKY for restart
            assertEquals(android.app.Service.START_STICKY, restartResult)

            // Service should maintain call state
            assertTrue(service.isCallActive.first())
        }

    /**
     * Test device orientation changes during video call
     * Requirement: 5.4
     */
    @Test
    fun testDeviceOrientationChanges() =
        runTest {
            // Start video call
            serviceManager.startCall(VideoMode.WEBCAM, "test-sdp-offer")

            // Bind to service
            serviceManager.bindService()
            delay(1000)

            val stateFlows = serviceManager.getServiceStateFlows()
            assertNotNull(stateFlows)

            // Verify call is active
            assertTrue(stateFlows.isCallActive.first())

            // Simulate orientation changes
            lifecycleManager.handleDeviceOrientationChange(90) // Landscape
            lifecycleManager.handleDeviceOrientationChange(0) // Portrait
            lifecycleManager.handleDeviceOrientationChange(270) // Landscape reverse

            // Call should remain active through orientation changes
            assertTrue(stateFlows.isCallActive.first())

            // End call
            serviceManager.endCall()
            delay(500)

            assertFalse(stateFlows.isCallActive.first())
        }

    /**
     * Test multiple system interruptions
     * Requirements: 5.2, 5.6
     */
    @Test
    fun testMultipleSystemInterruptions() =
        runTest {
            // Start call
            serviceManager.startCall(VideoMode.WEBCAM, "test-sdp-offer")

            // Bind to service
            serviceManager.bindService()
            delay(1000)

            val stateFlows = serviceManager.getServiceStateFlows()
            assertNotNull(stateFlows)

            // Verify call is active
            assertTrue(stateFlows.isCallActive.first())
            assertFalse(stateFlows.isCallPaused.first())

            // Simulate phone call interruption
            lifecycleManager.handlePhoneCallStart()
            assertTrue(stateFlows.isCallPaused.first())

            // Simulate low battery during phone call
            lifecycleManager.handleLowBattery()

            // Call should still be paused
            assertTrue(stateFlows.isCallPaused.first())

            // Phone call ends
            lifecycleManager.handlePhoneCallEnd()

            // Call should resume with battery optimization
            assertFalse(stateFlows.isCallPaused.first())

            // End call
            serviceManager.endCall()
            delay(500)

            assertFalse(stateFlows.isCallActive.first())
        }
}
