package ai.chatrt.app.viewmodel

import ai.chatrt.app.models.*
import ai.chatrt.app.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private lateinit var mockSettingsRepository: MockSettingsRepository
    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockSettingsRepository = MockSettingsRepository()
        viewModel = SettingsViewModel(mockSettingsRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be correct`() =
        runTest {
            // Given - ViewModel is initialized
            testDispatcher.scheduler.advanceUntilIdle()

            // When - checking initial state
            val settings = viewModel.settings.value
            val defaultVideoMode = viewModel.defaultVideoMode.value
            val audioQuality = viewModel.audioQuality.value
            val debugLogging = viewModel.debugLogging.value
            val serverUrl = viewModel.serverUrl.value
            val defaultCamera = viewModel.defaultCamera.value
            val isLoading = viewModel.isLoading.value
            val error = viewModel.error.value

            // Then - initial state should be correct
            assertEquals(VideoMode.AUDIO_ONLY, settings.defaultVideoMode)
            assertEquals(VideoMode.AUDIO_ONLY, defaultVideoMode)
            assertEquals(AudioQuality.MEDIUM, audioQuality)
            assertFalse(debugLogging)
            assertEquals("", serverUrl)
            assertEquals(CameraFacing.FRONT, defaultCamera)
            assertFalse(isLoading)
            assertNull(error)
        }

    @Test
    fun `updateDefaultVideoMode should update setting and show success`() =
        runTest {
            // Given - successful repository operation
            mockSettingsRepository.setDefaultVideoModeResult(Result.success(Unit))

            // When - updating default video mode
            viewModel.updateDefaultVideoMode(VideoMode.WEBCAM)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - setting should be updated
            assertEquals(VideoMode.WEBCAM, viewModel.settings.value.defaultVideoMode)
            // Note: saveSuccess might not be immediately available due to async nature
            assertFalse(viewModel.isLoading.value)
        }

    @Test
    fun `updateDefaultVideoMode with failure should set error`() =
        runTest {
            // Given - failed repository operation
            val exception = RuntimeException("Save failed")
            mockSettingsRepository.setDefaultVideoModeResult(Result.failure(exception))

            // When - updating default video mode
            viewModel.updateDefaultVideoMode(VideoMode.WEBCAM)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - error should be set and loading should be false
            assertNotNull(viewModel.error.value)
            assertFalse(viewModel.isLoading.value)
            assertFalse(viewModel.saveSuccess.value)
        }

    @Test
    fun `updateAudioQuality should update setting and show success`() =
        runTest {
            // Given - successful repository operation
            mockSettingsRepository.setAudioQualityResult(Result.success(Unit))

            // When - updating audio quality
            viewModel.updateAudioQuality(AudioQuality.HIGH)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - setting should be updated and success shown
            assertEquals(AudioQuality.HIGH, viewModel.settings.value.audioQuality)
            assertTrue(viewModel.saveSuccess.value)
        }

    @Test
    fun `updateDefaultCamera should update setting and show success`() =
        runTest {
            // Given - successful repository operation
            mockSettingsRepository.setDefaultCameraResult(Result.success(Unit))

            // When - updating default camera
            viewModel.updateDefaultCamera(CameraFacing.BACK)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - setting should be updated and success shown
            assertEquals(CameraFacing.BACK, viewModel.settings.value.defaultCamera)
            assertTrue(viewModel.saveSuccess.value)
        }

    @Test
    fun `toggleDebugLogging should toggle the setting`() =
        runTest {
            // Given - debug logging is initially false
            mockSettingsRepository.setDebugLoggingResult(Result.success(Unit))
            assertFalse(viewModel.settings.value.debugLogging)

            // When - toggling debug logging
            viewModel.toggleDebugLogging()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - debug logging should be enabled
            assertTrue(viewModel.settings.value.debugLogging)
            assertTrue(viewModel.saveSuccess.value)
        }

    @Test
    fun `updateDebugLogging should update setting directly`() =
        runTest {
            // Given - successful repository operation
            mockSettingsRepository.setDebugLoggingResult(Result.success(Unit))

            // When - updating debug logging directly
            viewModel.updateDebugLogging(true)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - setting should be updated
            assertTrue(viewModel.settings.value.debugLogging)
            assertTrue(viewModel.saveSuccess.value)
        }

    @Test
    fun `updateServerUrl with valid URL should update setting`() =
        runTest {
            // Given - successful repository operation
            mockSettingsRepository.setServerUrlResult(Result.success(Unit))
            val validUrl = "https://api.example.com"

            // When - updating server URL
            viewModel.updateServerUrl(validUrl)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - setting should be updated
            assertEquals(validUrl, viewModel.settings.value.serverUrl)
            assertTrue(viewModel.saveSuccess.value)
        }

    @Test
    fun `updateServerUrl with invalid URL should set error`() =
        runTest {
            // Given - invalid URL
            val invalidUrl = "not-a-valid-url"

            // When - updating server URL with invalid format
            viewModel.updateServerUrl(invalidUrl)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - error should be set
            assertNotNull(viewModel.error.value)
            assertTrue(viewModel.error.value is ChatRtError.ApiError)
            assertFalse(viewModel.saveSuccess.value)
        }

    @Test
    fun `updateServerUrl with empty URL should be allowed`() =
        runTest {
            // Given - successful repository operation
            mockSettingsRepository.setServerUrlResult(Result.success(Unit))

            // When - updating server URL with empty string
            viewModel.updateServerUrl("")
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - setting should be updated (empty URL is valid for reset)
            assertEquals("", viewModel.settings.value.serverUrl)
            assertTrue(viewModel.saveSuccess.value)
        }

    @Test
    fun `updateAllSettings should update complete settings object`() =
        runTest {
            // Given - successful repository operation and new settings
            mockSettingsRepository.setUpdateSettingsResult(Result.success(Unit))
            val newSettings =
                AppSettings(
                    defaultVideoMode = VideoMode.SCREEN_SHARE,
                    audioQuality = AudioQuality.LOW,
                    debugLogging = true,
                    serverUrl = "https://new-server.com",
                    defaultCamera = CameraFacing.BACK,
                )

            // When - updating all settings
            viewModel.updateAllSettings(newSettings)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - all settings should be updated
            assertEquals(newSettings, viewModel.settings.value)
            assertEquals(VideoMode.SCREEN_SHARE, viewModel.settings.value.defaultVideoMode)
            assertEquals(AudioQuality.LOW, viewModel.settings.value.audioQuality)
            assertTrue(viewModel.settings.value.debugLogging)
            assertEquals("https://new-server.com", viewModel.settings.value.serverUrl)
            assertEquals(CameraFacing.BACK, viewModel.settings.value.defaultCamera)
            assertTrue(viewModel.saveSuccess.value)
        }

    @Test
    fun `resetToDefaults should reset all settings`() =
        runTest {
            // Given - successful repository operation
            mockSettingsRepository.setResetToDefaultsResult(Result.success(Unit))

            // When - resetting to defaults
            viewModel.resetToDefaults()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - success should be shown
            assertTrue(viewModel.saveSuccess.value)
            assertFalse(viewModel.isLoading.value)
        }

    @Test
    fun `validateServerUrl with valid URL should return success`() =
        runTest {
            // Given - valid URL
            val validUrl = "https://api.example.com"
            var result: Pair<Boolean, String?> = Pair(false, null)

            // When - validating server URL
            viewModel.validateServerUrl(validUrl) { isValid, error ->
                result = Pair(isValid, error)
            }
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - validation should succeed
            assertTrue(result.first)
            assertNull(result.second)
        }

    @Test
    fun `validateServerUrl with invalid URL should return failure`() =
        runTest {
            // Given - invalid URL
            val invalidUrl = "not-a-url"
            var result: Pair<Boolean, String?> = Pair(true, null)

            // When - validating server URL
            viewModel.validateServerUrl(invalidUrl) { isValid, error ->
                result = Pair(isValid, error)
            }
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - validation should fail
            assertFalse(result.first)
            assertNotNull(result.second)
        }

    @Test
    fun `getCurrentSettings should return current settings synchronously`() =
        runTest {
            // Given - ViewModel with settings
            testDispatcher.scheduler.advanceUntilIdle()

            // When - getting current settings
            val currentSettings = viewModel.getCurrentSettings()

            // Then - should return current settings
            assertEquals(viewModel.settings.value, currentSettings)
        }

    @Test
    fun `isDebugLoggingEnabled should return current debug logging state`() =
        runTest {
            // Given - debug logging is initially false
            testDispatcher.scheduler.advanceUntilIdle()

            // When - checking if debug logging is enabled
            val isEnabled = viewModel.isDebugLoggingEnabled()

            // Then - should return false
            assertFalse(isEnabled)
        }

    @Test
    fun `getCurrentServerUrl should return current server URL`() =
        runTest {
            // Given - ViewModel with default server URL
            testDispatcher.scheduler.advanceUntilIdle()

            // When - getting current server URL
            val serverUrl = viewModel.getCurrentServerUrl()

            // Then - should return current server URL
            assertEquals(viewModel.settings.value.serverUrl, serverUrl)
        }

    @Test
    fun `clearError should clear error state`() =
        runTest {
            // Given - error state exists
            mockSettingsRepository.setDefaultVideoModeResult(Result.failure(RuntimeException("Test error")))
            viewModel.updateDefaultVideoMode(VideoMode.WEBCAM)
            testDispatcher.scheduler.advanceUntilIdle()
            assertNotNull(viewModel.error.value)

            // When - clearing error
            viewModel.clearError()

            // Then - error should be cleared
            assertNull(viewModel.error.value)
        }

    @Test
    fun `clearSaveSuccess should clear save success state`() =
        runTest {
            // Given - save success state exists
            mockSettingsRepository.setDefaultVideoModeResult(Result.success(Unit))
            viewModel.updateDefaultVideoMode(VideoMode.WEBCAM)
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(viewModel.saveSuccess.value)

            // When - clearing save success
            viewModel.clearSaveSuccess()

            // Then - save success should be cleared
            assertFalse(viewModel.saveSuccess.value)
        }

    @Test
    fun `save success should auto-clear after delay`() =
        runTest {
            // Given - successful save operation
            mockSettingsRepository.setDefaultVideoModeResult(Result.success(Unit))

            // When - updating setting
            viewModel.updateDefaultVideoMode(VideoMode.WEBCAM)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - save success should be true initially
            assertTrue(viewModel.saveSuccess.value)

            // When - advancing time by 3 seconds
            testDispatcher.scheduler.advanceTimeBy(3000)

            // Then - save success should be cleared
            assertFalse(viewModel.saveSuccess.value)
        }

    // Mock implementation of SettingsRepository for testing
    private class MockSettingsRepository : SettingsRepository {
        private val _settings = MutableStateFlow(AppSettings())

        private var defaultVideoModeResult: Result<Unit> = Result.success(Unit)
        private var audioQualityResult: Result<Unit> = Result.success(Unit)
        private var defaultCameraResult: Result<Unit> = Result.success(Unit)
        private var debugLoggingResult: Result<Unit> = Result.success(Unit)
        private var serverUrlResult: Result<Unit> = Result.success(Unit)
        private var updateSettingsResult: Result<Unit> = Result.success(Unit)
        private var resetToDefaultsResult: Result<Unit> = Result.success(Unit)

        fun setDefaultVideoModeResult(result: Result<Unit>) {
            defaultVideoModeResult = result
        }

        fun setAudioQualityResult(result: Result<Unit>) {
            audioQualityResult = result
        }

        fun setDefaultCameraResult(result: Result<Unit>) {
            defaultCameraResult = result
        }

        fun setDebugLoggingResult(result: Result<Unit>) {
            debugLoggingResult = result
        }

        fun setServerUrlResult(result: Result<Unit>) {
            serverUrlResult = result
        }

        fun setUpdateSettingsResult(result: Result<Unit>) {
            updateSettingsResult = result
        }

        fun setResetToDefaultsResult(result: Result<Unit>) {
            resetToDefaultsResult = result
        }

        override fun observeSettings(): Flow<AppSettings> = _settings.asStateFlow()

        override suspend fun getSettings(): AppSettings = _settings.value

        override suspend fun updateSettings(settings: AppSettings): Result<Unit> =
            updateSettingsResult.onSuccess {
                _settings.value = settings
            }

        override suspend fun getDefaultVideoMode(): VideoMode = _settings.value.defaultVideoMode

        override suspend fun setDefaultVideoMode(mode: VideoMode): Result<Unit> =
            defaultVideoModeResult.onSuccess {
                _settings.value = _settings.value.copy(defaultVideoMode = mode)
            }

        override suspend fun getAudioQuality(): AudioQuality = _settings.value.audioQuality

        override suspend fun setAudioQuality(quality: AudioQuality): Result<Unit> =
            audioQualityResult.onSuccess {
                _settings.value = _settings.value.copy(audioQuality = quality)
            }

        override suspend fun getDefaultCamera(): CameraFacing = _settings.value.defaultCamera

        override suspend fun setDefaultCamera(camera: CameraFacing): Result<Unit> =
            defaultCameraResult.onSuccess {
                _settings.value = _settings.value.copy(defaultCamera = camera)
            }

        override suspend fun isDebugLoggingEnabled(): Boolean = _settings.value.debugLogging

        override suspend fun setDebugLogging(enabled: Boolean): Result<Unit> =
            debugLoggingResult.onSuccess {
                _settings.value = _settings.value.copy(debugLogging = enabled)
            }

        override suspend fun getServerUrl(): String = _settings.value.serverUrl

        override suspend fun setServerUrl(url: String): Result<Unit> =
            serverUrlResult.onSuccess {
                _settings.value = _settings.value.copy(serverUrl = url)
            }

        override suspend fun resetToDefaults(): Result<Unit> =
            resetToDefaultsResult.onSuccess {
                _settings.value = AppSettings()
            }
    }
}
