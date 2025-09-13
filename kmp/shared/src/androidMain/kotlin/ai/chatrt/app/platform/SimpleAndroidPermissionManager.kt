package ai.chatrt.app.platform

import ai.chatrt.app.models.PermissionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Minimal PermissionManager implementation for Android to satisfy build.
 */
class SimpleAndroidPermissionManager : PermissionManager {
    private val _changes = MutableSharedFlow<PermissionChange>()

    override suspend fun checkPermission(permission: PermissionType): Boolean = true

    override suspend fun requestPermission(permission: PermissionType): Boolean = true

    override suspend fun requestMultiplePermissions(permissions: List<PermissionType>): Map<PermissionType, Boolean> =
        permissions.associateWith { true }

    override fun shouldShowRationale(permission: PermissionType): Boolean = false

    override fun openAppSettings() { /* no-op */ }

    override fun observePermissionChanges(): Flow<PermissionChange> = _changes.asSharedFlow()
}

actual fun createPermissionManager(): PermissionManager = SimpleAndroidPermissionManager()
