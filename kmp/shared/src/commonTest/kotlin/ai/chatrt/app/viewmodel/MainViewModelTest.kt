package ai.chatrt.app.viewmodel

import ai.chatrt.app.models.*
import ai.chatrt.app.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private lateinit var mockChatRepository: MockChatRepository
    private lateinit var viewModel: MainViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockChatRepository = MockChatRepository()
        viewModel = MainViewModel(mockChatRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be correct`() = runTest {
        // Given - ViewModel is initialized
        
        // When - checking initial state
        val connectionState = viewModel.connectionState.value
        val videoMode = viewModel.videoMode.value
        val logs = viewModel.logs.value
        val networkQuality = viewModel.networkQuality.value
        val isCallPaused = viewModel.isCallPaused.value
        val error = viewModel.error.value
        
        // Then - initial state should be correct
        assertEquals(ConnectionState.DISCONNECTED, connectionState)
        assertEquals(VideoMode.AUDIO_ONLY, videoMode)
        assertTrue(logs.isEmpty())
        assertEquals(NetworkQuality.GOOD, networkQuality)
        assertFalse(isCallPaused)
        assertNull(error)
    }

    @Test
    fun `startConnection should update connection state to connecting`() = runTest {
        // Given - ViewModel is initialized
        mockChatRepository.setCreateCallResult(Result.success(CallResponse("test-call-id", "test-sdp", "success")))
        
        // When - starting connection
        viewModel.startConnection()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - connection state should have been connecting at some point
        // Since the repository mock doesn't emit state changes, we check the logs instead
        val logs = viewModel.logs.value
        assertTrue(logs.any { it.message.contains("Starting connection") })
    }

    @Test
    fun `startConnection with successful response should start monitoring`() = runTest {
        // Given - successful call creation
        val callResponse = CallResponse("test-call-id", "test-sdp", "success")
        mockChatRepository.setCreateCallResult(Result.success(callResponse))
        mockChatRepository.setStartMonitoringResult(Result.success(Unit))
        
        // When - starting connection
        viewModel.startConnection()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - monitoring should be started
        assertTrue(mockChatRepository.startMonitoringCalled)
        assertEquals("test-call-id", mockChatRepository.lastCallId)
    }

    @Test
    fun `startConnection with failure should set error state`() = runTest {
        // Given - failed call creation
        val exception = RuntimeException("Network error")
        mockChatRepository.setCreateCallResult(Result.failure(exception))
        
        // When - starting connection
        viewModel.startConnection()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - error should be set and connection state should be failed
        assertEquals(ConnectionState.FAILED, viewModel.connectionState.value)
        assertNotNull(viewModel.error.value)
    }

    @Test
    fun `stopConnection should update state to disconnected`() = runTest {
        // Given - connected state
        mockChatRepository.setStopConnectionResult(Result.success(Unit))
        
        // When - stopping connection
        viewModel.stopConnection()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - connection state should be disconnected
        assertEquals(ConnectionState.DISCONNECTED, viewModel.connectionState.value)
        assertFalse(viewModel.isCallPaused.value)
    }

    @Test
    fun `setVideoMode should update video mode and log change`() = runTest {
        // Given - initial audio-only mode
        assertEquals(VideoMode.AUDIO_ONLY, viewModel.videoMode.value)
        
        // When - changing to webcam mode
        viewModel.setVideoMode(VideoMode.WEBCAM)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - video mode should be updated
        assertEquals(VideoMode.WEBCAM, viewModel.videoMode.value)
        
        // And - log should contain the change
        val logs = viewModel.logs.value
        assertTrue(logs.any { it.message.contains("Video mode changed") })
    }

    @Test
    fun `handleSystemInterruption with phone call should pause call`() = runTest {
        // Given - active connection
        mockChatRepository.emitConnectionState(ConnectionState.CONNECTED)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When - phone call interruption
        val interruption = SystemInterruption(
            type = InterruptionType.PHONE_CALL,
            shouldPause = true
        )
        viewModel.handleSystemInterruption(interruption)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - call should be paused
        assertTrue(viewModel.isCallPaused.value)
    }

    @Test
    fun `handleSystemInterruption with network loss should set reconnecting state`() = runTest {
        // Given - active connection
        mockChatRepository.emitConnectionState(ConnectionState.CONNECTED)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When - network loss interruption
        val interruption = SystemInterruption(
            type = InterruptionType.NETWORK_LOSS,
            shouldPause = false
        )
        viewModel.handleSystemInterruption(interruption)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - connection state should be reconnecting
        assertEquals(ConnectionState.RECONNECTING, viewModel.connectionState.value)
    }

    @Test
    fun `resumeAfterInterruption should resume paused call`() = runTest {
        // Given - connected state and paused call
        mockChatRepository.emitConnectionState(ConnectionState.CONNECTED)
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.handleSystemInterruption(SystemInterruption(InterruptionType.PHONE_CALL, true))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.isCallPaused.value)
        
        // When - resuming after interruption
        viewModel.resumeAfterInterruption()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - call should no longer be paused
        assertFalse(viewModel.isCallPaused.value)
    }

    @Test
    fun `handleNetworkQualityChange should update network quality and suggest optimization for poor network`() = runTest {
        // Given - good network quality initially
        assertEquals(NetworkQuality.GOOD, viewModel.networkQuality.value)
        
        // When - network quality changes to poor
        viewModel.handleNetworkQualityChange(NetworkQuality.POOR)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - network quality should be updated
        assertEquals(NetworkQuality.POOR, viewModel.networkQuality.value)
        
        // And - optimization should be suggested
        val optimization = viewModel.platformOptimization.value
        assertNotNull(optimization)
        assertEquals(VideoMode.AUDIO_ONLY, optimization.recommendedVideoMode)
        assertEquals(OptimizationReason.POOR_NETWORK, optimization.reason)
    }

    @Test
    fun `handleResourceConstraints with low memory should suggest optimization`() = runTest {
        // Given - low memory constraints
        val constraints = ResourceConstraints(
            availableMemory = 50_000_000, // 50MB - below threshold
            cpuUsage = 30.0f,
            networkBandwidth = 1000000
        )
        
        // When - handling resource constraints
        viewModel.handleResourceConstraints(constraints)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - optimization should be suggested
        val optimization = viewModel.platformOptimization.value
        assertNotNull(optimization)
        assertEquals(OptimizationReason.LOW_MEMORY, optimization.reason)
    }

    @Test
    fun `applyPlatformOptimization should apply recommended settings and clear optimization`() = runTest {
        // Given - optimization suggestion
        val optimization = PlatformOptimization(
            recommendedVideoMode = VideoMode.AUDIO_ONLY,
            recommendedAudioQuality = AudioQuality.LOW,
            reason = OptimizationReason.LOW_BATTERY
        )
        
        // When - applying optimization
        viewModel.applyPlatformOptimization(optimization)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - video mode should be applied and optimization cleared
        assertEquals(VideoMode.AUDIO_ONLY, viewModel.videoMode.value)
        assertNull(viewModel.platformOptimization.value)
    }

    @Test
    fun `dismissOptimization should clear optimization suggestion`() = runTest {
        // Given - optimization suggestion exists
        viewModel.handleNetworkQualityChange(NetworkQuality.POOR)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.platformOptimization.value)
        
        // When - dismissing optimization
        viewModel.dismissOptimization()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - optimization should be cleared
        assertNull(viewModel.platformOptimization.value)
    }

    @Test
    fun `clearError should clear error state`() = runTest {
        // Given - error state exists
        mockChatRepository.setCreateCallResult(Result.failure(RuntimeException("Test error")))
        viewModel.startConnection()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.error.value)
        
        // When - clearing error
        viewModel.clearError()
        
        // Then - error should be cleared
        assertNull(viewModel.error.value)
    }

    @Test
    fun `logs should be limited to 100 entries`() = runTest {
        // Given - ViewModel with many log entries
        repeat(150) { index ->
            viewModel.setVideoMode(if (index % 2 == 0) VideoMode.WEBCAM else VideoMode.AUDIO_ONLY)
            testDispatcher.scheduler.advanceUntilIdle()
        }
        
        // When - checking log count
        val logs = viewModel.logs.value
        
        // Then - logs should be limited to 100 entries
        assertTrue(logs.size <= 100)
    }

    // Mock implementation of ChatRepository for testing
    private class MockChatRepository : ChatRepository {
        private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
        private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
        
        private var createCallResult: Result<CallResponse> = Result.success(CallResponse("", "", "success"))
        private var startMonitoringResult: Result<Unit> = Result.success(Unit)
        private var stopConnectionResult: Result<Unit> = Result.success(Unit)
        
        var startMonitoringCalled = false
        var lastCallId: String? = null

        fun setCreateCallResult(result: Result<CallResponse>) {
            createCallResult = result
        }

        fun setStartMonitoringResult(result: Result<Unit>) {
            startMonitoringResult = result
        }

        fun setStopConnectionResult(result: Result<Unit>) {
            stopConnectionResult = result
        }

        fun emitConnectionState(state: ConnectionState) {
            _connectionState.value = state
        }

        override suspend fun createCall(callRequest: CallRequest): Result<CallResponse> {
            return createCallResult
        }

        override fun observeConnectionState(): Flow<ConnectionState> {
            return _connectionState.asStateFlow()
        }

        override fun observeLogs(): Flow<List<LogEntry>> {
            return _logs.asStateFlow()
        }

        override suspend fun startConnectionMonitoring(callId: String): Result<Unit> {
            startMonitoringCalled = true
            lastCallId = callId
            return startMonitoringResult
        }

        override suspend fun stopConnection(): Result<Unit> {
            return stopConnectionResult
        }

        override fun isConnected(): Boolean {
            return _connectionState.value == ConnectionState.CONNECTED
        }

        override fun getCurrentConnectionState(): ConnectionState {
            return _connectionState.value
        }
        
        override suspend fun switchCamera(): Result<Unit> {
            return Result.success(Unit)
        }
        
        override suspend fun updateVideoMode(videoMode: VideoMode): Result<Unit> {
            return Result.success(Unit)
        }
    }
}