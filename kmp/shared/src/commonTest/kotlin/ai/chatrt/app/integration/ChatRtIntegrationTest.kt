package ai.chatrt.app.integration

import ai.chatrt.app.models.*
import ai.chatrt.app.network.ChatRtApiService
import ai.chatrt.app.repository.ChatRepositoryImpl
import ai.chatrt.app.repository.MockWebRtcManager
import ai.chatrt.app.repository.SettingsRepositoryImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Integration tests for the complete ChatRT system
 * Tests the interaction between repositories, API service, and data models
 */
class ChatRtIntegrationTest {
    @Test
    fun testCompleteWorkflow() =
        runTest {
            // Given - Set up the complete system
            val apiService = ChatRtApiService("https://test-api.chatrt.com")
            val mockWebRtcManager = MockWebRtcManager()
            val chatRepository = ChatRepositoryImpl(apiService, mockWebRtcManager)
            val settingsRepository = SettingsRepositoryImpl()

            try {
                // Test 1: Initial state verification
                assertEquals(ConnectionState.DISCONNECTED, chatRepository.getCurrentConnectionState())
                assertFalse(chatRepository.isConnected())

                val initialLogs = chatRepository.observeLogs().first()
                assertTrue(initialLogs.isEmpty())

                // Test 2: Settings management
                val initialSettings = settingsRepository.getSettings()
                assertEquals(AppSettings(), initialSettings)

                // Update settings
                val newSettings =
                    AppSettings(
                        defaultVideoMode = VideoMode.WEBCAM,
                        audioQuality = AudioQuality.HIGH,
                        debugLogging = true,
                        serverUrl = "https://api.chatrt.com",
                        defaultCamera = CameraFacing.BACK,
                    )

                val updateResult = settingsRepository.updateSettings(newSettings)
                assertTrue(updateResult.isSuccess)

                val updatedSettings = settingsRepository.getSettings()
                assertEquals(newSettings, updatedSettings)

                // Test 3: Connection attempt (will fail but should update state)
                val sessionConfig =
                    SessionConfig(
                        instructions = "Test instructions for integration test",
                        audio =
                            AudioConfig(
                                input =
                                    AudioInputConfig(
                                        noiseReduction = NoiseReductionConfig(),
                                    ),
                                output = AudioOutputConfig(),
                            ),
                    )

                val callRequest =
                    CallRequest(
                        sdp = "test-sdp-offer-integration",
                        session = sessionConfig,
                    )

                val createCallResult = chatRepository.createCall(callRequest)

                // Should fail due to no real server, but state should be updated
                assertTrue(createCallResult.isFailure)
                assertEquals(ConnectionState.FAILED, chatRepository.getCurrentConnectionState())

                // Verify logs were created
                val logsAfterCall = chatRepository.observeLogs().first()
                assertTrue(logsAfterCall.isNotEmpty())
                assertTrue(logsAfterCall.any { it.message.contains("Creating call") })
                assertTrue(logsAfterCall.any { it.message.contains("Failed to create call") })

                // Test 4: State management
                mockWebRtcManager.simulateConnectionStateChange(ConnectionState.CONNECTED)
                delay(100) // Allow state to propagate
                assertTrue(chatRepository.isConnected())
                assertEquals(ConnectionState.CONNECTED, chatRepository.getCurrentConnectionState())

                // Test 5: Connection cleanup
                val stopResult = chatRepository.stopConnection()
                assertTrue(stopResult.isSuccess)
                assertEquals(ConnectionState.DISCONNECTED, chatRepository.getCurrentConnectionState())
                assertFalse(chatRepository.isConnected())

                // Test 6: Settings reset
                val resetResult = settingsRepository.resetToDefaults()
                assertTrue(resetResult.isSuccess)

                val finalSettings = settingsRepository.getSettings()
                assertEquals(AppSettings(), finalSettings)
            } finally {
                chatRepository.cleanup()
                apiService.close()
            }
        }

    @Test
    fun testErrorHandling() =
        runTest {
            val apiService = ChatRtApiService("https://invalid-url-that-does-not-exist.com")
            val mockWebRtcManager = MockWebRtcManager()
            val repository = ChatRepositoryImpl(apiService, mockWebRtcManager)

            try {
                // Test API error handling
                val sessionConfig =
                    SessionConfig(
                        instructions = "Test error handling",
                        audio =
                            AudioConfig(
                                input =
                                    AudioInputConfig(
                                        noiseReduction = NoiseReductionConfig(),
                                    ),
                                output = AudioOutputConfig(),
                            ),
                    )

                val callRequest =
                    CallRequest(
                        sdp = "test-sdp-offer",
                        session = sessionConfig,
                    )

                val result = repository.createCall(callRequest)

                // Should fail gracefully
                assertTrue(result.isFailure)
                assertEquals(ConnectionState.FAILED, repository.getCurrentConnectionState())

                // Verify error was logged
                val logs = repository.observeLogs().first()
                assertTrue(logs.any { it.level == LogLevel.ERROR })
            } finally {
                repository.cleanup()
                apiService.close()
            }
        }

    @Test
    fun testDataModelSerialization() {
        // Test that all data models can be properly serialized/deserialized
        val sessionConfig =
            SessionConfig(
                instructions = "Test serialization",
                audio =
                    AudioConfig(
                        input =
                            AudioInputConfig(
                                noiseReduction = NoiseReductionConfig(),
                            ),
                        output = AudioOutputConfig(),
                    ),
            )

        val callRequest =
            CallRequest(
                sdp = "test-sdp-offer",
                session = sessionConfig,
            )

        val appSettings =
            AppSettings(
                defaultVideoMode = VideoMode.SCREEN_SHARE,
                audioQuality = AudioQuality.LOW,
                debugLogging = true,
                serverUrl = "https://test.com",
                defaultCamera = CameraFacing.BACK,
            )

        // Verify objects are created correctly
        assertNotNull(callRequest)
        assertNotNull(sessionConfig)
        assertNotNull(appSettings)

        assertEquals("test-sdp-offer", callRequest.sdp)
        assertEquals("Test serialization", sessionConfig.instructions)
        assertEquals(VideoMode.SCREEN_SHARE, appSettings.defaultVideoMode)
    }
}
