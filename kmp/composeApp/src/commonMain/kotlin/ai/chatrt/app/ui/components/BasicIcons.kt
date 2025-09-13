package ai.chatrt.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Basic icons using only the most fundamental Material Icons
 * Used as fallbacks for missing icons in Compose Multiplatform
 */
object BasicIcons {
    // Network and connectivity - using basic icons
    val wifi: ImageVector = Icons.Default.Settings
    val wifiOff: ImageVector = Icons.Default.Settings
    val signal4: ImageVector = Icons.Default.Settings
    val signal3: ImageVector = Icons.Default.Settings
    val signal2: ImageVector = Icons.Default.Settings
    val signal1: ImageVector = Icons.Default.Settings

    // Media and camera - using basic icons
    val videocam: ImageVector = Icons.Default.PlayArrow
    val videocamOff: ImageVector = Icons.Default.PlayArrow
    val mic: ImageVector = Icons.Default.PlayArrow
    val micOff: ImageVector = Icons.Default.PlayArrow
    val cameraAlt: ImageVector = Icons.Default.PlayArrow
    val cameraSwitch: ImageVector = Icons.Default.Refresh
    val screenShare: ImageVector = Icons.Default.PlayArrow
    val screenShareOff: ImageVector = Icons.Default.PlayArrow

    // Communication
    val call: ImageVector = Icons.Default.Phone
    val callEnd: ImageVector = Icons.Default.Phone
    val videoCall: ImageVector = Icons.Default.Phone
    val phone: ImageVector = Icons.Default.Phone
    val phoneDisabled: ImageVector = Icons.Default.Phone

    // System and UI
    val error: ImageVector = Icons.Default.Warning
    val warning: ImageVector = Icons.Default.Warning
    val info: ImageVector = Icons.Default.Info
    val refresh: ImageVector = Icons.Default.Refresh
    val sync: ImageVector = Icons.Default.Refresh
    val close: ImageVector = Icons.Default.Close
    val checkCircle: ImageVector = Icons.Default.Check
    val block: ImageVector = Icons.Default.Settings
    val pause: ImageVector = Icons.Default.PlayArrow
    val playArrow: ImageVector = Icons.Default.PlayArrow

    // Storage and system
    val cloud: ImageVector = Icons.Default.Settings
    val cloudOff: ImageVector = Icons.Default.Settings
    val storage: ImageVector = Icons.Default.Settings
    val memory: ImageVector = Icons.Default.Settings
    val battery: ImageVector = Icons.Default.Settings
    val batteryAlert: ImageVector = Icons.Default.Warning

    // UI elements
    val expandMore: ImageVector = Icons.Default.KeyboardArrowDown
    val keyboardArrowDown: ImageVector = Icons.Default.KeyboardArrowDown
    val visibilityOff: ImageVector = Icons.Default.Settings
    val volumeUp: ImageVector = Icons.Default.Settings
    val trendingUp: ImageVector = Icons.Default.Settings
    val lightbulb: ImageVector = Icons.Default.Info
    val terminal: ImageVector = Icons.Default.Settings
    val eventNote: ImageVector = Icons.Default.List
    val bugReport: ImageVector = Icons.Default.Build
    val code: ImageVector = Icons.Default.Build
    val description: ImageVector = Icons.Default.List
    val tipsAndUpdates: ImageVector = Icons.Default.Info
    val presentToAll: ImageVector = Icons.Default.Share

    // Audio devices
    val headphones: ImageVector = Icons.Default.Settings
    val bluetooth: ImageVector = Icons.Default.Settings
}
