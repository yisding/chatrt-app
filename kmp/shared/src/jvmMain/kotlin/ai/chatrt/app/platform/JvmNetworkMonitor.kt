package ai.chatrt.app.platform

import ai.chatrt.app.models.NetworkQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * JVM/Desktop implementation of NetworkMonitor
 */
class JvmNetworkMonitor : NetworkMonitor {
    
    private val _networkState = MutableStateFlow(NetworkState(false, NetworkType.NONE))
    private val _networkQuality = MutableStateFlow(NetworkQuality.GOOD)
    
    override suspend fun initialize() {
        // Initialize network monitoring for desktop
        updateNetworkState()
    }
    
    override suspend fun startMonitoring() {
        // Start monitoring network changes on desktop
        // This would typically use Java NIO or other network monitoring APIs
        updateNetworkState()
    }
    
    override suspend fun stopMonitoring() {
        // Stop monitoring network changes
    }
    
    override suspend fun getCurrentNetworkState(): NetworkState {
        updateNetworkState()
        return _networkState.value
    }
    
    override suspend fun getCurrentNetworkQuality(): NetworkQuality {
        return _networkQuality.value
    }
    
    override fun observeNetworkState(): Flow<NetworkState> = _networkState.asStateFlow()
    
    override fun observeNetworkQuality(): Flow<NetworkQuality> = _networkQuality.asStateFlow()
    
    override suspend fun getNetworkCapabilities(): ai.chatrt.app.platform.NetworkCapabilities? {
        // Get network capabilities for desktop
        return ai.chatrt.app.platform.NetworkCapabilities(
            downloadBandwidth = 100_000_000L, // 100 Mbps default for desktop
            uploadBandwidth = 50_000_000L,    // 50 Mbps default for desktop
            latency = 20,                     // 20ms default
            supportsInternet = isConnectedToInternet(),
            isValidated = true
        )
    }
    
    override suspend fun testConnectivity(host: String, port: Int): Boolean {
        return try {
            val address = InetAddress.getByName(host)
            address.isReachable(5000) // 5 second timeout
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun measureBandwidth(): BandwidthInfo? {
        // Measure network bandwidth on desktop
        // This would require actual network testing
        return BandwidthInfo(
            downloadSpeed = 50_000_000L, // 50 Mbps placeholder
            uploadSpeed = 25_000_000L,   // 25 Mbps placeholder
            latency = 30,                // 30ms placeholder
            jitter = 5,                  // 5ms placeholder
            packetLoss = 0.05f           // 0.05% placeholder
        )
    }
    
    override suspend fun cleanup() {
        stopMonitoring()
    }
    
    private fun updateNetworkState() {
        try {
            val isConnected = isConnectedToInternet()
            val networkType = detectNetworkType()
            
            _networkState.value = NetworkState(
                isConnected = isConnected,
                networkType = networkType,
                isMetered = false, // Desktop connections are typically not metered
                signalStrength = if (isConnected) 100 else 0
            )
            
            // Update network quality based on connection type
            _networkQuality.value = when (networkType) {
                NetworkType.ETHERNET -> NetworkQuality.EXCELLENT
                NetworkType.WIFI -> NetworkQuality.GOOD
                NetworkType.CELLULAR -> NetworkQuality.FAIR
                else -> NetworkQuality.POOR
            }
        } catch (e: Exception) {
            _networkState.value = NetworkState(false, NetworkType.NONE)
            _networkQuality.value = NetworkQuality.POOR
        }
    }
    
    private fun isConnectedToInternet(): Boolean {
        return try {
            val address = InetAddress.getByName("8.8.8.8")
            address.isReachable(3000) // 3 second timeout
        } catch (e: Exception) {
            false
        }
    }
    
    private fun detectNetworkType(): NetworkType {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isUp && !networkInterface.isLoopback) {
                    val name = networkInterface.name.lowercase()
                    return when {
                        name.contains("eth") || name.contains("en") -> NetworkType.ETHERNET
                        name.contains("wlan") || name.contains("wifi") || name.contains("wl") -> NetworkType.WIFI
                        name.contains("ppp") || name.contains("cellular") -> NetworkType.CELLULAR
                        name.contains("tun") || name.contains("vpn") -> NetworkType.VPN
                        name.contains("bt") || name.contains("bluetooth") -> NetworkType.BLUETOOTH
                        else -> NetworkType.UNKNOWN
                    }
                }
            }
            NetworkType.NONE
        } catch (e: Exception) {
            NetworkType.UNKNOWN
        }
    }
}

/**
 * Factory function for creating JVM network monitor
 */
actual fun createNetworkMonitor(): NetworkMonitor = JvmNetworkMonitor()