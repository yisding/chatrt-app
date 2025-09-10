package ai.chatrt.app.platform

import ai.chatrt.app.models.NetworkQuality
import kotlinx.coroutines.flow.Flow

/**
 * Network monitor interface for tracking network state and quality
 */
interface NetworkMonitor {
    /**
     * Initialize network monitoring
     */
    suspend fun initialize()

    /**
     * Start monitoring network changes
     */
    suspend fun startMonitoring()

    /**
     * Stop monitoring network changes
     */
    suspend fun stopMonitoring()

    /**
     * Get current network state
     */
    suspend fun getCurrentNetworkState(): NetworkState

    /**
     * Get current network quality
     */
    suspend fun getCurrentNetworkQuality(): NetworkQuality

    /**
     * Observe network state changes
     */
    fun observeNetworkState(): Flow<NetworkState>

    /**
     * Observe network quality changes
     */
    fun observeNetworkQuality(): Flow<NetworkQuality>

    /**
     * Get network capabilities
     */
    suspend fun getNetworkCapabilities(): NetworkCapabilities?

    /**
     * Test network connectivity to a specific host
     */
    suspend fun testConnectivity(
        host: String,
        port: Int = 80,
    ): Boolean

    /**
     * Measure network bandwidth
     */
    suspend fun measureBandwidth(): BandwidthInfo?

    /**
     * Cleanup network monitoring resources
     */
    suspend fun cleanup()
}

/**
 * Network state information
 */
data class NetworkState(
    val isConnected: Boolean,
    val networkType: NetworkType,
    val isMetered: Boolean = false,
    val signalStrength: Int = 0, // 0-100, -1 if unknown
)

/**
 * Network type
 */
enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    BLUETOOTH,
    VPN,
    UNKNOWN,
    NONE,
}

/**
 * Network capabilities
 */
data class NetworkCapabilities(
    val downloadBandwidth: Long, // bits per second
    val uploadBandwidth: Long, // bits per second
    val latency: Int, // milliseconds
    val supportsInternet: Boolean,
    val isValidated: Boolean,
)

/**
 * Bandwidth measurement information
 */
data class BandwidthInfo(
    val downloadSpeed: Long, // bits per second
    val uploadSpeed: Long, // bits per second
    val latency: Int, // milliseconds
    val jitter: Int, // milliseconds
    val packetLoss: Float, // percentage (0.0 - 100.0)
)

/**
 * Expected network monitor factory function
 */
expect fun createNetworkMonitor(): NetworkMonitor
