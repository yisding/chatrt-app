package ai.chatrt.app.repository

import ai.chatrt.app.models.AppSettings
import ai.chatrt.app.models.AudioQuality
import ai.chatrt.app.models.CameraFacing
import ai.chatrt.app.models.VideoMode
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for app configuration and settings
 * Handles persistent storage of user preferences
 */
interface SettingsRepository {
    /**
     * Observes changes to app settings
     * @return Flow of app settings updates
     */
    fun observeSettings(): Flow<AppSettings>

    /**
     * Gets the current app settings
     * @return Current app settings
     */
    suspend fun getSettings(): AppSettings

    /**
     * Updates the complete app settings
     * @param settings New app settings to save
     */
    suspend fun updateSettings(settings: AppSettings): Result<Unit>

    /**
     * Gets the default video mode
     * @return Current default video mode
     */
    suspend fun getDefaultVideoMode(): VideoMode

    /**
     * Sets the default video mode
     * @param mode New default video mode
     */
    suspend fun setDefaultVideoMode(mode: VideoMode): Result<Unit>

    /**
     * Gets the audio quality setting
     * @return Current audio quality setting
     */
    suspend fun getAudioQuality(): AudioQuality

    /**
     * Sets the audio quality setting
     * @param quality New audio quality setting
     */
    suspend fun setAudioQuality(quality: AudioQuality): Result<Unit>

    /**
     * Gets the default camera facing direction
     * @return Current default camera facing
     */
    suspend fun getDefaultCamera(): CameraFacing

    /**
     * Sets the default camera facing direction
     * @param camera New default camera facing
     */
    suspend fun setDefaultCamera(camera: CameraFacing): Result<Unit>

    /**
     * Gets the debug logging setting
     * @return true if debug logging is enabled
     */
    suspend fun isDebugLoggingEnabled(): Boolean

    /**
     * Sets the debug logging setting
     * @param enabled Whether to enable debug logging
     */
    suspend fun setDebugLogging(enabled: Boolean): Result<Unit>

    /**
     * Gets the server URL setting
     * @return Current server URL
     */
    suspend fun getServerUrl(): String

    /**
     * Sets the server URL setting
     * @param url New server URL
     */
    suspend fun setServerUrl(url: String): Result<Unit>

    /**
     * Resets all settings to default values
     */
    suspend fun resetToDefaults(): Result<Unit>
}
