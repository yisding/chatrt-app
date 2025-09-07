package ai.chatrt.app.platform

import ai.chatrt.app.models.SystemInterruption
import kotlinx.coroutines.flow.Flow

/**
 * Lifecycle manager interface for handling app lifecycle and system events
 */
interface LifecycleManager {
    /**
     * Initialize lifecycle monitoring
     */
    suspend fun initialize()
    
    /**
     * Start lifecycle monitoring
     */
    suspend fun startMonitoring()
    
    /**
     * Stop lifecycle monitoring
     */
    suspend fun stopMonitoring()
    
    /**
     * Handle app going to background
     */
    suspend fun handleAppBackground()
    
    /**
     * Handle app coming to foreground
     */
    suspend fun handleAppForeground()
    
    /**
     * Handle app being paused
     */
    suspend fun handleAppPause()
    
    /**
     * Handle app being resumed
     */
    suspend fun handleAppResume()
    
    /**
     * Handle app being destroyed/terminated
     */
    suspend fun handleAppDestroy()
    
    /**
     * Handle device orientation change
     */
    suspend fun handleOrientationChange(orientation: DeviceOrientation)
    
    /**
     * Observe app lifecycle state changes
     */
    fun observeLifecycleState(): Flow<AppLifecycleState>
    
    /**
     * Observe system interruptions
     */
    fun observeSystemInterruptions(): Flow<SystemInterruption>
    
    /**
     * Get current app lifecycle state
     */
    suspend fun getCurrentLifecycleState(): AppLifecycleState
    
    /**
     * Register for system interruption callbacks (phone calls, etc.)
     */
    suspend fun registerSystemInterruptionCallbacks()
    
    /**
     * Unregister system interruption callbacks
     */
    suspend fun unregisterSystemInterruptionCallbacks()
    
    /**
     * Cleanup lifecycle monitoring resources
     */
    suspend fun cleanup()
}

/**
 * App lifecycle states
 */
enum class AppLifecycleState {
    CREATED,
    STARTED,
    RESUMED,
    PAUSED,
    STOPPED,
    DESTROYED
}

/**
 * Device orientation
 */
enum class DeviceOrientation {
    PORTRAIT,
    LANDSCAPE,
    PORTRAIT_REVERSE,
    LANDSCAPE_REVERSE,
    UNKNOWN
}

/**
 * Expected lifecycle manager factory function
 */
expect fun createLifecycleManager(): LifecycleManager