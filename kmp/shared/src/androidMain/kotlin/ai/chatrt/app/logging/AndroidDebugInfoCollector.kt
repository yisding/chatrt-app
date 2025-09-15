package ai.chatrt.app.logging

import android.app.ActivityManager
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import kotlinx.coroutines.flow.Flow

/** Android-specific implementation of DebugInfoCollector */
class AndroidDebugInfoCollector(
    private val context: Context,
    private val baseCollector: DebugInfoCollector,
) {
    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    fun collectDebugInfo(): Flow<DebugInfoCollector.DebugInfo> = baseCollector.collectDebugInfo()

    suspend fun exportDebugInfo(): String =
        buildString {
            appendLine("=== ChatRT Android Debug Information ===")
            appendLine("Generated: ${kotlinx.datetime.Clock.System.now()}")
            appendLine()

            appendLine("=== Android System Information ===")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Build: ${Build.DISPLAY}")
            appendLine("Memory Usage: ${getMemoryUsage()}")
            appendLine("CPU Usage: ${getCpuUsage()}")
            appendLine("Network Type: ${getNetworkType()}")
            appendLine("Battery Level: ${getBatteryLevel()}")
            appendLine("Available Memory: ${getAvailableMemory()}")
            appendLine("Total Memory: ${getTotalMemory()}")
            appendLine()

            appendLine("=== App Information ===")
            appendLine("Package: ${context.packageName}")
            appendLine("Version: ${getAppVersion()}")
            appendLine("Debug Mode: ${isDebugMode()}")
            appendLine()

            appendLine("=== Permissions ===")
            appendLine("Camera: ${hasPermission(android.Manifest.permission.CAMERA)}")
            appendLine("Microphone: ${hasPermission(android.Manifest.permission.RECORD_AUDIO)}")
            appendLine(
                "Network State: ${hasPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)}",
            )
            appendLine()

            // Include base debug info
            append(baseCollector.exportDebugInfo())
        }

    private fun getMemoryUsage(): String =
        try {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)

            val usedMemory = memoryInfo.totalMem - memoryInfo.availMem
            val usedMemoryMB = usedMemory / (1024 * 1024)
            val totalMemoryMB = memoryInfo.totalMem / (1024 * 1024)
            val percentage = (usedMemory.toDouble() / memoryInfo.totalMem * 100).toInt()

            "$usedMemoryMB MB / $totalMemoryMB MB ($percentage%)"
        } catch (e: Exception) {
            "Unknown (${e.message})"
        }

    private fun getCpuUsage(): String =
        try {
            // This is a simplified CPU usage - in practice you'd need more sophisticated monitoring
            val runtime = Runtime.getRuntime()
            val processors = runtime.availableProcessors()
            "Processors: $processors"
        } catch (e: Exception) {
            "Unknown (${e.message})"
        }

    @SuppressLint("MissingPermission")
    private fun getNetworkType(): String =
        try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true ->
                    "Cellular"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true ->
                    "Ethernet"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) == true ->
                    "Bluetooth"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown (${e.message})"
        }

    private fun getBatteryLevel(): String =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val batteryLevel =
                    batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                "$batteryLevel%"
            } else {
                "Unknown (API < 21)"
            }
        } catch (e: Exception) {
            "Unknown (${e.message})"
        }

    private fun getAvailableMemory(): String =
        try {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            "${memoryInfo.availMem / (1024 * 1024)} MB"
        } catch (e: Exception) {
            "Unknown"
        }

    private fun getTotalMemory(): String =
        try {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            "${memoryInfo.totalMem / (1024 * 1024)} MB"
        } catch (e: Exception) {
            "Unknown"
        }

    private fun getAppVersion(): String =
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                "${packageInfo.versionName} (${packageInfo.longVersionCode})"
            } else {
                @Suppress("DEPRECATION")
                "${packageInfo.versionName} (${packageInfo.versionCode})"
            }
        } catch (e: Exception) {
            "Unknown"
        }

    private fun isDebugMode(): Boolean =
        try {
            (
                context.applicationInfo.flags and
                    android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE
            ) != 0
        } catch (e: Exception) {
            false
        }

    private fun hasPermission(permission: String): Boolean =
        try {
            context.checkSelfPermission(permission) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
}
