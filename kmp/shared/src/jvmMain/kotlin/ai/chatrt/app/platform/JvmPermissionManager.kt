package ai.chatrt.app.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * JVM/Desktop implementation of PermissionManager
 */
class JvmPermissionManager : PermissionManager {
    
    private val _permissionChanges = MutableSharedFlow<PermissionChange>()
    
    override suspend fun checkMicrophonePermission(): Boolean {
        // Desktop platforms typically don't have runtime permissions like mobile
        // This would check system-level microphone access
        return true
    }
    
    override suspend fun checkCameraPermission(): Boolean {
        // Desktop platforms typically don't have runtime permissions like mobile
        return true
    }
    
    override suspend fun checkScreenCapturePermission(): Boolean {
        // Desktop screen capture might require system permissions on some platforms (macOS)
        return true
    }
    
    override suspend fun checkNotificationPermission(): Boolean {
        // Desktop notifications typically don't require explicit permission
        return true
    }
    
    override suspend fun requestMicrophonePermission(): PermissionStatus {
        // Desktop doesn't typically require runtime permission requests
        return PermissionStatus.GRANTED
    }
    
    override suspend fun requestCameraPermission(): PermissionStatus {
        // Desktop doesn't typically require runtime permission requests
        return PermissionStatus.GRANTED
    }
    
    override suspend fun requestScreenCapturePermission(): ScreenCapturePermissionResult {
        // Desktop screen capture permission handling
        // On macOS, this might require system preferences changes
        return ScreenCapturePermissionResult(
            status = PermissionStatus.GRANTED,
            data = null
        )
    }
    
    override suspend fun requestNotificationPermission(): PermissionStatus {
        // Desktop notifications typically don't require explicit permission
        return PermissionStatus.GRANTED
    }
    
    override suspend fun requestPermissions(permissions: List<Permission>): Map<Permission, PermissionStatus> {
        // Request multiple permissions - typically all granted on desktop
        return permissions.associateWith { PermissionStatus.GRANTED }
    }
    
    override suspend fun shouldShowRationale(permission: Permission): Boolean {
        // Desktop doesn't typically show permission rationales
        return false
    }
    
    override suspend fun openAppSettings() {
        // Open system settings or app preferences
        // This would be platform-specific (Windows Settings, macOS System Preferences, etc.)
    }
    
    override fun observePermissionChanges(): Flow<PermissionChange> = _permissionChanges.asSharedFlow()
}

/**
 * Factory function for creating JVM permission manager
 */
actual fun createPermissionManager(): PermissionManager = JvmPermissionManager()