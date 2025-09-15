package ai.chatrt.app.service

import ai.chatrt.app.models.AudioConfig
import ai.chatrt.app.models.AudioInputConfig
import ai.chatrt.app.models.AudioOutputConfig
import ai.chatrt.app.models.CallRequest
import ai.chatrt.app.models.ConnectionState
import ai.chatrt.app.models.NoiseReductionConfig
import ai.chatrt.app.models.SessionConfig
import ai.chatrt.app.models.VideoMode
import ai.chatrt.app.platform.AndroidLifecycleManager
import ai.chatrt.app.platform.AndroidWebRtcManager
import ai.chatrt.app.repository.ChatRepository
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Background service for ChatRT call continuation
 * Requirements: 5.1, 5.2, 5.3, 5.6
 */
class ChatRtService : Service() {
    companion object {
        // Service actions
        const val ACTION_START_CALL = "ai.chatrt.app.START_CALL"
        const val ACTION_END_CALL = "ai.chatrt.app.END_CALL"
        const val ACTION_PAUSE_CALL = "ai.chatrt.app.PAUSE_CALL"
        const val ACTION_RESUME_CALL = "ai.chatrt.app.RESUME_CALL"

        // Intent extras
        const val EXTRA_VIDEO_MODE = "video_mode"
        const val EXTRA_SDP_OFFER = "sdp_offer"
    }

    private val binder = ChatRtBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Injected dependencies
    private val chatRepository: ChatRepository by inject()
    private val lifecycleManager: AndroidLifecycleManager by inject()
    private val webRtcManager: AndroidWebRtcManager by inject()

    // Service state
    private val _isCallActive = MutableStateFlow(false)
    val isCallActive: StateFlow<Boolean> = _isCallActive.asStateFlow()

    private val _isCallPaused = MutableStateFlow(false)
    val isCallPaused: StateFlow<Boolean> = _isCallPaused.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var currentVideoMode: VideoMode = VideoMode.AUDIO_ONLY
    private var notificationManager: ChatRtNotificationManager? = null
    private var connectionJob: Job? = null

    inner class ChatRtBinder : Binder() {
        fun getService(): ChatRtService = this@ChatRtService
    }

    override fun onCreate() {
        super.onCreate()

        notificationManager = ChatRtNotificationManager(this)

        // Monitor system interruptions
        serviceScope.launch {
            lifecycleManager.observeSystemInterruptions().collect { interruption ->
                when (interruption.type) {
                    ai.chatrt.app.models.InterruptionType.PHONE_CALL -> {
                        if (interruption.shouldPause) {
                            pauseCall()
                        } else if (interruption.canResume) {
                            resumeCall()
                        }
                    }
                    ai.chatrt.app.models.InterruptionType.LOW_POWER_MODE -> {
                        // Optimize for battery saving
                        optimizeForBattery()
                    }
                    else -> {
                        // Handle other interruptions
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START_CALL -> {
                val videoMode = intent.getSerializableExtra(EXTRA_VIDEO_MODE) as? VideoMode ?: VideoMode.AUDIO_ONLY
                val sdpOffer = intent.getStringExtra(EXTRA_SDP_OFFER)
                startCall(videoMode, sdpOffer)
            }
            ACTION_END_CALL -> {
                endCall()
            }
            ACTION_PAUSE_CALL -> {
                pauseCall()
            }
            ACTION_RESUME_CALL -> {
                resumeCall()
            }
        }

        return START_STICKY // Restart service if killed by system
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up resources
        connectionJob?.cancel()
        serviceScope.cancel()

        // Ensure WebRTC cleanup
        if (_isCallActive.value) {
            serviceScope.launch { webRtcManager.close() }
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    /**
     * Start a ChatRT call in background service
     * Requirement: 5.1
     */
    fun startCall(
        videoMode: VideoMode,
        sdpOffer: String?,
    ) {
        if (_isCallActive.value) return

        currentVideoMode = videoMode
        _isCallActive.value = true
        _connectionState.value = ConnectionState.CONNECTING

        // Start foreground service with notification
        startForegroundService()

        // Start WebRTC connection
        connectionJob =
            serviceScope.launch {
                try {
                    sdpOffer?.let { offer ->
                        val request =
                            CallRequest(
                                sdp = offer,
                                session =
                                    SessionConfig(
                                        instructions = "Background service call",
                                        audio =
                                            AudioConfig(
                                                input = AudioInputConfig(NoiseReductionConfig()),
                                                output = AudioOutputConfig(),
                                            ),
                                    ),
                            )
                        val response = chatRepository.createCall(request)
                        response.fold(
                            onSuccess = {
                                _connectionState.value = ConnectionState.CONNECTED
                                notificationManager?.updateServiceNotification(
                                    ConnectionState.CONNECTED,
                                    currentVideoMode,
                                    _isCallPaused.value,
                                )
                            },
                            onFailure = { error ->
                                _connectionState.value = ConnectionState.FAILED
                                notificationManager?.updateServiceNotification(
                                    ConnectionState.FAILED,
                                    currentVideoMode,
                                    _isCallPaused.value,
                                )
                                endCall()
                            },
                        )
                    }
                } catch (e: Exception) {
                    _connectionState.value = ConnectionState.FAILED
                    notificationManager?.updateServiceNotification(
                        ConnectionState.FAILED,
                        currentVideoMode,
                        _isCallPaused.value,
                    )
                    endCall()
                }
            }
    }

    /**
     * End the ChatRT call and stop service
     * Requirement: 5.1
     */
    fun endCall() {
        if (!_isCallActive.value) return

        _isCallActive.value = false
        _isCallPaused.value = false
        _connectionState.value = ConnectionState.DISCONNECTED

        // Clean up WebRTC connection
        serviceScope.launch { webRtcManager.close() }

        // Cancel connection job
        connectionJob?.cancel()
        connectionJob = null

        // Stop foreground service
        stopForegroundService()

        // Stop service
        stopSelf()
    }

    /**
     * Pause the ChatRT call (e.g., during phone call)
     * Requirement: 5.2
     */
    fun pauseCall() {
        if (!_isCallActive.value || _isCallPaused.value) return

        _isCallPaused.value = true

        // Mute audio/video streams
        webRtcManager.muteAudio(true)
        if (currentVideoMode != VideoMode.AUDIO_ONLY) {
            webRtcManager.muteVideo(true)
        }

        notificationManager?.updateServiceNotification(
            _connectionState.value,
            currentVideoMode,
            isPaused = true,
        )
    }

    /**
     * Resume the ChatRT call after interruption
     * Requirement: 5.2
     */
    fun resumeCall() {
        if (!_isCallActive.value || !_isCallPaused.value) return

        _isCallPaused.value = false

        // Unmute audio/video streams
        webRtcManager.muteAudio(false)
        if (currentVideoMode != VideoMode.AUDIO_ONLY) {
            webRtcManager.muteVideo(false)
        }

        notificationManager?.updateServiceNotification(
            _connectionState.value,
            currentVideoMode,
            isPaused = false,
        )
    }

    /**
     * Optimize service for battery saving
     * Requirement: 5.6
     */
    private fun optimizeForBattery() {
        if (!_isCallActive.value) return

        // Reduce video quality or disable video if battery is low
        when (currentVideoMode) {
            VideoMode.WEBCAM, VideoMode.SCREEN_SHARE -> {
                // Switch to audio-only mode to save battery
                webRtcManager.muteVideo(true)
                notificationManager?.updateServiceNotification(
                    _connectionState.value,
                    // Switched to audio-only for battery saving
                    VideoMode.AUDIO_ONLY,
                    _isCallPaused.value,
                )
            }
            VideoMode.AUDIO_ONLY -> {
                // Already optimized for battery
            }
        }
    }

    /**
     * Start foreground service with notification
     */
    private fun startForegroundService() {
        val notification =
            notificationManager?.createServiceNotification(
                ConnectionState.CONNECTING,
                currentVideoMode,
                isPaused = false,
            )
        notification?.let {
            startForeground(ChatRtNotificationManager.SERVICE_NOTIFICATION_ID, it)
        }
    }

    /**
     * Stop foreground service
     */
    private fun stopForegroundService() {
        notificationManager?.hideServiceNotification()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
