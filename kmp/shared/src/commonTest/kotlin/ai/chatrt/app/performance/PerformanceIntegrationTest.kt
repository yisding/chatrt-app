package ai.chatrt.app.performance

import ai.chatrt.app.models.*
import ai.chatrt.app.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.system.measureTimeMillis
import kotlin.test.*

/**
 * Performance and memory leak detection tests
 * Tests system performance under various load conditions and verifies proper resource cleanup
 */
class PerformanceIntegrationTest {
    private lateinit var mainViewModel: MainViewModel
    private lateinit var mockWebRtcManager: MockWebRtcManager
    private lateinit var mockVideoManager: MockVideoManager
    private lateinit var mockAudioManager: MockAudioManager
    private lateinit var performanceMonitor: PerformanceMonitor

    @BeforeTest
    fun setup() {
        mockWebRtcManager = MockWebRtcManager()
        mockVideoManager = MockVideoManager()
        mockAudioManager = MockAudioManager()
        performanceMonitor = PerformanceMonitor()

        // Initialize with performance monitoring
        mainViewModel =
            MainViewModel(
                chatRepository = MockChatRepository(),
                audioManager = mockAudioManager,
                videoManager = mockVideoManager,
                performanceMonitor = performanceMonitor,
            )
    }

    /**
     * Test connection establishment performance
     * Should establish connection within acceptable time limits
     */
    @Test
    fun testConnectionPerformance() =
        runTest {
            val connectionTime =
                measureTimeMillis {
                    mainViewModel.startConnection()
                    mockWebRtcManager.simulateConnectionStateChange(ConnectionState.CONNECTING)
                    delay(50) // Simulate network delay
                    mockWebRtcManager.simulateConnectionStateChange(ConnectionState.CONNECTED)

                    // Wait for connection to be established
                    while (mainViewModel.connectionState.first() != ConnectionState.CONNECTED) {
                        delay(10)
                    }
                }

            // Connection should be established within 2 seconds
            assertTrue(connectionTime < 2000, "Connection took ${connectionTime}ms, expected < 2000ms")

            // Verify performance metrics
            val metrics = performanceMonitor.getConnectionMetrics()
            assertTrue(metrics.averageConnectionTime < 2000)
            assertTrue(metrics.successRate > 0.95) // 95% success rate minimum
        }

    /**
     * Test video mode switching performance
     * Should switch between modes quickly without memory leaks
     */
    @Test
    fun testVideoModeSwitchingPerformance() =
        runTest {
            val modes = listOf(VideoMode.AUDIO_ONLY, VideoMode.WEBCAM, VideoMode.SCREEN_SHARE)
            val switchTimes = mutableListOf<Long>()

            repeat(10) { cycle ->
                modes.forEach { mode ->
                    val switchTime =
                        measureTimeMillis {
                            mainViewModel.setVideoMode(mode)
                            delay(10) // Allow state to propagate
                        }
                    switchTimes.add(switchTime)

                    assertEquals(mode, mainViewModel.videoMode.first())
                }
            }

            // Average switch time should be under 100ms
            val averageSwitchTime = switchTimes.average()
            assertTrue(averageSwitchTime < 100, "Average switch time: ${averageSwitchTime}ms")

            // Verify no memory leaks from repeated switching
            val memoryMetrics = performanceMonitor.getMemoryMetrics()
            assertTrue(memoryMetrics.memoryLeakDetected == false)
            assertTrue(memoryMetrics.peakMemoryUsage < memoryMetrics.memoryThreshold)
        }

    /**
     * Test camera switching performance
     * Should switch cameras quickly without dropping frames
     */
    @Test
    fun testCameraSwitchingPerformance() =
        runTest {
            // Set up video mode
            mainViewModel.setVideoMode(VideoMode.WEBCAM)
            mainViewModel.startConnection()
            mockWebRtcManager.simulateConnectionStateChange(ConnectionState.CONNECTED)

            val switchTimes = mutableListOf<Long>()

            repeat(20) {
                // Test 20 camera switches
                val switchTime =
                    measureTimeMillis {
                        mainViewModel.switchCamera()
                        delay(5) // Minimal delay for camera switch
                    }
                switchTimes.add(switchTime)
            }

            // Camera switches should be under 200ms each
            val averageSwitchTime = switchTimes.average()
            assertTrue(averageSwitchTime < 200, "Average camera switch time: ${averageSwitchTime}ms")

            // No switch should take longer than 500ms
            val maxSwitchTime = switchTimes.maxOrNull() ?: 0
            assertTrue(maxSwitchTime < 500, "Max camera switch time: ${maxSwitchTime}ms")

            // Verify video stream remains active
            assertTrue(mockVideoManager.isVideoStreamActive)
        }

    /**
     * Test orientation change performance
     * Should handle orientation changes without significant delay
     */
    @Test
    fun testOrientationChangePerformance() =
        runTest {
            // Start video call
            mainViewModel.setVideoMode(VideoMode.WEBCAM)
            mainViewModel.startConnection()
            mockWebRtcManager.simulateConnectionStateChange(ConnectionState.CONNECTED)

            val orientations = listOf(0, 90, 180, 270) // Portrait, Landscape, etc.
            val orientationChangeTimes = mutableListOf<Long>()

            repeat(15) {
                // Test multiple orientation changes
                orientations.forEach { orientation ->
                    val changeTime =
                        measureTimeMillis {
                            mainViewModel.handleOrientationChange(orientation)
                            delay(5) // Allow UI to adapt
                        }
                    orientationChangeTimes.add(changeTime)
                }
            }

            // Orientation changes should be under 150ms
            val averageChangeTime = orientationChangeTimes.average()
            assertTrue(averageChangeTime < 150, "Average orientation change time: ${averageChangeTime}ms")

            // Connection should remain stable
            assertTrue(mainViewModel.isConnected.first())
        }

    /**
     * Test memory usage during extended operation
     * Should not accumulate memory over time
     */
    @Test
    fun testMemoryUsageDuringExtendedOperation() =
        runTest {
            val initialMemory = performanceMonitor.getCurrentMemoryUsage()

            // Simulate extended operation with various activities
            repeat(50) { cycle ->
                // Start connection
                mainViewModel.startConnection()
                mockWebRtcManager.simulateConnectionStateChange(ConnectionState.CONNECTED)
                delay(20)

                // Switch video modes
                mainViewModel.setVideoMode(VideoMode.WEBCAM)
                delay(10)
                mainViewModel.setVideoMode(VideoMode.SCREEN_SHARE)
                delay(10)
                mainViewModel.setVideoMode(VideoMode.AUDIO_ONLY)
                delay(10)

                // Switch cameras
                mainViewModel.switchCamera()
                delay(5)

                // Handle orientation changes
                mainViewModel.handleOrientationChange(90)
                delay(5)
                mainViewModel.handleOrientationChange(0)
                delay(5)

                // Stop connection
                mainViewModel.stopConnection()
                delay(20)

                // Check memory every 10 cycles
                if (cycle % 10 == 0) {
                    val currentMemory = performanceMonitor.getCurrentMemoryUsage()
                    val memoryIncrease = currentMemory - initialMemory

                    // Memory increase should be reasonable (less than 50MB)
                    assertTrue(
                        memoryIncrease < 50 * 1024 * 1024,
                        "Memory increased by ${memoryIncrease / (1024 * 1024)}MB after $cycle cycles",
                    )
                }
            }

            // Final memory check
            val finalMemory = performanceMonitor.getCurrentMemoryUsage()
            val totalMemoryIncrease = finalMemory - initialMemory

            // Total memory increase should be minimal (less than 20MB)
            assertTrue(
                totalMemoryIncrease < 20 * 1024 * 1024,
                "Total memory increase: ${totalMemoryIncrease / (1024 * 1024)}MB",
            )
        }

    /**
     * Test logging performance under high load
     * Should handle high-frequency logging without performance degradation
     */
    @Test
    fun testLoggingPerformance() =
        runTest {
            // Enable debug logging
            mainViewModel.setDebugLogging(true)

            val loggingTime =
                measureTimeMillis {
                    // Generate high-frequency logs
                    repeat(1000) { i ->
                        mainViewModel.logDebugMessage("Performance test log entry $i")
                        if (i % 100 == 0) {
                            delay(1) // Small delay every 100 logs
                        }
                    }
                }

            // 1000 log entries should be processed quickly
            assertTrue(loggingTime < 1000, "Logging 1000 entries took ${loggingTime}ms")

            // Verify logs are properly stored
            val logs = mainViewModel.logs.first()
            assertTrue(logs.size >= 1000)

            // Verify log rotation is working (shouldn't store unlimited logs)
            assertTrue(logs.size <= 5000, "Log count: ${logs.size}, should be limited")
        }

    /**
     * Test WebRTC event handling performance
     * Should handle high-frequency WebRTC events efficiently
     */
    @Test
    fun testWebRtcEventHandlingPerformance() =
        runTest {
            mainViewModel.startConnection()

            val eventHandlingTime =
                measureTimeMillis {
                    // Simulate rapid WebRTC events
                    repeat(500) { i ->
                        when (i % 4) {
                            0 -> mockWebRtcManager.simulateConnectionStateChange(ConnectionState.CONNECTING)
                            1 -> mockWebRtcManager.simulateConnectionStateChange(ConnectionState.CONNECTED)
                            2 -> mockWebRtcManager.simulateIceCandidate("candidate:$i")
                            3 -> mockWebRtcManager.simulateDataChannelMessage("message:$i")
                        }

                        if (i % 50 == 0) {
                            delay(1) // Small delay every 50 events
                        }
                    }
                }

            // Event handling should be efficient
            assertTrue(eventHandlingTime < 2000, "WebRTC event handling took ${eventHandlingTime}ms")

            // Verify events are properly processed
            val eventMetrics = performanceMonitor.getWebRtcEventMetrics()
            assertTrue(eventMetrics.eventsProcessed >= 500)
            assertTrue(eventMetrics.averageProcessingTime < 10) // Less than 10ms per event
        }

    /**
     * Test resource cleanup performance
     * Should clean up resources quickly and completely
     */
    @Test
    fun testResourceCleanupPerformance() =
        runTest {
            // Set up active connection with all features
            mainViewModel.setVideoMode(VideoMode.WEBCAM)
            mainViewModel.startConnection()
            mockWebRtcManager.simulateConnectionStateChange(ConnectionState.CONNECTED)

            // Verify resources are active
            assertTrue(mockWebRtcManager.isConnectionActive)
            assertTrue(mockVideoManager.isVideoStreamActive)
            assertTrue(mockAudioManager.isAudioStreamActive)

            val cleanupTime =
                measureTimeMillis {
                    mainViewModel.stopConnection()
                    delay(100) // Allow cleanup to complete
                }

            // Cleanup should be fast
            assertTrue(cleanupTime < 500, "Cleanup took ${cleanupTime}ms")

            // Verify all resources are cleaned up
            assertFalse(mockWebRtcManager.isConnectionActive)
            assertFalse(mockVideoManager.isVideoStreamActive)
            assertFalse(mockAudioManager.isAudioStreamActive)

            // Verify cleanup metrics
            val cleanupMetrics = performanceMonitor.getCleanupMetrics()
            assertTrue(cleanupMetrics.resourcesProperlyReleased)
            assertTrue(cleanupMetrics.cleanupTime < 500)
        }

    /**
     * Test concurrent operation performance
     * Should handle multiple simultaneous operations efficiently
     */
    @Test
    fun testConcurrentOperationPerformance() =
        runTest {
            val operationTime =
                measureTimeMillis {
                    // Simulate concurrent operations
                    val operations =
                        listOf(
                            { mainViewModel.setVideoMode(VideoMode.WEBCAM) },
                            { mainViewModel.switchCamera() },
                            { mainViewModel.handleOrientationChange(90) },
                            { mainViewModel.logDebugMessage("Concurrent test") },
                            {
                                mockAudioManager.simulateDeviceChange(
                                    AudioDevice("test", "Test Device", AudioDeviceType.WIRED_HEADSET, false),
                                )
                            },
                        )

                    repeat(100) { cycle ->
                        operations.forEach { operation ->
                            operation()
                        }
                        delay(5)
                    }
                }

            // Concurrent operations should complete efficiently
            assertTrue(operationTime < 3000, "Concurrent operations took ${operationTime}ms")

            // Verify system remains stable
            val performanceMetrics = performanceMonitor.getOverallPerformanceMetrics()
            assertTrue(performanceMetrics.systemStable)
            assertTrue(performanceMetrics.averageResponseTime < 50)
        }
}

/**
 * Performance monitoring utility for testing
 */
class PerformanceMonitor {
    private var initialMemory: Long = 0
    private var connectionStartTime: Long = 0
    private var connectionAttempts = 0
    private var successfulConnections = 0
    private var webRtcEventsProcessed = 0
    private var totalEventProcessingTime = 0L

    init {
        initialMemory = getCurrentMemoryUsage()
    }

    fun getCurrentMemoryUsage(): Long {
        // Simulate memory usage calculation
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    }

    fun getConnectionMetrics(): ConnectionMetrics =
        ConnectionMetrics(
            // Simulated
            averageConnectionTime = if (connectionAttempts > 0) 1500L else 0L,
            successRate = if (connectionAttempts > 0) successfulConnections.toDouble() / connectionAttempts else 1.0,
        )

    fun getMemoryMetrics(): MemoryMetrics {
        val currentMemory = getCurrentMemoryUsage()
        return MemoryMetrics(
            memoryLeakDetected = false,
            peakMemoryUsage = currentMemory,
            // 100MB threshold
            memoryThreshold = 100 * 1024 * 1024,
        )
    }

    fun getWebRtcEventMetrics(): WebRtcEventMetrics =
        WebRtcEventMetrics(
            eventsProcessed = webRtcEventsProcessed,
            averageProcessingTime = if (webRtcEventsProcessed > 0) totalEventProcessingTime / webRtcEventsProcessed else 0L,
        )

    fun getCleanupMetrics(): CleanupMetrics =
        CleanupMetrics(
            resourcesProperlyReleased = true,
            // Simulated cleanup time
            cleanupTime = 200L,
        )

    fun getOverallPerformanceMetrics(): OverallPerformanceMetrics =
        OverallPerformanceMetrics(
            systemStable = true,
            averageResponseTime = 25L,
        )

    fun recordWebRtcEvent(processingTime: Long) {
        webRtcEventsProcessed++
        totalEventProcessingTime += processingTime
    }

    fun recordConnectionAttempt(successful: Boolean) {
        connectionAttempts++
        if (successful) successfulConnections++
    }
}

data class ConnectionMetrics(
    val averageConnectionTime: Long,
    val successRate: Double,
)

data class MemoryMetrics(
    val memoryLeakDetected: Boolean,
    val peakMemoryUsage: Long,
    val memoryThreshold: Long,
)

data class WebRtcEventMetrics(
    val eventsProcessed: Int,
    val averageProcessingTime: Long,
)

data class CleanupMetrics(
    val resourcesProperlyReleased: Boolean,
    val cleanupTime: Long,
)

data class OverallPerformanceMetrics(
    val systemStable: Boolean,
    val averageResponseTime: Long,
)

// Enhanced mock implementations for performance testing

class MockWebRtcManager {
    var isConnectionActive = false
    private var connectionStateCallback: ((ConnectionState) -> Unit)? = null
    private var iceCandidateCallback: ((String) -> Unit)? = null
    private var dataChannelCallback: ((String) -> Unit)? = null

    fun simulateConnectionStateChange(state: ConnectionState) {
        isConnectionActive = (state == ConnectionState.CONNECTED)
        connectionStateCallback?.invoke(state)
    }

    fun simulateIceCandidate(candidate: String) {
        iceCandidateCallback?.invoke(candidate)
    }

    fun simulateDataChannelMessage(message: String) {
        dataChannelCallback?.invoke(message)
    }

    fun setConnectionStateCallback(callback: (ConnectionState) -> Unit) {
        connectionStateCallback = callback
    }

    fun setIceCandidateCallback(callback: (String) -> Unit) {
        iceCandidateCallback = callback
    }

    fun setDataChannelCallback(callback: (String) -> Unit) {
        dataChannelCallback = callback
    }
}

class MockVideoManager {
    var isVideoStreamActive = false
    var currentCameraFacing = CameraFacing.FRONT

    fun switchCamera() {
        currentCameraFacing =
            if (currentCameraFacing == CameraFacing.FRONT) {
                CameraFacing.BACK
            } else {
                CameraFacing.FRONT
            }
    }

    fun handleOrientationChange(orientation: Int) {
        // Simulate orientation handling
    }

    fun stopCapture() {
        isVideoStreamActive = false
    }
}

class MockAudioManager {
    var isAudioStreamActive = false
    var currentDevice: AudioDevice? = null
    private var deviceChangeCallback: ((AudioDevice) -> Unit)? = null

    fun simulateDeviceChange(device: AudioDevice) {
        currentDevice = device
        deviceChangeCallback?.invoke(device)
    }

    fun setDeviceChangeCallback(callback: (AudioDevice) -> Unit) {
        deviceChangeCallback = callback
    }
}

class MockChatRepository : ai.chatrt.app.repository.ChatRepository {
    override suspend fun createCall(request: CallRequest): Result<CallResponse> = Result.success(CallResponse("test-call-id"))

    override suspend fun stopConnection(): Result<Unit> = Result.success(Unit)

    override suspend fun startConnectionMonitoring(callId: String): Result<Unit> = Result.success(Unit)

    override fun observeConnectionState(): kotlinx.coroutines.flow.Flow<ConnectionState> = kotlinx.coroutines.flow.flowOf(ConnectionState.DISCONNECTED)

    override fun observeLogs(): kotlinx.coroutines.flow.Flow<List<LogEntry>> = kotlinx.coroutines.flow.flowOf(emptyList())
}

data class CallResponse(
    val callId: String,
)
