package ai.chatrt.app.platform

import ai.chatrt.app.models.PermissionType
import kotlinx.coroutines.flow.Flow

/**
 * Permission manager interface for handling platform-specific permissions
 * Requirements: 1.2, 2.6, 3.6 - Permission handling with fallback options
 */
interface PermissionManager {
    /**
     * Check if a specific permission is granted
     */
    suspend fun checkPermission(permission: PermissionType): Boolean

    /**
     * Request a specific permission
     */
    suspend fun requestPermission(permission: PermissionType): Boolean

    /**
     * Request multiple permissions at once
     */
    suspend fun requestMultiplePermissions(permissions: List<PermissionType>): Map<PermissionType, Boolean>

    /**
     * Check if should show permission rationale
     */
    fun shouldShowRationale(permission: PermissionType): Boolean

    /**
     * Open app settings for manual permission grant
     */
    fun openAppSettings()

    /**
     * Observe permission changes
     */
    fun observePermissionChanges(): Flow<PermissionChange>

    // Convenience methods for specific permissions

    /**
     * Check if microphone permission is granted
     */
    suspend fun checkMicrophonePermission(): Boolean = checkPermission(PermissionType.MICROPHONE)

    /**
     * Check if camera permission is granted
     */
    suspend fun checkCameraPermission(): Boolean = checkPermission(PermissionType.CAMERA)

    /**
     * Check if screen capture permission is granted
     */
    suspend fun checkScreenCapturePermission(): Boolean = checkPermission(PermissionType.SCREEN_CAPTURE)

    /**
     * Check if notification permission is granted
     */
    suspend fun checkNotificationPermission(): Boolean = checkPermission(PermissionType.NOTIFICATION)

    /**
     * Request microphone permission
     */
    suspend fun requestMicrophonePermission(): Boolean = requestPermission(PermissionType.MICROPHONE)

    /**
     * Request camera permission
     */
    suspend fun requestCameraPermission(): Boolean = requestPermission(PermissionType.CAMERA)

    /**
     * Request screen capture permission
     */
    suspend fun requestScreenCapturePermission(): Boolean = requestPermission(PermissionType.SCREEN_CAPTURE)

    /**
     * Request notification permission
     */
    suspend fun requestNotificationPermission(): Boolean = requestPermission(PermissionType.NOTIFICATION)
}

/**
 * Permission change event
 */
data class PermissionChange(
    val permission: PermissionType,
    val granted: Boolean,
)

/**
 * Expected permission manager factory function
 */
expect fun createPermissionManager(): PermissionManager
