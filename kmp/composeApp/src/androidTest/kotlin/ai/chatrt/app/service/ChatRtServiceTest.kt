package ai.chatrt.app.service

import ai.chatrt.app.models.ConnectionState
import ai.chatrt.app.models.InterruptionType
import ai.chatrt.app.models.SystemInterruption
import ai.chatrt.app.models.VideoMode
import ai.chatrt.app.platform.AndroidLifecycleManager
import ai.chatrt.app.platform.AndroidWebRtcManager
import ai.chatrt.app.repository.ChatRepository
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for ChatRtService lifecycle and background behavior
 * Requirements: 5.1, 5.2, 5.3, 5.6
 */
@RunWith(AndroidJUnit4::class)
class ChatRtServiceTest {
    @get:Rule
    val serviceRule = ServiceTestRule()

    private lateinit var context: Context
    private lateinit var mockChatRepository: ChatRepository
    private lateinit var mockLifecycleManager: AndroidLifecycleManager
    private lateinit var mockWebRtcManager: AndroidWebRtcManager
    private lateinit var systemInterruptionsFlow: MutableSharedFlow<SystemInterruption>

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Create mocks
        mockChatRepository = mockk(relaxed = true)
        mockLifecycleManager = mockk(relaxed = true)
        mockWebRtcManager = mockk(relaxed = true)
        systemInterruptionsFlow = MutableSharedFlow()

        // Setup mock behaviors
        every { mockLifecycleManager.observeSystemInterruptions() } returns systemInterruptionsFlow
        coEvery { mockChatRepository.createCall(any()) } returns Result.success("test-response")

        // Setup Koin for dependency injection
        startKoin {
            modules(
                module {
                    single<ChatRepository> { mockChatRepository }
                    single<AndroidLifecycleManager> { mockLifecycleManager }
                    single<AndroidWebRtcManager> { mockWebRtcManager }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    /**
     * Test service starts and maintains call in background
     * Requirement: 5.1
     */
    @Test
    fun testBackgroundCallContinuation() =
        runTest {
            // Start service with call
            val serviceIntent =
                Intent(context, ChatRtService::class.java).apply {
                    action = ChatRtService.ACTION_START_CALL
                    putExtra(ChatRtService.EXTRA_VIDEO_MODE, VideoMode.AUDIO_ONLY)
                    putExtra(ChatRtService.EXTRA_SDP_OFFER, "test-sdp-offer")
                }

            val binder = serviceRule.bindService(serviceIntent) as ChatRtService.ChatRtBinder
            val service = binder.getService()

            // Verify service starts call
            assertTrue(service.isCallActive.first())
            assertEquals(ConnectionState.CONNECTING, service.connectionState.first())

            // Verify repository was called
            coVerify { mockChatRepository.createCall("test-sdp-offer") }
        }

    /**
     * Test phone call interruption pauses ChatRT session
     * Requirement: 5.2
     */
    @Test
    fun testPhoneCallInterruption() =
        runTest {
            // Start service with active call
            val serviceIntent =
                Intent(context, ChatRtService::class.java).apply {
                    action = ChatRtService.ACTION_START_CALL
                    putExtra(ChatRtService.EXTRA_VIDEO_MODE, VideoMode.AUDIO_ONLY)
                    putExtra(ChatRtService.EXTRA_SDP_OFFER, "test-sdp-offer")
                }

            val binder = serviceRule.bindService(serviceIntent) as ChatRtService.ChatRtBinder
            val service = binder.getService()

            // Wait for call to be active
            assertTrue(service.isCallActive.first())

            // Simulate phone call interruption
            val phoneCallInterruption =
                SystemInterruption(
                    type = InterruptionType.PHONE_CALL,
                    shouldPause = true,
                    canResume = true,
                )
            systemInterruptionsFlow.emit(phoneCallInterruption)

            // Verify call is paused
            assertTrue(service.isCallPaused.first())
            verify { mockWebRtcManager.muteAudio(true) }

            // Simulate phone call end
            val phoneCallEnd =
                SystemInterruption(
                    type = InterruptionType.PHONE_CALL,
                    shouldPause = false,
                    canResume = true,
                )
            systemInterruptionsFlow.emit(phoneCallEnd)

            // Verify call is resumed
            assertFalse(service.isCallPaused.first())
            verify { mockWebRtcManager.muteAudio(false) }
        }

    /**
     * Test battery optimization during low power mode
     * Requirement: 5.6
     */
    @Test
    fun testBatteryOptimization() =
        runTest {
            // Start service with video call
            val serviceIntent =
                Intent(context, ChatRtService::class.java).apply {
                    action = ChatRtService.ACTION_START_CALL
                    putExtra(ChatRtService.EXTRA_VIDEO_MODE, VideoMode.WEBCAM)
                    putExtra(ChatRtService.EXTRA_SDP_OFFER, "test-sdp-offer")
                }

            val binder = serviceRule.bindService(serviceIntent) as ChatRtService.ChatRtBinder
            val service = binder.getService()

            // Wait for call to be active
            assertTrue(service.isCallActive.first())

            // Simulate low power mode
            val lowPowerInterruption =
                SystemInterruption(
                    type = InterruptionType.LOW_POWER_MODE,
                    shouldPause = false,
                    canResume = true,
                )
            systemInterruptionsFlow.emit(lowPowerInterruption)

            // Verify video is muted for battery saving
            verify { mockWebRtcManager.muteVideo(true) }
        }

    /**
     * Test service cleanup on destroy
     * Requirement: 5.1
     */
    @Test
    fun testServiceCleanup() =
        runTest {
            // Start service with call
            val serviceIntent =
                Intent(context, ChatRtService::class.java).apply {
                    action = ChatRtService.ACTION_START_CALL
                    putExtra(ChatRtService.EXTRA_VIDEO_MODE, VideoMode.AUDIO_ONLY)
                    putExtra(ChatRtService.EXTRA_SDP_OFFER, "test-sdp-offer")
                }

            val binder = serviceRule.bindService(serviceIntent) as ChatRtService.ChatRtBinder
            val service = binder.getService()

            // Verify call is active
            assertTrue(service.isCallActive.first())

            // End call
            val endCallIntent =
                Intent(context, ChatRtService::class.java).apply {
                    action = ChatRtService.ACTION_END_CALL
                }
            context.startService(endCallIntent)

            // Verify cleanup
            assertFalse(service.isCallActive.first())
            assertEquals(ConnectionState.DISCONNECTED, service.connectionState.first())
            verify { mockWebRtcManager.close() }
        }

    /**
     * Test service restart after system kill
     * Requirement: 5.1
     */
    @Test
    fun testServiceRestart() =
        runTest {
            // Start service with call
            val serviceIntent =
                Intent(context, ChatRtService::class.java).apply {
                    action = ChatRtService.ACTION_START_CALL
                    putExtra(ChatRtService.EXTRA_VIDEO_MODE, VideoMode.AUDIO_ONLY)
                    putExtra(ChatRtService.EXTRA_SDP_OFFER, "test-sdp-offer")
                }

            // Service should return START_STICKY for restart behavior
            val binder = serviceRule.bindService(serviceIntent) as ChatRtService.ChatRtBinder
            val service = binder.getService()

            // Verify service is configured for restart
            assertTrue(service.isCallActive.first())

            // The actual restart behavior would be tested in integration tests
            // as it requires system-level service management
        }

    /**
     * Test pause and resume actions
     * Requirement: 5.2
     */
    @Test
    fun testPauseResumeActions() =
        runTest {
            // Start service with call
            val serviceIntent =
                Intent(context, ChatRtService::class.java).apply {
                    action = ChatRtService.ACTION_START_CALL
                    putExtra(ChatRtService.EXTRA_VIDEO_MODE, VideoMode.WEBCAM)
                    putExtra(ChatRtService.EXTRA_SDP_OFFER, "test-sdp-offer")
                }

            val binder = serviceRule.bindService(serviceIntent) as ChatRtService.ChatRtBinder
            val service = binder.getService()

            // Wait for call to be active
            assertTrue(service.isCallActive.first())
            assertFalse(service.isCallPaused.first())

            // Pause call
            val pauseIntent =
                Intent(context, ChatRtService::class.java).apply {
                    action = ChatRtService.ACTION_PAUSE_CALL
                }
            context.startService(pauseIntent)

            // Verify call is paused
            assertTrue(service.isCallPaused.first())
            verify { mockWebRtcManager.muteAudio(true) }
            verify { mockWebRtcManager.muteVideo(true) }

            // Resume call
            val resumeIntent =
                Intent(context, ChatRtService::class.java).apply {
                    action = ChatRtService.ACTION_RESUME_CALL
                }
            context.startService(resumeIntent)

            // Verify call is resumed
            assertFalse(service.isCallPaused.first())
            verify { mockWebRtcManager.muteAudio(false) }
            verify { mockWebRtcManager.muteVideo(false) }
        }
}
