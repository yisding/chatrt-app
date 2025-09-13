package ai.chatrt.app.platform

import ai.chatrt.app.models.InterruptionType
import ai.chatrt.app.models.SystemInterruption
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.BatteryManager
import android.os.PowerManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Android implementation of LifecycleManager for comprehensive app lifecycle management
 * Requirements: 5.1, 5.2, 5.3, 5.6
 */
class AndroidLifecycleManager(
    private val context: Context,
) : LifecycleManager,
    DefaultLifecycleObserver {
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    private val _lifecycleState = MutableStateFlow(AppLifecycleState.CREATED)
    private val _systemInterruptions = MutableSharedFlow<SystemInterruption>()

    private var systemReceiver: BroadcastReceiver? = null
    private var isMonitoring = false
    private var currentOrientation = DeviceOrientation.UNKNOWN

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
                        // Phone call started - pause ChatRT session
                        val interruption =
                            SystemInterruption(
                                type = InterruptionType.PHONE_CALL,
                                shouldPause = true,
                                canResume = true,
                            )
                        _systemInterruptions.tryEmit(interruption)
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        // Phone call ended - can resume ChatRT session
                        val interruption =
                            SystemInterruption(
                                type = InterruptionType.PHONE_CALL,
                                shouldPause = false,
                                canResume = true,
                            )
                        _systemInterruptions.tryEmit(interruption)
                    }
                }
            }
        }

    override suspend fun initialize() {
        // Optionally register process lifecycle observer if available
        // (No-op in stub to avoid dependency on lifecycle-process)

        // Initialize current orientation
        currentOrientation = getCurrentOrientation()
    }

    override suspend fun startMonitoring() {
        if (isMonitoring) return

        registerSystemInterruptionCallbacks()
        registerSystemBroadcastReceiver()

        isMonitoring = true
    }

    override suspend fun stopMonitoring() {
        if (!isMonitoring) return

        unregisterSystemInterruptionCallbacks()
        unregisterSystemBroadcastReceiver()

        isMonitoring = false
    }

    // DefaultLifecycleObserver implementation
    override fun onCreate(owner: LifecycleOwner) {
        _lifecycleState.value = AppLifecycleState.CREATED
    }

    override fun onStart(owner: LifecycleOwner) {
        _lifecycleState.value = AppLifecycleState.STARTED
    }

    override fun onResume(owner: LifecycleOwner) {
        _lifecycleState.value = AppLifecycleState.RESUMED
    }

    override fun onPause(owner: LifecycleOwner) {
        _lifecycleState.value = AppLifecycleState.PAUSED
    }

    override fun onStop(owner: LifecycleOwner) {
        _lifecycleState.value = AppLifecycleState.STOPPED
    }

    override fun onDestroy(owner: LifecycleOwner) {
        _lifecycleState.value = AppLifecycleState.DESTROYED
        kotlinx.coroutines.GlobalScope.launch { cleanup() }
    }

    override suspend fun handleAppBackground() {
        _lifecycleState.value = AppLifecycleState.STOPPED

        // Emit system interruption for background handling
        val interruption =
            SystemInterruption(
                type = InterruptionType.LOW_POWER_MODE,
                shouldPause = false, // Continue in background for calls
                canResume = true,
            )
        _systemInterruptions.tryEmit(interruption)
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
        if (currentOrientation != orientation) {
            currentOrientation = orientation

            // Notify about orientation change for UI adaptation
            // This would typically trigger camera/video orientation updates
        }
    }

    override fun observeLifecycleState(): Flow<AppLifecycleState> = _lifecycleState.asStateFlow()

    override fun observeSystemInterruptions(): Flow<SystemInterruption> = _systemInterruptions.asSharedFlow()

    override suspend fun getCurrentLifecycleState(): AppLifecycleState = _lifecycleState.value

    override suspend fun registerSystemInterruptionCallbacks() {
        // Register for phone state changes (requires READ_PHONE_STATE permission)
        try {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (e: SecurityException) {
            // Permission not granted - phone call detection won't work
        }
    }

    override suspend fun unregisterSystemInterruptionCallbacks() {
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
    }

    override suspend fun cleanup() {
        stopMonitoring()
    }

    private fun registerSystemBroadcastReceiver() {
        systemReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    when (intent?.action) {
                        Intent.ACTION_BATTERY_LOW -> {
                            val interruption =
                                SystemInterruption(
                                    type = InterruptionType.LOW_POWER_MODE,
                                    shouldPause = false,
                                    canResume = true,
                                )
                            _systemInterruptions.tryEmit(interruption)
                        }
                        Intent.ACTION_POWER_CONNECTED -> {
                            // Power connected - can resume full quality
                        }
                        Intent.ACTION_POWER_DISCONNECTED -> {
                            // Power disconnected - consider battery optimization
                            val interruption =
                                SystemInterruption(
                                    type = InterruptionType.LOW_POWER_MODE,
                                    shouldPause = false,
                                    canResume = true,
                                )
                            _systemInterruptions.tryEmit(interruption)
                        }
                        PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                            if (powerManager.isPowerSaveMode) {
                                val interruption =
                                    SystemInterruption(
                                        type = InterruptionType.LOW_POWER_MODE,
                                        shouldPause = false,
                                        canResume = true,
                                    )
                                _systemInterruptions.tryEmit(interruption)
                            }
                        }
                        Intent.ACTION_CONFIGURATION_CHANGED -> {
                            // Handle configuration changes (orientation, etc.)
                            handleConfigurationChange()
                        }
                    }
                }
            }

        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_LOW)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                addAction(Intent.ACTION_CONFIGURATION_CHANGED)
            }

        context.registerReceiver(systemReceiver, filter)
    }

    private fun unregisterSystemBroadcastReceiver() {
        systemReceiver?.let { receiver ->
            try {
                context.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                // Receiver was not registered
            }
        }
        systemReceiver = null
    }

    private fun handleConfigurationChange() {
        val newOrientation = getCurrentOrientation()
        if (newOrientation != currentOrientation) {
            kotlinx.coroutines.GlobalScope.launch {
                handleOrientationChange(newOrientation)
            }
        }
    }

    private fun getCurrentOrientation(): DeviceOrientation =
        when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> DeviceOrientation.PORTRAIT
            Configuration.ORIENTATION_LANDSCAPE -> DeviceOrientation.LANDSCAPE
            else -> DeviceOrientation.UNKNOWN
        }

    /**
     * Handle phone call start - pause ChatRT session
     * Requirement: 5.2
     */
    suspend fun handlePhoneCallStart() {
        val interruption =
            SystemInterruption(
                type = InterruptionType.PHONE_CALL,
                shouldPause = true,
                canResume = true,
            )
        _systemInterruptions.tryEmit(interruption)
    }

    /**
     * Handle phone call end - resume ChatRT session
     * Requirement: 5.2
     */
    suspend fun handlePhoneCallEnd() {
        val interruption =
            SystemInterruption(
                type = InterruptionType.PHONE_CALL,
                shouldPause = false,
                canResume = true,
            )
        _systemInterruptions.tryEmit(interruption)
    }

    /**
     * Handle device orientation change with UI adaptation
     * Requirement: 5.4
     */
    suspend fun handleDeviceOrientationChange(orientation: Int) {
        val deviceOrientation =
            when (orientation) {
                0 -> DeviceOrientation.PORTRAIT
                90 -> DeviceOrientation.LANDSCAPE
                180 -> DeviceOrientation.PORTRAIT_REVERSE
                270 -> DeviceOrientation.LANDSCAPE_REVERSE
                else -> DeviceOrientation.UNKNOWN
            }

        handleOrientationChange(deviceOrientation)
    }

    /**
     * Handle low battery condition
     * Requirement: 5.6
     */
    suspend fun handleLowBattery(): BatteryOptimization {
        val batteryLevel = getBatteryLevel()

        return when {
            batteryLevel < 15 -> BatteryOptimization.AGGRESSIVE
            batteryLevel < 30 -> BatteryOptimization.MODERATE
            else -> BatteryOptimization.NONE
        }
    }

    /**
     * Request battery optimization exemption for background operation
     * Requirement: 5.1
     */
    suspend fun requestBatteryOptimizationExemption() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                val intent =
                    Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Handle case where intent cannot be resolved
                }
            }
        }
    }

    private fun getBatteryLevel(): Int =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } else {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                (level * 100 / scale)
            } else {
                -1
            }
        }
}

/**
 * Battery optimization levels
 */
enum class BatteryOptimization {
    NONE,
    MODERATE,
    AGGRESSIVE,
}

/**
 * Factory function for creating Android lifecycle manager
 */
actual fun createLifecycleManager(): LifecycleManager =
    throw IllegalStateException("Android LifecycleManager requires Context. Use AndroidLifecycleManager(context) directly.")
