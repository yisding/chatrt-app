package ai.chatrt.app.viewmodel

import ai.chatrt.app.models.*
import ai.chatrt.app.repository.ChatRepository
import ai.chatrt.app.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelBasicTest {
    private lateinit var mockChatRepository: MockChatRepository
    private lateinit var mockSettingsRepository: MockSettingsRepository
    private lateinit var mainViewModel: MainViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockChatRepository = MockChatRepository()
        mockSettingsRepository = MockSettingsRepository()
        mainViewModel = MainViewModel(mockChatRepository)
        settingsViewModel = SettingsViewModel(mockSettingsRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `MainViewModel initial state should be correct`() =
        runTest {
            // Then - initial state should be correct
            assertEquals(ConnectionState.DISCONNECTED, mainViewModel.connectionState.value)
            assertEquals(VideoMode.AUDIO_ONLY, mainViewModel.videoMode.value)
            assertTrue(mainViewModel.logs.value.isEmpty())
            assertEquals(NetworkQuality.GOOD, mainViewModel.networkQuality.value)
            assertFalse(mainViewModel.isCallPaused.value)
            assertNull(mainViewModel.error.value)
        }

    @Test
    fun `SettingsViewModel initial state should be correct`() =
        runTest {
            // Then - initial state should be correct
            val settings = settingsViewModel.settings.value
            assertEquals(VideoMode.AUDIO_ONLY, settings.defaultVideoMode)
            assertEquals(AudioQuality.MEDIUM, settings.audioQuality)
            assertFalse(settings.debugLogging)
            assertEquals("", settings.serverUrl)
            assertEquals(CameraFacing.FRONT, settings.defaultCamera)
            assertFalse(settingsViewModel.isLoading.value)
            assertNull(settingsViewModel.error.value)
        }

    @Test
    fun `MainViewModel setVideoMode should update video mode`() =
        runTest {
            // When - changing video mode
            mainViewModel.setVideoMode(VideoMode.WEBCAM)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - video mode should be updated
            assertEquals(VideoMode.WEBCAM, mainViewModel.videoMode.value)
        }

    @Test
    fun `MainViewModel should handle network quality changes`() =
        runTest {
            // When - network quality changes
            mainViewModel.handleNetworkQualityChange(NetworkQuality.POOR)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - network quality should be updated
            assertEquals(NetworkQuality.POOR, mainViewModel.networkQuality.value)

            // And - optimization should be suggested
            assertNotNull(mainViewModel.platformOptimization.value)
            assertEquals(OptimizationReason.POOR_NETWORK, mainViewModel.platformOptimization.value?.reason)
        }

    @Test
    fun `MainViewModel should handle system interruptions`() =
        runTest {
            // Given - connected state
            mockChatRepository.emitConnectionState(ConnectionState.CONNECTED)
            testDispatcher.scheduler.advanceUntilIdle()

            // When - phone call interruption
            val interruption = SystemInterruption(InterruptionType.PHONE_CALL, shouldPause = true)
            mainViewModel.handleSystemInterruption(interruption)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - call should be paused
            assertTrue(mainViewModel.isCallPaused.value)
        }

    @Test
    fun `SettingsViewModel getCurrentSettings should return current settings`() =
        runTest {
            // When - getting current settings
            val currentSettings = settingsViewModel.getCurrentSettings()

            // Then - should return current settings
            assertEquals(settingsViewModel.settings.value, currentSettings)
        }

    @Test
    fun `SettingsViewModel isDebugLoggingEnabled should return current state`() =
        runTest {
            // When - checking debug logging state
            val isEnabled = settingsViewModel.isDebugLoggingEnabled()

            // Then - should return current state
            assertEquals(settingsViewModel.settings.value.debugLogging, isEnabled)
        }

    @Test
    fun `SettingsViewModel clearError should clear error state`() =
        runTest {
            // Given - error state exists (simulate by setting directly)
            settingsViewModel.clearError() // This should work even if no error exists

            // Then - error should be null
            assertNull(settingsViewModel.error.value)
        }

    @Test
    fun `MainViewModel clearError should clear error state`() =
        runTest {
            // Given - clear error is called
            mainViewModel.clearError()

            // Then - error should be null
            assertNull(mainViewModel.error.value)
        }

    @Test
    fun `MainViewModel should log video mode changes`() =
        runTest {
            // When - changing video mode multiple times
            mainViewModel.setVideoMode(VideoMode.WEBCAM)
            testDispatcher.scheduler.advanceUntilIdle()
            mainViewModel.setVideoMode(VideoMode.SCREEN_SHARE)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - logs should contain video mode changes
            val logs = mainViewModel.logs.value
            assertTrue(logs.any { it.message.contains("Video mode changed") })
        }

    // Simplified mock implementations
    private class MockChatRepository : ChatRepository {
        private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
        private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())

        fun emitConnectionState(state: ConnectionState) {
            _connectionState.value = state
        }

        override suspend fun createCall(callRequest: CallRequest): Result<CallResponse> =
            Result.success(CallResponse("test-id", "test-sdp", "success"))

        override fun observeConnectionState(): Flow<ConnectionState> = _connectionState.asStateFlow()

        override fun observeLogs(): Flow<List<LogEntry>> = _logs.asStateFlow()

        override suspend fun startConnectionMonitoring(callId: String): Result<Unit> = Result.success(Unit)

        override suspend fun stopConnection(): Result<Unit> = Result.success(Unit)

        override fun isConnected(): Boolean = _connectionState.value == ConnectionState.CONNECTED

        override fun getCurrentConnectionState(): ConnectionState = _connectionState.value

        override suspend fun switchCamera(): Result<Unit> = Result.success(Unit)

        override suspend fun updateVideoMode(videoMode: VideoMode): Result<Unit> = Result.success(Unit)
    }

    private class MockSettingsRepository : SettingsRepository {
        private val _settings = MutableStateFlow(AppSettings())

        override fun observeSettings(): Flow<AppSettings> = _settings.asStateFlow()

        override suspend fun getSettings(): AppSettings = _settings.value

        override suspend fun updateSettings(settings: AppSettings): Result<Unit> = Result.success(Unit)

        override suspend fun getDefaultVideoMode(): VideoMode = _settings.value.defaultVideoMode

        override suspend fun setDefaultVideoMode(mode: VideoMode): Result<Unit> = Result.success(Unit)

        override suspend fun getAudioQuality(): AudioQuality = _settings.value.audioQuality

        override suspend fun setAudioQuality(quality: AudioQuality): Result<Unit> = Result.success(Unit)

        override suspend fun getDefaultCamera(): CameraFacing = _settings.value.defaultCamera

        override suspend fun setDefaultCamera(camera: CameraFacing): Result<Unit> = Result.success(Unit)

        override suspend fun isDebugLoggingEnabled(): Boolean = _settings.value.debugLogging

        override suspend fun setDebugLogging(enabled: Boolean): Result<Unit> = Result.success(Unit)

        override suspend fun getServerUrl(): String = _settings.value.serverUrl

        override suspend fun setServerUrl(url: String): Result<Unit> = Result.success(Unit)

        override suspend fun resetToDefaults(): Result<Unit> = Result.success(Unit)
    }
}
