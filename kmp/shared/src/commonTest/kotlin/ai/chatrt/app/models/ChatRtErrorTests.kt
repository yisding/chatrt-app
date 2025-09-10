package ai.chatrt.app.models

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChatRtErrorTests {
    private val json = Json { prettyPrint = true }

    @Test
    fun testNetworkErrorMessage() {
        val error = ChatRtError.NetworkError
        assertEquals("Network connection error", error.message)
    }

    @Test
    fun testPermissionDeniedMessage() {
        val error = ChatRtError.PermissionDenied
        assertEquals("Required permissions were denied", error.message)
    }

    @Test
    fun testWebRtcErrorMessage() {
        val error = ChatRtError.WebRtcError
        assertEquals("WebRTC connection error", error.message)
    }

    @Test
    fun testAudioDeviceErrorMessage() {
        val error = ChatRtError.AudioDeviceError
        assertEquals("Audio device error", error.message)
    }

    @Test
    fun testCameraErrorMessage() {
        val error = ChatRtError.CameraError
        assertEquals("Camera access error", error.message)
    }

    @Test
    fun testScreenCaptureErrorMessage() {
        val error = ChatRtError.ScreenCaptureError
        assertEquals("Screen capture error", error.message)
    }

    @Test
    fun testServiceConnectionErrorMessage() {
        val error = ChatRtError.ServiceConnectionError
        assertEquals("Service connection error", error.message)
    }

    @Test
    fun testPhoneCallInterruptionErrorMessage() {
        val error = ChatRtError.PhoneCallInterruptionError
        assertEquals("Phone call interruption error", error.message)
    }

    @Test
    fun testBatteryOptimizationErrorMessage() {
        val error = ChatRtError.BatteryOptimizationError
        assertEquals("Battery optimization error", error.message)
    }

    @Test
    fun testNetworkQualityErrorMessage() {
        val error = ChatRtError.NetworkQualityError
        assertEquals("Network quality error", error.message)
    }

    @Test
    fun testApiErrorSerialization() {
        val apiError =
            ChatRtError.ApiError(
                code = 404,
                message = "Resource not found",
            )

        val serialized = json.encodeToString(apiError)
        val deserialized = json.decodeFromString<ChatRtError.ApiError>(serialized)

        assertEquals(apiError.code, deserialized.code)
        assertEquals(apiError.message, deserialized.message)
    }

    @Test
    fun testApiErrorProperties() {
        val apiError =
            ChatRtError.ApiError(
                code = 500,
                message = "Internal server error",
            )

        assertEquals(500, apiError.code)
        assertEquals("Internal server error", apiError.message)
    }

    @Test
    fun testErrorInheritance() {
        val networkError: Exception = ChatRtError.NetworkError
        val apiError: Exception = ChatRtError.ApiError(400, "Bad request")

        assertTrue(networkError is ChatRtError)
        assertTrue(apiError is ChatRtError)
        assertTrue(networkError is Exception)
        assertTrue(apiError is Exception)
    }

    @Test
    fun testErrorMessages() {
        val errors =
            listOf(
                ChatRtError.NetworkError,
                ChatRtError.PermissionDenied,
                ChatRtError.WebRtcError,
                ChatRtError.AudioDeviceError,
                ChatRtError.CameraError,
                ChatRtError.ScreenCaptureError,
                ChatRtError.ServiceConnectionError,
                ChatRtError.PhoneCallInterruptionError,
                ChatRtError.BatteryOptimizationError,
                ChatRtError.NetworkQualityError,
            )

        errors.forEach { error ->
            assertNotNull(error.message)
            assertTrue(error.message!!.isNotEmpty())
        }
    }
}
