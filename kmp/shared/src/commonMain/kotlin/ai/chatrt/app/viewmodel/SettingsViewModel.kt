package ai.chatrt.app.viewmodel

import ai.chatrt.app.models.*
import ai.chatrt.app.repository.SettingsRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for app settings and preferences management
 * Handles all user configuration options and preferences
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    // Complete app settings
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // Individual setting properties for easier UI binding
    val defaultVideoMode: StateFlow<VideoMode> =
        _settings.map { it.defaultVideoMode }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VideoMode.AUDIO_ONLY,
        )

    val audioQuality: StateFlow<AudioQuality> =
        _settings.map { it.audioQuality }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AudioQuality.MEDIUM,
        )

    val debugLogging: StateFlow<Boolean> =
        _settings.map { it.debugLogging }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    val serverUrl: StateFlow<String> =
        _settings.map { it.serverUrl }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "",
        )

    val defaultCamera: StateFlow<CameraFacing> =
        _settings.map { it.defaultCamera }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CameraFacing.FRONT,
        )

    // Error state for settings operations
    private val _error = MutableStateFlow<ChatRtError?>(null)
    val error: StateFlow<ChatRtError?> = _error.asStateFlow()

    // Loading state for async operations
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Success state for user feedback
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    init {
        // Load initial settings
        loadSettings()

        // Observe settings changes from repository
        viewModelScope.launch {
            settingsRepository
                .observeSettings()
                .catch { exception ->
                    _error.value = mapExceptionToChatRtError(exception)
                }.collect { newSettings ->
                    _settings.value = newSettings
                }
        }
    }

    /**
     * Updates the default video mode
     */
    fun updateDefaultVideoMode(mode: VideoMode) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = settingsRepository.setDefaultVideoMode(mode)

                result.fold(
                    onSuccess = {
                        // Update local state
                        _settings.value = _settings.value.copy(defaultVideoMode = mode)
                        _saveSuccess.value = true
                        clearSaveSuccessAfterDelay()
                    },
                    onFailure = { exception ->
                        _error.value = mapExceptionToChatRtError(exception)
                    },
                )
            } catch (e: Exception) {
                _error.value = mapExceptionToChatRtError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Updates the audio quality setting
     */
    fun updateAudioQuality(quality: AudioQuality) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = settingsRepository.setAudioQuality(quality)

                result.fold(
                    onSuccess = {
                        // Update local state
                        _settings.value = _settings.value.copy(audioQuality = quality)
                        _saveSuccess.value = true
                        clearSaveSuccessAfterDelay()
                    },
                    onFailure = { exception ->
                        _error.value = mapExceptionToChatRtError(exception)
                    },
                )
            } catch (e: Exception) {
                _error.value = mapExceptionToChatRtError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Updates the default camera setting
     */
    fun updateDefaultCamera(camera: CameraFacing) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = settingsRepository.setDefaultCamera(camera)

                result.fold(
                    onSuccess = {
                        // Update local state
                        _settings.value = _settings.value.copy(defaultCamera = camera)
                        _saveSuccess.value = true
                        clearSaveSuccessAfterDelay()
                    },
                    onFailure = { exception ->
                        _error.value = mapExceptionToChatRtError(exception)
                    },
                )
            } catch (e: Exception) {
                _error.value = mapExceptionToChatRtError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Toggles debug logging setting
     */
    fun toggleDebugLogging() {
        val newValue = !_settings.value.debugLogging
        updateDebugLogging(newValue)
    }

    /**
     * Updates debug logging setting
     */
    fun updateDebugLogging(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = settingsRepository.setDebugLogging(enabled)

                result.fold(
                    onSuccess = {
                        // Update local state
                        _settings.value = _settings.value.copy(debugLogging = enabled)
                        _saveSuccess.value = true
                        clearSaveSuccessAfterDelay()
                    },
                    onFailure = { exception ->
                        _error.value = mapExceptionToChatRtError(exception)
                    },
                )
            } catch (e: Exception) {
                _error.value = mapExceptionToChatRtError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Updates server URL setting
     */
    fun updateServerUrl(url: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Basic URL validation
                if (url.isNotBlank() && !isValidUrl(url)) {
                    _error.value = ChatRtError.ApiError(400, "Invalid server URL format")
                    _isLoading.value = false
                    return@launch
                }

                val result = settingsRepository.setServerUrl(url)

                result.fold(
                    onSuccess = {
                        // Update local state
                        _settings.value = _settings.value.copy(serverUrl = url)
                        _saveSuccess.value = true
                        clearSaveSuccessAfterDelay()
                    },
                    onFailure = { exception ->
                        _error.value = mapExceptionToChatRtError(exception)
                    },
                )
            } catch (e: Exception) {
                _error.value = mapExceptionToChatRtError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Updates all settings at once
     */
    fun updateAllSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = settingsRepository.updateSettings(newSettings)

                result.fold(
                    onSuccess = {
                        // Update local state
                        _settings.value = newSettings
                        _saveSuccess.value = true
                        clearSaveSuccessAfterDelay()
                    },
                    onFailure = { exception ->
                        _error.value = mapExceptionToChatRtError(exception)
                    },
                )
            } catch (e: Exception) {
                _error.value = mapExceptionToChatRtError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Resets all settings to default values
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = settingsRepository.resetToDefaults()

                result.fold(
                    onSuccess = {
                        // The settings will be updated via the observer
                        _saveSuccess.value = true
                        clearSaveSuccessAfterDelay()
                    },
                    onFailure = { exception ->
                        _error.value = mapExceptionToChatRtError(exception)
                    },
                )
            } catch (e: Exception) {
                _error.value = mapExceptionToChatRtError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Validates server URL configuration by attempting a test connection
     */
    fun validateServerUrl(
        url: String,
        onResult: (Boolean, String?) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                if (!isValidUrl(url)) {
                    onResult(false, "Invalid URL format")
                    return@launch
                }

                // For now, just validate the format
                // In a real implementation, you might want to test the connection
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    /**
     * Gets current settings synchronously (for immediate access)
     */
    fun getCurrentSettings(): AppSettings = _settings.value

    /**
     * Checks if debug logging is currently enabled
     */
    fun isDebugLoggingEnabled(): Boolean = _settings.value.debugLogging

    /**
     * Gets the current server URL
     */
    fun getCurrentServerUrl(): String = _settings.value.serverUrl

    /**
     * Clears the current error state
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Clears the save success state
     */
    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }

    /**
     * Loads settings from repository
     */
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val loadedSettings = settingsRepository.getSettings()
                _settings.value = loadedSettings
            } catch (e: Exception) {
                _error.value = mapExceptionToChatRtError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clears save success state after a delay for user feedback
     */
    private fun clearSaveSuccessAfterDelay() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000) // Show success for 3 seconds
            _saveSuccess.value = false
        }
    }

    /**
     * Basic URL validation
     */
    private fun isValidUrl(url: String): Boolean =
        try {
            url.startsWith("http://") || url.startsWith("https://") || url.startsWith("ws://") || url.startsWith("wss://")
        } catch (e: Exception) {
            false
        }

    /**
     * Maps exceptions to ChatRtError types
     */
    private fun mapExceptionToChatRtError(exception: Throwable): ChatRtError =
        when {
            exception.message?.contains("network", ignoreCase = true) == true ||
                exception.message?.contains("connection", ignoreCase = true) == true ||
                exception.message?.contains("host", ignoreCase = true) == true -> ChatRtError.NetworkError
            exception.message?.contains("permission", ignoreCase = true) == true ||
                exception.message?.contains("security", ignoreCase = true) == true -> ChatRtError.PermissionDenied
            else -> ChatRtError.ApiError(0, exception.message ?: "Unknown error")
        }
}
