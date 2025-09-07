package ai.chatrt.app.models

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformModelTests {

    private val json = Json { prettyPrint = true }

    @Test
    fun testPlatformOptimizationSerialization() {
        val optimization = PlatformOptimization(
            recommendedVideoMode = VideoMode.AUDIO_ONLY,
            recommendedAudioQuality = AudioQuality.LOW,
            disableVideoPreview = true,
            reason = OptimizationReason.LOW_BATTERY
        )

        val serialized = json.encodeToString(optimization)
        val deserialized = json.decodeFromString<PlatformOptimization>(serialized)

        assertEquals(optimization.recommendedVideoMode, deserialized.recommendedVideoMode)
        assertEquals(optimization.recommendedAudioQuality, deserialized.recommendedAudioQuality)
        assertEquals(optimization.disableVideoPreview, deserialized.disableVideoPreview)
        assertEquals(optimization.reason, deserialized.reason)
    }

    @Test
    fun testPlatformOptimizationDefaults() {
        val optimization = PlatformOptimization(
            recommendedVideoMode = VideoMode.WEBCAM,
            recommendedAudioQuality = AudioQuality.MEDIUM,
            reason = OptimizationReason.POOR_NETWORK
        )

        assertFalse(optimization.disableVideoPreview)
    }

    @Test
    fun testSystemInterruptionSerialization() {
        val interruption = SystemInterruption(
            type = InterruptionType.PHONE_CALL,
            shouldPause = true,
            canResume = false
        )

        val serialized = json.encodeToString(interruption)
        val deserialized = json.decodeFromString<SystemInterruption>(serialized)

        assertEquals(interruption.type, deserialized.type)
        assertEquals(interruption.shouldPause, deserialized.shouldPause)
        assertEquals(interruption.canResume, deserialized.canResume)
    }

    @Test
    fun testSystemInterruptionDefaults() {
        val interruption = SystemInterruption(
            type = InterruptionType.NETWORK_LOSS,
            shouldPause = true
        )

        assertTrue(interruption.canResume)
    }

    @Test
    fun testResourceConstraintsSerialization() {
        val constraints = ResourceConstraints(
            availableMemory = 1024L * 1024L * 512L, // 512MB
            cpuUsage = 75.5f,
            networkBandwidth = 1000000L, // 1Mbps
            platformSpecific = mapOf(
                "batteryLevel" to "25",
                "thermalState" to "normal"
            )
        )

        val serialized = json.encodeToString(constraints)
        val deserialized = json.decodeFromString<ResourceConstraints>(serialized)

        assertEquals(constraints.availableMemory, deserialized.availableMemory)
        assertEquals(constraints.cpuUsage, deserialized.cpuUsage)
        assertEquals(constraints.networkBandwidth, deserialized.networkBandwidth)
        assertEquals(constraints.platformSpecific, deserialized.platformSpecific)
    }

    @Test
    fun testResourceConstraintsDefaults() {
        val constraints = ResourceConstraints(
            availableMemory = 1024L,
            cpuUsage = 50.0f,
            networkBandwidth = 100000L
        )

        assertTrue(constraints.platformSpecific.isEmpty())
    }

    @Test
    fun testPowerSavingRecommendationSerialization() {
        val recommendation = PowerSavingRecommendation(
            mode = PowerSavingMode.MODERATE,
            reason = "Battery level is below 20%",
            estimatedBatteryGain = "30 minutes"
        )

        val serialized = json.encodeToString(recommendation)
        val deserialized = json.decodeFromString<PowerSavingRecommendation>(serialized)

        assertEquals(recommendation.mode, deserialized.mode)
        assertEquals(recommendation.reason, deserialized.reason)
        assertEquals(recommendation.estimatedBatteryGain, deserialized.estimatedBatteryGain)
    }

    @Test
    fun testOptimizationReasonValues() {
        val reasons = OptimizationReason.values()
        
        assertTrue(reasons.contains(OptimizationReason.LOW_BATTERY))
        assertTrue(reasons.contains(OptimizationReason.HIGH_CPU_USAGE))
        assertTrue(reasons.contains(OptimizationReason.LOW_MEMORY))
        assertTrue(reasons.contains(OptimizationReason.POOR_NETWORK))
    }

    @Test
    fun testInterruptionTypeValues() {
        val types = InterruptionType.values()
        
        assertTrue(types.contains(InterruptionType.PHONE_CALL))
        assertTrue(types.contains(InterruptionType.SYSTEM_CALL))
        assertTrue(types.contains(InterruptionType.LOW_POWER_MODE))
        assertTrue(types.contains(InterruptionType.NETWORK_LOSS))
    }

    @Test
    fun testNetworkQualityValues() {
        val qualities = NetworkQuality.values()
        
        assertTrue(qualities.contains(NetworkQuality.POOR))
        assertTrue(qualities.contains(NetworkQuality.FAIR))
        assertTrue(qualities.contains(NetworkQuality.GOOD))
        assertTrue(qualities.contains(NetworkQuality.EXCELLENT))
    }

    @Test
    fun testPowerSavingModeValues() {
        val modes = PowerSavingMode.values()
        
        assertTrue(modes.contains(PowerSavingMode.NONE))
        assertTrue(modes.contains(PowerSavingMode.MODERATE))
        assertTrue(modes.contains(PowerSavingMode.AGGRESSIVE))
    }
}