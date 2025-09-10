package ai.chatrt.app.platform

import ai.chatrt.app.models.InterruptionType
import ai.chatrt.app.models.SystemInterruption
import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android implementation of LifecycleManager
 */
class AndroidLifecycleManager(
    private val context: Context,
) : LifecycleManager {
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val _lifecycleState = MutableStateFlow(AppLifecycleState.CREATED)
    private val _systemInterruptions = MutableSharedFlow<SystemInterruption>()

    private val phoneStateListener =
        object : PhoneStateListener() {
            override fun onCallStateChanged(
                state: Int,
                phoneNumber: String?,
            ) {
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING,
                    TelephonyManager.CALL_STATE_OFFHOOK,
                    -> {
                        // Phone call started
                        val interruption =
                            SystemInterruption(
                                type = InterruptionType.PHONE_CALL,
                                shouldPause = true,
                                canResume = true,
                            )
                        _systemInterruptions.tryEmit(interruption)
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        // Phone call ended - could resume
                    }
                }
            }
        }

    override suspend fun initialize() {
        // Initialize lifecycle monitoring
    }

    override suspend fun startMonitoring() {
        // Start monitoring lifecycle events
        registerSystemInterruptionCallbacks()
    }

    override suspend fun stopMonitoring() {
        // Stop monitoring lifecycle events
        unregisterSystemInterruptionCallbacks()
    }

    override suspend fun handleAppBackground() {
        _lifecycleState.value = AppLifecycleState.STOPPED
    }

    override suspend fun handleAppForeground() {
        _lifecycleState.value = AppLifecycleState.RESUMED
    }

    override suspend fun handleAppPause() {
        _lifecycleState.value = AppLifecycleState.PAUSED
    }

    override suspend fun handleAppResume() {
        _lifecycleState.value = AppLifecycleState.RESUMED
    }

    override suspend fun handleAppDestroy() {
        _lifecycleState.value = AppLifecycleState.DESTROYED
        cleanup()
    }

    override suspend fun handleOrientationChange(orientation: DeviceOrientation) {
        // Handle orientation change
        // This would typically trigger UI layout updates
    }

    override fun observeLifecycleState(): Flow<AppLifecycleState> = _lifecycleState.asStateFlow()

    override fun observeSystemInterruptions(): Flow<SystemInterruption> = _systemInterruptions.asSharedFlow()

    override suspend fun getCurrentLifecycleState(): AppLifecycleState = _lifecycleState.value

    override suspend fun registerSystemInterruptionCallbacks() {
        // Register for phone state changes
        try {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (e: SecurityException) {
            // Handle permission not granted
        }
    }

    override suspend fun unregisterSystemInterruptionCallbacks() {
        // Unregister phone state listener
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
    }

    override suspend fun cleanup() {
        stopMonitoring()
    }
}

/**
 * Factory function for creating Android lifecycle manager
 */
actual fun createLifecycleManager(): LifecycleManager =
    throw IllegalStateException("Android LifecycleManager requires Context. Use AndroidLifecycleManager(context) directly.")
