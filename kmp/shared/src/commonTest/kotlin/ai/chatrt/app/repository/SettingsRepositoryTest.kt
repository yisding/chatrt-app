package ai.chatrt.app.repository

import ai.chatrt.app.models.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Tests for SettingsRepositoryImpl
 * Tests settings management, persistence, and state updates
 */
class SettingsRepositoryTest {
    private fun createRepository(): SettingsRepositoryImpl = SettingsRepositoryImpl()

    @Test
    fun testInitialSettings() =
        runTest {
            val repository = createRepository()
            // When
            val settings = repository.getSettings()

            // Then - Should have default values
            assertEquals(VideoMode.AUDIO_ONLY, settings.defaultVideoMode)
            assertEquals(AudioQuality.MEDIUM, settings.audioQuality)
            assertEquals(false, settings.debugLogging)
            assertEquals("", settings.serverUrl)
            assertEquals(CameraFacing.FRONT, settings.defaultCamera)
        }

    @Test
    fun testObserveSettings() =
        runTest {
            val repository = createRepository()
            // When
            val settingsFlow = repository.observeSettings()
            val initialSettings = settingsFlow.first()

            // Then
            assertEquals(AppSettings(), initialSettings)
        }

    @Test
    fun testUpdateSettings() =
        runTest {
            val repository = createRepository()
            // Given
            val newSettings =
                AppSettings(
                    defaultVideoMode = VideoMode.WEBCAM,
                    audioQuality = AudioQuality.HIGH,
                    debugLogging = true,
                    serverUrl = "https://api.chatrt.com",
                    defaultCamera = CameraFacing.BACK,
                )

            // When
            val result = repository.updateSettings(newSettings)

            // Then
            assertTrue(result.isSuccess)
            val updatedSettings = repository.getSettings()
            assertEquals(newSettings, updatedSettings)
        }

    @Test
    fun testSetDefaultVideoMode() =
        runTest {
            val repository = createRepository()
            // Given
            val newMode = VideoMode.SCREEN_SHARE

            // When
            val result = repository.setDefaultVideoMode(newMode)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(newMode, repository.getDefaultVideoMode())

            val settings = repository.getSettings()
            assertEquals(newMode, settings.defaultVideoMode)
        }

    @Test
    fun testSetAudioQuality() =
        runTest {
            val repository = createRepository()
            // Given
            val newQuality = AudioQuality.LOW

            // When
            val result = repository.setAudioQuality(newQuality)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(newQuality, repository.getAudioQuality())

            val settings = repository.getSettings()
            assertEquals(newQuality, settings.audioQuality)
        }

    @Test
    fun testSetDefaultCamera() =
        runTest {
            val repository = createRepository()
            // Given
            val newCamera = CameraFacing.BACK

            // When
            val result = repository.setDefaultCamera(newCamera)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(newCamera, repository.getDefaultCamera())

            val settings = repository.getSettings()
            assertEquals(newCamera, settings.defaultCamera)
        }

    @Test
    fun testSetDebugLogging() =
        runTest {
            val repository = createRepository()
            // Given
            val enableDebug = true

            // When
            val result = repository.setDebugLogging(enableDebug)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(enableDebug, repository.isDebugLoggingEnabled())

            val settings = repository.getSettings()
            assertEquals(enableDebug, settings.debugLogging)
        }

    @Test
    fun testSetServerUrl() =
        runTest {
            val repository = createRepository()
            // Given
            val newUrl = "https://new-api.chatrt.com"

            // When
            val result = repository.setServerUrl(newUrl)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(newUrl, repository.getServerUrl())

            val settings = repository.getSettings()
            assertEquals(newUrl, settings.serverUrl)
        }

    @Test
    fun testResetToDefaults() =
        runTest {
            val repository = createRepository()
            // Given - Change some settings first
            repository.setDefaultVideoMode(VideoMode.WEBCAM)
            repository.setAudioQuality(AudioQuality.HIGH)
            repository.setDebugLogging(true)
            repository.setServerUrl("https://test.com")
            repository.setDefaultCamera(CameraFacing.BACK)

            // Verify settings were changed
            val modifiedSettings = repository.getSettings()
            assertNotEquals(AppSettings(), modifiedSettings)

            // When
            val result = repository.resetToDefaults()

            // Then
            assertTrue(result.isSuccess)
            val resetSettings = repository.getSettings()
            assertEquals(AppSettings(), resetSettings)
        }

    @Test
    fun testSettingsFlowUpdates() =
        runTest {
            val repository = createRepository()
            // Given
            val settingsFlow = repository.observeSettings()
            val initialSettings = settingsFlow.first()

            // When
            repository.setDefaultVideoMode(VideoMode.WEBCAM)

            // Then - The flow should emit the updated settings
            val updatedSettings = repository.getSettings()
            assertEquals(VideoMode.WEBCAM, updatedSettings.defaultVideoMode)
            assertNotEquals(initialSettings, updatedSettings)
        }
}
