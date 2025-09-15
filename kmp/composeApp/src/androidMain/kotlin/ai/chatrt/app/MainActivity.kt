package ai.chatrt.app

import ai.chatrt.app.platform.AndroidLifecycleManager
import ai.chatrt.app.service.ChatRtServiceManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val lifecycleManager: AndroidLifecycleManager by inject()
    private lateinit var serviceManager: ChatRtServiceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize service manager
        serviceManager = ChatRtServiceManager(this)

        // Initialize lifecycle manager
        lifecycleScope.launch {
            lifecycleManager.initialize()
            lifecycleManager.startMonitoring()
        }

        setContent {
            // Handle service binding in Compose
            DisposableEffect(Unit) {
                serviceManager.bindService()
                onDispose {
                    serviceManager.unbindService()
                }
            }

            AndroidApp()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Handle device orientation changes with UI adaptation
        // Requirement: 5.4
        lifecycleScope.launch {
            val orientation =
                when (newConfig.orientation) {
                    Configuration.ORIENTATION_PORTRAIT -> 0
                    Configuration.ORIENTATION_LANDSCAPE -> 90
                    else -> 0
                }
            lifecycleManager.handleDeviceOrientationChange(orientation)
        }
    }

    override fun onPause() {
        super.onPause()

        // Handle app backgrounding during active calls with service continuation
        // Requirement: 5.1
        lifecycleScope.launch {
            lifecycleManager.handleAppPause()
        }
    }

    override fun onResume() {
        super.onResume()

        // Handle app foregrounding
        // Requirement: 5.1
        lifecycleScope.launch {
            lifecycleManager.handleAppResume()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Implement proper resource cleanup on app termination
        // Requirement: 5.5
        lifecycleScope.launch {
            lifecycleManager.handleAppDestroy()
        }

        serviceManager.unbindService()
    }
}

@Preview
@Suppress("FunctionName")
@Composable
fun AppAndroidPreview() {
    AndroidApp()
}
