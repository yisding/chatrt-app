package ai.chatrt.app.repository

import ai.chatrt.app.models.*
import ai.chatrt.app.network.ChatRtApiService
import kotlin.test.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/**
 * Tests for ChatRepositoryImpl
 * Tests repository functionality, state management, and error handling
 */
class ChatRepositoryTest {
    
    private fun createRepository(): Pair<ChatRepositoryImpl, ChatRtApiService> {
        val apiService = ChatRtApiService("https://test-api.chatrt.com")
        val repository = ChatRepositoryImpl(apiService)
        return repository to apiService
    }
    
    @Test
    fun testInitialConnectionState() = runTest {
        val (repository, apiService) = createRepository()
        try {
            // When
            val initialState = repository.getCurrentConnectionState()
            
            // Then
            assertEquals(ConnectionState.DISCONNECTED, initialState)
            assertFalse(repository.isConnected())
        } finally {
            apiService.close()
        }
    }
    
    @Test
    fun testObserveConnectionState() = runTest {
        val (repository, apiService) = createRepository()
        try {
            // When
            val connectionStateFlow = repository.observeConnectionState()
            val initialState = connectionStateFlow.first()
            
            // Then
            assertEquals(ConnectionState.DISCONNECTED, initialState)
        } finally {
            apiService.close()
        }
    }
    
    @Test
    fun testObserveLogs() = runTest {
        val (repository, apiService) = createRepository()
        try {
            // When
            val logsFlow = repository.observeLogs()
            val initialLogs = logsFlow.first()
            
            // Then
            assertTrue(initialLogs.isEmpty())
        } finally {
            apiService.close()
        }
    }
    
    @Test
    fun testCreateCallUpdatesState() = runTest {
        val (repository, apiService) = createRepository()
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
            
            // Then - The call will fail due to no real server, but state should be updated
            assertTrue(result.isFailure)
            assertEquals(ConnectionState.FAILED, repository.getCurrentConnectionState())
            assertFalse(repository.isConnected())
        } finally {
            apiService.close()
        }
    }
    
    @Test
    fun testStopConnection() = runTest {
        val (repository, apiService) = createRepository()
        try {
            // Given - Simulate a connection state change
            repository.simulateConnectionStateChange(ConnectionState.CONNECTED)
            assertTrue(repository.isConnected())
            
            // When
            val result = repository.stopConnection()
            
            // Then
            assertTrue(result.isSuccess)
            assertEquals(ConnectionState.DISCONNECTED, repository.getCurrentConnectionState())
            assertFalse(repository.isConnected())
        } finally {
            apiService.close()
        }
    }
    
    @Test
    fun testSimulateConnectionStateChange() = runTest {
        val (repository, apiService) = createRepository()
        try {
            // Given
            val initialState = repository.getCurrentConnectionState()
            assertEquals(ConnectionState.DISCONNECTED, initialState)
            
            // When
            repository.simulateConnectionStateChange(ConnectionState.CONNECTING)
            
            // Then
            assertEquals(ConnectionState.CONNECTING, repository.getCurrentConnectionState())
            
            // When
            repository.simulateConnectionStateChange(ConnectionState.CONNECTED)
            
            // Then
            assertEquals(ConnectionState.CONNECTED, repository.getCurrentConnectionState())
            assertTrue(repository.isConnected())
        } finally {
            apiService.close()
        }
    }
    
    @Test
    fun testClearLogs() = runTest {
        val (repository, apiService) = createRepository()
        try {
            // Given - Simulate some connection activity to generate logs
            repository.simulateConnectionStateChange(ConnectionState.CONNECTING)
            repository.simulateConnectionStateChange(ConnectionState.CONNECTED)
            
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
            apiService.close()
        }
    }
    
    @Test
    fun testStartConnectionMonitoring() = runTest {
        val (repository, apiService) = createRepository()
        try {
            // Given
            val callId = "test-call-id"
            
            // When
            val result = repository.startConnectionMonitoring(callId)
            
            // Then - Will fail due to no real server, but should handle gracefully
            assertTrue(result.isFailure)
        } finally {
            apiService.close()
        }
    }
}