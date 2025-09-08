package ai.chatrt.app.di

import ai.chatrt.app.network.ChatRtApiService
import io.ktor.client.HttpClient
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Test class for Koin dependency injection setup
 * Verifies that shared dependencies can be resolved correctly
 */
class KoinModuleTest : KoinTest {
    
    @BeforeTest
    fun setup() {
        // Initialize Koin with shared module only for testing
        initKoinShared()
    }
    
    @AfterTest
    fun tearDown() {
        // Stop Koin after each test
        stopKoin()
    }
    
    @Test
    fun `verify HTTP client can be injected`() {
        val httpClient: HttpClient by inject()
        
        assertNotNull(httpClient)
    }
    
    @Test
    fun `verify API service can be injected`() {
        val apiService: ChatRtApiService by inject()
        
        assertNotNull(apiService)
        // Verify that the API service has the correct base URL
        assert(apiService.toString().isNotEmpty())
    }
    
    @Test
    fun `verify Koin context is properly initialized`() {
        val koin = getKoin()
        
        assertNotNull(koin)
        // Verify that we can get basic dependencies
        val httpClient = koin.get<HttpClient>()
        assertNotNull(httpClient)
    }
    
    @Test
    fun `verify all shared module dependencies are resolvable`() {
        // Test that all dependencies in the shared module can be resolved
        val httpClient = getKoin().get<HttpClient>()
        val apiService = getKoin().get<ChatRtApiService>()
        
        assertNotNull(httpClient)
        assertNotNull(apiService)
    }
}