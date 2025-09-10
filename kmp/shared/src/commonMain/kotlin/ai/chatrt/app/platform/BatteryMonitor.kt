package ai.chatrt.app.platform

import ai.chatrt.app.models.PowerSavingMode
import ai.chatrt.app.models.PowerSavingRecommendation
import kotlinx.coroutines.flow.Flow

/**
 * Battery monitor interface for tracking battery state and power management
 */
interface BatteryMonitor {
    /**
     * Initialize battery monitoring
     */
    suspend fun initialize()

    /**
     * Start monitoring battery changes
     */
    suspend fun startMonitoring()

    /**
     * Stop monitoring battery changes
     */
    suspend fun stopMonitoring()

    /**
     * Get current battery level (0-100)
     */
    suspend fun getCurrentBatteryLevel(): Int

    /**
     * Check if battery is currently low
     */
    suspend fun isBatteryLow(): Boolean

    /**
     * Check if device is currently charging
     */
    suspend fun isCharging(): Boolean

    /**
     * Get current battery state
     */
    suspend fun getBatteryState(): BatteryState

    /**
     * Observe battery state changes
     */
    fun observeBatteryState(): Flow<BatteryState>

    /**
     * Get power saving recommendation based on current state
     */
    suspend fun getPowerSavingRecommendation(): PowerSavingRecommendation?

    /**
     * Apply power saving mode
     */
    suspend fun applyPowerSavingMode(mode: PowerSavingMode)

    /**
     * Check if power saving mode is active
     */
    suspend fun isPowerSavingModeActive(): Boolean

    /**
     * Request battery optimization exemption (Android)
     */
    suspend fun requestBatteryOptimizationExemption(): Boolean

    /**
     * Cleanup battery monitoring resources
     */
    suspend fun cleanup()
}

/**
 * Battery state information
 */
data class BatteryState(
    val level: Int, // 0-100
    val isCharging: Boolean,
    val chargingType: ChargingType,
    val temperature: Float, // Celsius
    val voltage: Float, // Volts
    val health: BatteryHealth,
    val powerSavingMode: PowerSavingMode,
)

/**
 * Charging type
 */
enum class ChargingType {
    NOT_CHARGING,
    AC,
    USB,
    WIRELESS,
    UNKNOWN,
}

/**
 * Battery health status
 */
enum class BatteryHealth {
    GOOD,
    OVERHEAT,
    DEAD,
    OVER_VOLTAGE,
    COLD,
    UNKNOWN,
}

/**
 * Expected battery monitor factory function
 */
expect fun createBatteryMonitor(): BatteryMonitor
