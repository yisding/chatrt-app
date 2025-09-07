package ai.chatrt.app.models

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DataModelTests {

    private val json = Json { prettyPrint = true }

    @Test
    fun testLogEntrySerialization() {
        val logEntry = LogEntry(
            timestamp = 1234567890L,
            message = "Test message",
            level = LogLevel.INFO
        )

        val serialized = json.encodeToString(logEntry)
        val deserialized = json.decodeFromString<LogEntry>(serialized)

        assertEquals(logEntry.timestamp, deserialized.timestamp)
        assertEquals(logEntry.message, deserialized.message)
        assertEquals(logEntry.level, deserialized.level)
    }

    @Test
    fun testLogEntryDefaultLevel() {
        val logEntry = LogEntry(
            timestamp = 1234567890L,
            message = "Test message"
        )

        assertEquals(LogLevel.INFO, logEntry.level)
    }

    @Test
    fun testAppSettingsSerialization() {
        val appSettings = AppSettings(
            defaultVideoMode = VideoMode.WEBCAM,
            audioQuality = AudioQuality.HIGH,
            debugLogging = true,
            serverUrl = "https://example.com",
            defaultCamera = CameraFacing.BACK
        )

        val serialized = json.encodeToString(appSettings)
        val deserialized = json.decodeFromString<AppSettings>(serialized)

        assertEquals(appSettings.defaultVideoMode, deserialized.defaultVideoMode)
        assertEquals(appSettings.audioQuality, deserialized.audioQuality)
        assertEquals(appSettings.debugLogging, deserialized.debugLogging)
        assertEquals(appSettings.serverUrl, deserialized.serverUrl)
        assertEquals(appSettings.defaultCamera, deserialized.defaultCamera)
    }

    @Test
    fun testAppSettingsDefaults() {
        val appSettings = AppSettings()

        assertEquals(VideoMode.AUDIO_ONLY, appSettings.defaultVideoMode)
        assertEquals(AudioQuality.MEDIUM, appSettings.audioQuality)
        assertEquals(false, appSettings.debugLogging)
        assertEquals("", appSettings.serverUrl)
        assertEquals(CameraFacing.FRONT, appSettings.defaultCamera)
    }

    @Test
    fun testSessionConfigSerialization() {
        val sessionConfig = SessionConfig(
            type = "realtime",
            model = "gpt-realtime",
            instructions = "Test instructions",
            audio = AudioConfig(
                input = AudioInputConfig(
                    noiseReduction = NoiseReductionConfig(type = "near_field")
                ),
                output = AudioOutputConfig(voice = "marin")
            )
        )

        val serialized = json.encodeToString(sessionConfig)
        val deserialized = json.decodeFromString<SessionConfig>(serialized)

        assertEquals(sessionConfig.type, deserialized.type)
        assertEquals(sessionConfig.model, deserialized.model)
        assertEquals(sessionConfig.instructions, deserialized.instructions)
        assertEquals(sessionConfig.audio.input.noiseReduction.type, deserialized.audio.input.noiseReduction.type)
        assertEquals(sessionConfig.audio.output.voice, deserialized.audio.output.voice)
    }

    @Test
    fun testCallRequestSerialization() {
        val callRequest = CallRequest(
            sdp = "test-sdp-offer",
            session = SessionConfig(
                instructions = "Test instructions",
                audio = AudioConfig(
                    input = AudioInputConfig(
                        noiseReduction = NoiseReductionConfig()
                    ),
                    output = AudioOutputConfig()
                )
            )
        )

        val serialized = json.encodeToString(callRequest)
        val deserialized = json.decodeFromString<CallRequest>(serialized)

        assertEquals(callRequest.sdp, deserialized.sdp)
        assertEquals(callRequest.session.instructions, deserialized.session.instructions)
    }

    @Test
    fun testCallResponseSerialization() {
        val callResponse = CallResponse(
            callId = "test-call-id",
            sdpAnswer = "test-sdp-answer",
            status = "connected"
        )

        val serialized = json.encodeToString(callResponse)
        val deserialized = json.decodeFromString<CallResponse>(serialized)

        assertEquals(callResponse.callId, deserialized.callId)
        assertEquals(callResponse.sdpAnswer, deserialized.sdpAnswer)
        assertEquals(callResponse.status, deserialized.status)
    }

    @Test
    fun testConnectionParamsSerialization() {
        val connectionParams = ConnectionParams(
            videoMode = VideoMode.SCREEN_SHARE,
            audioQuality = AudioQuality.HIGH,
            cameraFacing = CameraFacing.BACK
        )

        val serialized = json.encodeToString(connectionParams)
        val deserialized = json.decodeFromString<ConnectionParams>(serialized)

        assertEquals(connectionParams.videoMode, deserialized.videoMode)
        assertEquals(connectionParams.audioQuality, deserialized.audioQuality)
        assertEquals(connectionParams.cameraFacing, deserialized.cameraFacing)
    }

    @Test
    fun testConnectionParamsDefaults() {
        val connectionParams = ConnectionParams(
            videoMode = VideoMode.AUDIO_ONLY,
            audioQuality = AudioQuality.MEDIUM
        )

        assertEquals(CameraFacing.FRONT, connectionParams.cameraFacing)
    }
}