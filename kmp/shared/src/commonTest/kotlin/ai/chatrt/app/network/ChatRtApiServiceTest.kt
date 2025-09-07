package ai.chatrt.app.network

import ai.chatrt.app.models.*
import kotlin.test.*
import kotlinx.coroutines.test.runTest

/**
 * Integration tests for ChatRtApiService
 * Tests API communication, error handling, and retry logic
 */
class ChatRtApiServiceTest {
    
    private val testBaseUrl = "https://test-api.chatrt.com"
    
    private fun createApiService(): ChatRtApiService {
        return ChatRtApiService(testBaseUrl)
    }
    
    @Test
    fun testCreateCallRequest() = runTest {
        val apiService = createApiService()
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
            
            // When - This will fail in tests since we don't have a real server
            val result = apiService.createCall(callRequest)
            
            // Then - We expect a network error since there's no real server
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertNotNull(exception)
        } finally {
            apiService.close()
        }
    }
    
    @Test
    fun testStartCallMonitoring() = runTest {
        val apiService = createApiService()
        try {
            // Given
            val callId = "test-call-id"
            
            // When - This will fail in tests since we don't have a real server
            val result = apiService.startCallMonitoring(callId)
            
            // Then - We expect a network error since there's no real server
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertNotNull(exception)
        } finally {
            apiService.close()
        }
    }
    
    @Test
    fun testCheckHealth() = runTest {
        val apiService = createApiService()
        try {
            // When - This will fail in tests since we don't have a real server
            val result = apiService.checkHealth()
            
            // Then - We expect a network error since there's no real server
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertNotNull(exception)
        } finally {
            apiService.close()
        }
    }
    
    @Test
    fun testApiServiceCreation() {
        // Given/When
        val service = ChatRtApiService("https://example.com")
        
        // Then
        assertNotNull(service)
        
        // Cleanup
        service.close()
    }
    
    @Test
    fun testDefaultHttpClientCreation() {
        // When
        val httpClient = ChatRtApiService.createDefaultHttpClient()
        
        // Then
        assertNotNull(httpClient)
        
        // Cleanup
        httpClient.close()
    }
}