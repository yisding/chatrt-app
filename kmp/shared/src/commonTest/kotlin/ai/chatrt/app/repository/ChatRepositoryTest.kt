package ai.chatrt.app.repository

import ai.chatrt.app.models.*
import ai.chatrt.app.network.ChatRtApiService
import ai.chatrt.app.platform.IceConnectionState
import kotlin.test.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay

/**
 * Tests for ChatRepositoryImpl with WebRTC integration
 * Tests repository functionality, state management, and error handling
 */
class ChatRepositoryTest {
    
    private fun createRepository(): Triple<ChatRepositoryImpl, ChatRtApiService, MockWebRtcManager> {
        val apiService = ChatRtApiService("https://test-api.chatrt.com")
        val mockWebRtcManager = MockWebRtcManager()
        val repository = ChatRepositoryImpl(apiService, mockWebRtcManager)
        return Triple(repository, apiService, mockWebRtcManager)
    }
    
    @Test
    fun testInitialConnectionState() = runTest {
        val (repository, apiService, mockWebRtc) = createRepository()
        try {
            // When
            val initialState = repository.getCurrentConnectionState()
            
            // Then
            assertEquals(ConnectionState.DISCONNECTED, initialState)
            assertFalse(repository.isConnected())
        } finally {
            repository.cleanup()
            apiService.close()
        }
    }
    
    @Test
    fun testObserveConnectionState() = runTest {
        val (repository, apiService, mockWebRtc) = createRepository()
        try {
            // When
            val connectionStateFlow = repository.observeConnectionState()
            val initialState = connectionStateFlow.first()
            
            // Then
            assertEquals(ConnectionState.DISCONNECTED, initialState)
        } finally {
            repository.cleanup()
            apiService.close()
        }
    }
    
    @Test
    fun testObserveLogs() = runTest {
        val (repository, apiService, mockWebRtc) = createRepository()
        try {
            // When
            val logsFlow = repository.observeLogs()
            val initialLogs = logsFlow.first()
            
            // Then
            assertTrue(initialLogs.isEmpty())
        } finally {
            repository.cleanup()
            apiService.close()
        }
    }
    
    @Test
    fun testCreateCallUpdatesState() = runTest {
        val (repository, apiService, mockWebRtc) = createRepository()
        try {
            // Given
            val sessionConfig = SessionConfig(
                instructions = "Test instructions",
                audio = AudioConfig(
                    input = AudioInputConfig(
                        noiseReduction = NoiseReductionConfig()
                    ),
                    output = AudioOutputConfig()
                )
            )
            
            val callRequest = CallRequest(
                sdp = "test-sdp-offer",
                session = sessionConfig
            )
            
            // When
            val result = repository.createCall(callRequest)
            
            // Then - The call will fail due to no real server, but WebRTC should be initialized
            assertTrue(result.isFailure)
            assertEquals(ConnectionState.FAILED, repository.getCurrentConnectionState())
            assertFalse(repository.isConnected())
            
            // Verify WebRTC manager was called
            assertTrue(mockWebRtc.initializeCalled)
            assertTrue(mockWebRtc.createOfferCalled)
        } finally {
            repository.cleanup()
            apiService.close()
        }
    }
    
    @Test
    fun testStopConnection() = runTest {
        val (repository, apiService, mockWebRtc) = createRepository()
        try {
            // Given - Simulate a connection state change
            mockWebRtc.simulateConnectionStateChange(ConnectionState.CONNECTED)
            delay(200) // Allow state to propagate
            assertTrue(repository.isConnected())
            
            // When
            val result = repository.stopConnection()
            
            // Then
            assertTrue(result.isSuccess)
            assertEquals(ConnectionState.DISCONNECTED, repository.getCurrentConnectionState())
            assertFalse(repository.isConnected())
            
            // Verify WebRTC manager cleanup was called
            assertTrue(mockWebRtc.removeLocalStreamCalled)
            assertTrue(mockWebRtc.closeCalled)
        } finally {
            repository.cleanup()
            apiService.close()
        }
    }
    
    @Test
    fun testWebRtcConnectionStateChanges() = runTest {
        val (repository, apiService, mockWebRtc) = createRepository()
        try {
            // Given
            val initialState = repository.getCurrentConnectionState()
            assertEquals(ConnectionState.DISCONNECTED, initialState)
            
            // When - Simulate WebRTC state changes
            mockWebRtc.simulateConnectionStateChange(ConnectionState.CONNECTING)
            
            // Wait for state to propagate by observing the flow
            var currentState = repository.observeConnectionState().first()
            var attempts = 0
            while (currentState != ConnectionState.CONNECTING && attempts < 10) {
                delay(50)
                currentState = repository.getCurrentConnectionState()
                attempts++
            }
            
            // Then
            assertEquals(ConnectionState.CONNECTING, repository.getCurrentConnectionState())
            
            // When
            mockWebRtc.simulateConnectionStateChange(ConnectionState.CONNECTED)
            
            // Wait for state to propagate
            attempts = 0
            while (currentState != ConnectionState.CONNECTED && attempts < 10) {
                delay(50)
                currentState = repository.getCurrentConnectionState()
                attempts++
            }
            
            // Then
            assertEquals(ConnectionState.CONNECTED, repository.getCurrentConnectionState())
            assertTrue(repository.isConnected())
        } finally {
            repository.cleanup()
            apiService.close()
        }
    }
    
    @Test
    fun testClearLogs() = runTest {
        val (repository, apiService, mockWebRtc) = createRepository()
        try {
            // Given - Simulate some connection activity to generate logs
            mockWebRtc.simulateConnectionStateChange(ConnectionState.CONNECTING)
            mockWebRtc.simulateConnectionStateChange(ConnectionState.CONNECTED)
            delay(100) // Allow logs to be generated
            
            // Verify logs exist
            val logsBeforeClear = repository.observeLogs().first()
            assertTrue(logsBeforeClear.isNotEmpty())
            
            // When
            repository.clearLogs()
            
            // Then
            val logsAfterClear = repository.observeLogs().first()
            assertEquals(1, logsAfterClear.size) // Only the "Logs cleared" message should remain
            assertEquals("Logs cleared", logsAfterClear.first().message)
        } finally {
            repository.cleanup()
            apiService.close()
        }
    }
    
    @Test
    fun testStartConnectionMonitoring() = runTest {
        val (repository, apiService, mockWebRtc) = createRepository()
        try {
            // Given
            val callId = "test-call-id"
            
            // When
            val result = repository.startConnectionMonitoring(callId)
            
            // Then - Will fail due to no real server, but should handle gracefully
            assertTrue(result.isFailure)
        } finally {
            repository.cleanup()
            apiService.close()
        }
    }
    
    @Test
    fun testIceConnectionStateHandling() = runTest {
        val (repository, apiService, mockWebRtc) = createRepository()
        try {
            // Given - Start with connecting state
            mockWebRtc.simulateConnectionStateChange(ConnectionState.CONNECTING)
            delay(100)
            
            // When - ICE connection succeeds
            mockWebRtc.simulateIceConnectionStateChange(IceConnectionState.CONNECTED)
            delay(100)
            
            // Then - Should update to connected
            assertEquals(ConnectionState.CONNECTED, repository.getCurrentConnectionState())
            
            // When - ICE connection fails
            mockWebRtc.simulateIceConnectionStateChange(IceConnectionState.FAILED)
            delay(100)
            
            // Then - Should update to failed if still connecting
            // (In this case it won't change since we're already connected)
            
            // When - ICE connection disconnects
            mockWebRtc.simulateConnectionStateChange(ConnectionState.CONNECTED) // Reset to connected
            delay(100)
            mockWebRtc.simulateIceConnectionStateChange(IceConnectionState.DISCONNECTED)
            delay(100)
            
            // Then - Should update to disconnected
            assertEquals(ConnectionState.DISCONNECTED, repository.getCurrentConnectionState())
        } finally {
            repository.cleanup()
            apiService.close()
        }
    }
    
    @Test
    fun testSwitchCamera() = runTest {
        val (repository, apiService, mockWebRtc) = createRepository()
        try {
            // Given - Not connected
            assertFalse(repository.isConnected())
            
            // When - Try to switch camera while not connected
            val result1 = repository.switchCamera()
            
            // Then - Should fail
            assertTrue(result1.isFailure)
            assertFalse(mockWebRtc.switchCameraCalled)
            
            // Given - Connected
            mockWebRtc.simulateConnectionStateChange(ConnectionState.CONNECTED)
            delay(200)
            assertTrue(repository.isConnected())
            
            // When - Switch camera while connected
            val result2 = repository.switchCamera()
            
            // Then - Should succeed
            assertTrue(result2.isSuccess)
            assertTrue(mockWebRtc.switchCameraCalled)
        } finally {
            repository.cleanup()
            apiService.close()
        }
    }
    
    @Test
    fun testUpdateVideoMode() = runTest {
        val (repository, apiService, mockWebRtc) = createRepository()
        try {
            // Given - Not connected
            assertFalse(repository.isConnected())
            
            // When - Try to update video mode while not connected
            val result1 = repository.updateVideoMode(VideoMode.WEBCAM)
            
            // Then - Should fail
            assertTrue(result1.isFailure)
            
            // Given - Connected
            mockWebRtc.simulateConnectionStateChange(ConnectionState.CONNECTED)
            delay(200)
            assertTrue(repository.isConnected())
            mockWebRtc.reset() // Reset call flags
            
            // When - Update video mode while connected
            val result2 = repository.updateVideoMode(VideoMode.WEBCAM)
            
            // Then - Should succeed and update stream
            assertTrue(result2.isSuccess)
            assertTrue(mockWebRtc.removeLocalStreamCalled)
            assertTrue(mockWebRtc.addLocalStreamCalled)
        } finally {
            repository.cleanup()
            apiService.close()
        }
    }
    
    @Test
    fun testWebRtcInitializationError() = runTest {
        val (repository, apiService, mockWebRtc) = createRepository()
        try {
            // Given - WebRTC initialization will fail
            mockWebRtc.shouldThrowOnInitialize = true
            
            val sessionConfig = SessionConfig(
                instructions = "Test instructions",
                audio = AudioConfig(
                    input = AudioInputConfig(
                        noiseReduction = NoiseReductionConfig()
                    ),
                    output = AudioOutputConfig()
                )
            )
            
            val callRequest = CallRequest(
                sdp = "test-sdp-offer",
                session = sessionConfig
            )
            
            // When
            val result = repository.createCall(callRequest)
            
            // Then - Should fail gracefully
            assertTrue(result.isFailure)
            assertEquals(ConnectionState.FAILED, repository.getCurrentConnectionState())
            assertTrue(mockWebRtc.initializeCalled)
        } finally {
            repository.cleanup()
            apiService.close()
        }
    }
}