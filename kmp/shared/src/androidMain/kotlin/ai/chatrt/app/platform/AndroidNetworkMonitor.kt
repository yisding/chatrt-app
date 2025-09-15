@file:Suppress("PropertyName")

package ai.chatrt.app.platform

import ai.chatrt.app.models.NetworkQuality
import android.content.Context
import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android implementation of NetworkMonitor
 */
class AndroidNetworkMonitor(
    private val context: Context,
) : NetworkMonitor {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _networkState = MutableStateFlow(NetworkState(false, NetworkType.NONE))
    private val _networkQuality = MutableStateFlow(NetworkQuality.POOR)

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateNetworkState()
            }

            override fun onLost(network: Network) {
                updateNetworkState()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                updateNetworkState()
                updateNetworkQuality(networkCapabilities)
            }
        }

    override suspend fun initialize() {
        // Initialize network monitoring
        updateNetworkState()
    }

    @SuppressLint("MissingPermission")
    override suspend fun startMonitoring() {
        val request =
            NetworkRequest
                .Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    @SuppressLint("MissingPermission")
    override suspend fun stopMonitoring() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    override suspend fun getCurrentNetworkState(): NetworkState = _networkState.value

    override suspend fun getCurrentNetworkQuality(): NetworkQuality = _networkQuality.value

    override fun observeNetworkState(): Flow<NetworkState> = _networkState.asStateFlow()

    override fun observeNetworkQuality(): Flow<NetworkQuality> = _networkQuality.asStateFlow()

    @SuppressLint("MissingPermission")
    override suspend fun getNetworkCapabilities(): ai.chatrt.app.platform.NetworkCapabilities? {
        val activeNetwork = connectivityManager.activeNetwork ?: return null
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return null

        return ai.chatrt.app.platform.NetworkCapabilities(
            downloadBandwidth = capabilities.linkDownstreamBandwidthKbps * 1000L,
            uploadBandwidth = capabilities.linkUpstreamBandwidthKbps * 1000L,
            // Would need to measure
            latency = 0,
            supportsInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
            isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
        )
    }

    override suspend fun testConnectivity(
        host: String,
        port: Int,
    ): Boolean {
        // Test connectivity to specific host
        return try {
            // Would implement actual connectivity test
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun measureBandwidth(): BandwidthInfo? {
        // Measure network bandwidth
        return BandwidthInfo(
            // 10 Mbps placeholder
            downloadSpeed = 10_000_000L,
            // 5 Mbps placeholder
            uploadSpeed = 5_000_000L,
            // 50ms placeholder
            latency = 50,
            // 10ms placeholder
            jitter = 10,
            // 0.1% placeholder
            packetLoss = 0.1f,
        )
    }

    override suspend fun cleanup() {
        stopMonitoring()
    }

    @SuppressLint("MissingPermission")
    private fun updateNetworkState() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }

        val networkType =
            when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> NetworkType.WIFI
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> NetworkType.CELLULAR
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> NetworkType.ETHERNET
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) == true -> NetworkType.BLUETOOTH
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> NetworkType.VPN
                else -> NetworkType.NONE
            }

        val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val isMetered = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)?.let { !it } ?: true

        _networkState.value =
            NetworkState(
                isConnected = isConnected,
                networkType = networkType,
                isMetered = isMetered,
                // Would need to get actual signal strength
                signalStrength = -1,
            )
    }

    private fun updateNetworkQuality(capabilities: NetworkCapabilities) {
        val downstreamBandwidth = capabilities.linkDownstreamBandwidthKbps

        _networkQuality.value =
            when {
                // 10+ Mbps
                downstreamBandwidth >= 10000 -> NetworkQuality.EXCELLENT
                // 5-10 Mbps
                downstreamBandwidth >= 5000 -> NetworkQuality.GOOD
                // 1-5 Mbps
                downstreamBandwidth >= 1000 -> NetworkQuality.FAIR
                // < 1 Mbps
                else -> NetworkQuality.POOR
            }
    }
}

/**
 * Factory function for creating Android network monitor
 */
actual fun createNetworkMonitor(): NetworkMonitor =
    throw IllegalStateException("Android NetworkMonitor requires Context. Use AndroidNetworkMonitor(context) directly.")
