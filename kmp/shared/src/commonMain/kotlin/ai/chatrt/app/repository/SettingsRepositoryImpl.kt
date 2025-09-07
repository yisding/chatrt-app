package ai.chatrt.app.repository

import ai.chatrt.app.models.AppSettings
import ai.chatrt.app.models.AudioQuality
import ai.chatrt.app.models.CameraFacing
import ai.chatrt.app.models.VideoMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementation of SettingsRepository
 * This is a base implementation that uses in-memory storage
 * Platform-specific implementations should extend this to use persistent storage
 */
open class SettingsRepositoryImpl : SettingsRepository {
    
    private val _settings = MutableStateFlow(AppSettings())
    private val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    
    override fun observeSettings(): Flow<AppSettings> {
        return settings
    }
    
    override suspend fun getSettings(): AppSettings {
        return _settings.value
    }
    
    override suspend fun updateSettings(settings: AppSettings): Result<Unit> {
        return try {
            _settings.value = settings
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getDefaultVideoMode(): VideoMode {
        return _settings.value.defaultVideoMode
    }
    
    override suspend fun setDefaultVideoMode(mode: VideoMode): Result<Unit> {
        return try {
            _settings.value = _settings.value.copy(defaultVideoMode = mode)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getAudioQuality(): AudioQuality {
        return _settings.value.audioQuality
    }
    
    override suspend fun setAudioQuality(quality: AudioQuality): Result<Unit> {
        return try {
            _settings.value = _settings.value.copy(audioQuality = quality)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getDefaultCamera(): CameraFacing {
        return _settings.value.defaultCamera
    }
    
    override suspend fun setDefaultCamera(camera: CameraFacing): Result<Unit> {
        return try {
            _settings.value = _settings.value.copy(defaultCamera = camera)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun isDebugLoggingEnabled(): Boolean {
        return _settings.value.debugLogging
    }
    
    override suspend fun setDebugLogging(enabled: Boolean): Result<Unit> {
        return try {
            _settings.value = _settings.value.copy(debugLogging = enabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getServerUrl(): String {
        return _settings.value.serverUrl
    }
    
    override suspend fun setServerUrl(url: String): Result<Unit> {
        return try {
            _settings.value = _settings.value.copy(serverUrl = url)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun resetToDefaults(): Result<Unit> {
        return try {
            _settings.value = AppSettings()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}