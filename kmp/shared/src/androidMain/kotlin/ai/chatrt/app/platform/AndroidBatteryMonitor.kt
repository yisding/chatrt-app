@file:Suppress("PropertyName")

package ai.chatrt.app.platform

import ai.chatrt.app.models.PowerSavingMode
import ai.chatrt.app.models.PowerSavingRecommendation
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android implementation of BatteryMonitor
 */
class AndroidBatteryMonitor(
    private val context: Context,
) : BatteryMonitor {
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val _batteryState = MutableStateFlow(createInitialBatteryState())

    override suspend fun initialize() {
        // Initialize battery monitoring
        updateBatteryState()
    }

    override suspend fun startMonitoring() {
        // Register battery state receiver
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
        // Would register BroadcastReceiver here
    }

    override suspend fun stopMonitoring() {
        // Unregister battery state receiver
    }

    override suspend fun getCurrentBatteryLevel(): Int = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

    override suspend fun isBatteryLow(): Boolean = getCurrentBatteryLevel() < 20

    override suspend fun isCharging(): Boolean {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    override suspend fun getBatteryState(): ai.chatrt.app.platform.BatteryState = _batteryState.value

    override fun observeBatteryState(): Flow<ai.chatrt.app.platform.BatteryState> = _batteryState.asStateFlow()

    override suspend fun getPowerSavingRecommendation(): PowerSavingRecommendation? {
        val batteryLevel = getCurrentBatteryLevel()
        val isCharging = isCharging()

        return when {
            batteryLevel < 15 && !isCharging ->
                PowerSavingRecommendation(
                    mode = PowerSavingMode.AGGRESSIVE,
                    reason = "Battery critically low",
                    estimatedBatteryGain = "30-60 minutes",
                )
            batteryLevel < 30 && !isCharging ->
                PowerSavingRecommendation(
                    mode = PowerSavingMode.MODERATE,
                    reason = "Battery low",
                    estimatedBatteryGain = "15-30 minutes",
                )
            else -> null
        }
    }

    override suspend fun applyPowerSavingMode(mode: PowerSavingMode) {
        // Apply power saving mode settings
        // This would adjust various system settings based on the mode
    }

    override suspend fun isPowerSavingModeActive(): Boolean = powerManager.isPowerSaveMode

    override suspend fun requestBatteryOptimizationExemption(): Boolean {
        // Request battery optimization exemption
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            !powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    override suspend fun cleanup() {
        stopMonitoring()
    }

    private fun createInitialBatteryState(): ai.chatrt.app.platform.BatteryState {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = (level * 100 / scale.toFloat()).toInt()

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging =
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val chargingType =
            when (chargePlug) {
                BatteryManager.BATTERY_PLUGGED_AC -> ChargingType.AC
                BatteryManager.BATTERY_PLUGGED_USB -> ChargingType.USB
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargingType.WIRELESS
                else -> ChargingType.NOT_CHARGING
            }

        val temperature = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val voltage = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

        val health =
            batteryStatus?.getIntExtra(
                BatteryManager.EXTRA_HEALTH,
                BatteryManager.BATTERY_HEALTH_UNKNOWN,
            ) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val batteryHealth =
            when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> ai.chatrt.app.platform.BatteryHealth.GOOD
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> ai.chatrt.app.platform.BatteryHealth.OVERHEAT
                BatteryManager.BATTERY_HEALTH_DEAD -> ai.chatrt.app.platform.BatteryHealth.DEAD
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> ai.chatrt.app.platform.BatteryHealth.OVER_VOLTAGE
                BatteryManager.BATTERY_HEALTH_COLD -> ai.chatrt.app.platform.BatteryHealth.COLD
                else -> ai.chatrt.app.platform.BatteryHealth.UNKNOWN
            }

        val powerSavingMode =
            if (powerManager.isPowerSaveMode) {
                PowerSavingMode.MODERATE
            } else {
                PowerSavingMode.NONE
            }

        return ai.chatrt.app.platform.BatteryState(
            level = batteryPct,
            isCharging = isCharging,
            chargingType = chargingType,
            temperature = temperature / 10.0f, // Convert from tenths of degree Celsius
            voltage = voltage / 1000.0f, // Convert from millivolts to volts
            health = batteryHealth,
            powerSavingMode = powerSavingMode,
        )
    }

    private fun updateBatteryState() {
        _batteryState.value = createInitialBatteryState()
    }
}

/**
 * Factory function for creating Android battery monitor
 */
actual fun createBatteryMonitor(): BatteryMonitor =
    throw IllegalStateException("Android BatteryMonitor requires Context. Use AndroidBatteryMonitor(context) directly.")
