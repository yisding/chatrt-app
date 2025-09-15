package ai.chatrt.app.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for AndroidLifecycleManager
 * Requirements: 5.1, 5.2, 5.3, 5.6
 */
@RunWith(AndroidJUnit4::class)
class AndroidLifecycleManagerTest {
    private lateinit var context: Context
    private lateinit var lifecycleManager: AndroidLifecycleManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        lifecycleManager = AndroidLifecycleManager(context)
    }

    @Test
    fun testInitialize() =
        runTest {
            lifecycleManager.initialize()
            // Should complete without error
        }

    @Test
    fun testStartAndStopMonitoring() =
        runTest {
            lifecycleManager.initialize()

            lifecycleManager.startMonitoring()
            lifecycleManager.stopMonitoring()

            // Should complete without error
        }

    @Test
    fun testObserveLifecycleState() =
        runTest {
            lifecycleManager.initialize()

            val initialState = lifecycleManager.observeLifecycleState().first()
            assertEquals(AppLifecycleState.CREATED, initialState)
        }

    @Test
    fun testGetCurrentLifecycleState() =
        runTest {
            lifecycleManager.initialize()

            val currentState = lifecycleManager.getCurrentLifecycleState()
            assertEquals(AppLifecycleState.CREATED, currentState)
        }

    @Test
    fun testHandleAppLifecycleEvents() =
        runTest {
            lifecycleManager.initialize()

            lifecycleManager.handleAppBackground()
            assertEquals(AppLifecycleState.STOPPED, lifecycleManager.getCurrentLifecycleState())

            lifecycleManager.handleAppForeground()
            assertEquals(AppLifecycleState.RESUMED, lifecycleManager.getCurrentLifecycleState())

            lifecycleManager.handleAppPause()
            assertEquals(AppLifecycleState.PAUSED, lifecycleManager.getCurrentLifecycleState())

            lifecycleManager.handleAppResume()
            assertEquals(AppLifecycleState.RESUMED, lifecycleManager.getCurrentLifecycleState())
        }

    @Test
    fun testHandleOrientationChange() =
        runTest {
            lifecycleManager.initialize()

            lifecycleManager.handleOrientationChange(DeviceOrientation.LANDSCAPE)
            lifecycleManager.handleOrientationChange(DeviceOrientation.PORTRAIT)

            // Should complete without error
        }

    @Test
    fun testHandleDeviceOrientationChange() =
        runTest {
            lifecycleManager.initialize()

            lifecycleManager.handleDeviceOrientationChange(90) // Landscape
            lifecycleManager.handleDeviceOrientationChange(0) // Portrait
            lifecycleManager.handleDeviceOrientationChange(180) // Portrait reverse
            lifecycleManager.handleDeviceOrientationChange(270) // Landscape reverse

            // Should complete without error
        }

    @Test
    fun testObserveSystemInterruptions() =
        runTest {
            lifecycleManager.initialize()

            val interruptionsFlow = lifecycleManager.observeSystemInterruptions()
            assertNotNull(interruptionsFlow)
        }

    @Test
    fun testHandlePhoneCallEvents() =
        runTest {
            lifecycleManager.initialize()
            lifecycleManager.startMonitoring()

            lifecycleManager.handlePhoneCallStart()
            lifecycleManager.handlePhoneCallEnd()

            // Should complete without error
        }

    @Test
    fun testHandleLowBattery() =
        runTest {
            lifecycleManager.initialize()

            val batteryOptimization = lifecycleManager.handleLowBattery()
            assertNotNull(batteryOptimization)
            assertTrue(
                batteryOptimization == BatteryOptimization.NONE ||
                    batteryOptimization == BatteryOptimization.MODERATE ||
                    batteryOptimization == BatteryOptimization.AGGRESSIVE,
            )
        }

    @Test
    fun testRequestBatteryOptimizationExemption() =
        runTest {
            lifecycleManager.initialize()

            lifecycleManager.requestBatteryOptimizationExemption()

            // Should complete without error (may not actually show dialog in test)
        }

    @Test
    fun testRegisterAndUnregisterSystemInterruptionCallbacks() =
        runTest {
            lifecycleManager.initialize()

            lifecycleManager.registerSystemInterruptionCallbacks()
            lifecycleManager.unregisterSystemInterruptionCallbacks()

            // Should complete without error
        }

    @Test
    fun testHandleAppDestroy() =
        runTest {
            lifecycleManager.initialize()
            lifecycleManager.startMonitoring()

            lifecycleManager.handleAppDestroy()

            assertEquals(AppLifecycleState.DESTROYED, lifecycleManager.getCurrentLifecycleState())
        }

    @Test
    fun testCleanup() =
        runTest {
            lifecycleManager.initialize()
            lifecycleManager.startMonitoring()

            lifecycleManager.cleanup()

            // Should complete without error
        }
}
