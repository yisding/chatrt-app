package ai.chatrt.app.platform

import ai.chatrt.app.models.InterruptionType
import ai.chatrt.app.models.SystemInterruption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * JVM/Desktop implementation of LifecycleManager
 */
class JvmLifecycleManager : LifecycleManager {
    private val _lifecycleState = MutableStateFlow(AppLifecycleState.CREATED)
    private val _systemInterruptions = MutableSharedFlow<SystemInterruption>()

    override suspend fun initialize() {
        // Initialize lifecycle monitoring for desktop
        _lifecycleState.value = AppLifecycleState.STARTED
    }

    override suspend fun startMonitoring() {
        // Start monitoring lifecycle events on desktop
        registerSystemInterruptionCallbacks()
    }

    override suspend fun stopMonitoring() {
        // Stop monitoring lifecycle events
        unregisterSystemInterruptionCallbacks()
    }

    override suspend fun handleAppBackground() {
        // Desktop apps don't typically go to "background" like mobile apps
        // This might represent window minimization
        _lifecycleState.value = AppLifecycleState.PAUSED
    }

    override suspend fun handleAppForeground() {
        // Desktop app brought to foreground (window restored/focused)
        _lifecycleState.value = AppLifecycleState.RESUMED
    }

    override suspend fun handleAppPause() {
        // Desktop app paused (window minimized or lost focus)
        _lifecycleState.value = AppLifecycleState.PAUSED
    }

    override suspend fun handleAppResume() {
        // Desktop app resumed (window restored or gained focus)
        _lifecycleState.value = AppLifecycleState.RESUMED
    }

    override suspend fun handleAppDestroy() {
        // Desktop app being closed
        _lifecycleState.value = AppLifecycleState.DESTROYED
        cleanup()
    }

    override suspend fun handleOrientationChange(orientation: DeviceOrientation) {
        // Desktop doesn't typically have orientation changes
        // This might represent window resize or display configuration changes
    }

    override fun observeLifecycleState(): Flow<AppLifecycleState> = _lifecycleState.asStateFlow()

    override fun observeSystemInterruptions(): Flow<SystemInterruption> = _systemInterruptions.asSharedFlow()

    override suspend fun getCurrentLifecycleState(): AppLifecycleState = _lifecycleState.value

    override suspend fun registerSystemInterruptionCallbacks() {
        // Register for system interruption callbacks on desktop
        // This might include:
        // - System sleep/wake events
        // - Network disconnection
        // - Other system calls (Skype, Teams, etc.)

        // Add shutdown hook for graceful cleanup
        Runtime.getRuntime().addShutdownHook(
            Thread {
                _lifecycleState.value = AppLifecycleState.DESTROYED
            },
        )
    }

    override suspend fun unregisterSystemInterruptionCallbacks() {
        // Unregister system interruption callbacks
    }

    override suspend fun cleanup() {
        stopMonitoring()
    }

    /**
     * Desktop-specific method to handle system sleep
     */
    suspend fun handleSystemSleep() {
        val interruption =
            SystemInterruption(
                type = InterruptionType.LOW_POWER_MODE,
                shouldPause = true,
                canResume = true,
            )
        _systemInterruptions.emit(interruption)
    }

    /**
     * Desktop-specific method to handle system wake
     */
    suspend fun handleSystemWake() {
        // System woke up, can resume operations
    }

    /**
     * Desktop-specific method to handle other system calls
     */
    suspend fun handleSystemCall() {
        val interruption =
            SystemInterruption(
                type = InterruptionType.SYSTEM_CALL,
                shouldPause = true,
                canResume = true,
            )
        _systemInterruptions.emit(interruption)
    }
}

/**
 * Factory function for creating JVM lifecycle manager
 */
actual fun createLifecycleManager(): LifecycleManager = JvmLifecycleManager()
