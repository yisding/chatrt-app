package ai.chatrt.app.service

import ai.chatrt.app.models.ConnectionState
import ai.chatrt.app.models.VideoMode
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager for ChatRtService connection and lifecycle
 * Requirements: 5.1, 5.2, 5.3
 */
class ChatRtServiceManager(
    private val context: Context,
) {
    private val _isServiceBound = MutableStateFlow(false)
    val isServiceBound: Flow<Boolean> = _isServiceBound.asStateFlow()

    private var chatRtService: ChatRtService? = null
    private var isBound = false

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                val binder = service as ChatRtService.ChatRtBinder
                chatRtService = binder.getService()
                isBound = true
                _isServiceBound.value = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                chatRtService = null
                isBound = false
                _isServiceBound.value = false
            }
        }

    /**
     * Bind to ChatRtService
     */
    fun bindService() {
        if (!isBound) {
            val intent = Intent(context, ChatRtService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    /**
     * Unbind from ChatRtService
     */
    fun unbindService() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
            _isServiceBound.value = false
        }
    }

    /**
     * Start a ChatRT call through the service
     * Requirement: 5.1
     */
    fun startCall(
        videoMode: VideoMode,
        sdpOffer: String?,
    ) {
        val intent =
            Intent(context, ChatRtService::class.java).apply {
                action = ChatRtService.ACTION_START_CALL
                putExtra(ChatRtService.EXTRA_VIDEO_MODE, videoMode)
                putExtra(ChatRtService.EXTRA_SDP_OFFER, sdpOffer)
            }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    /**
     * End the ChatRT call
     * Requirement: 5.1
     */
    fun endCall() {
        val intent =
            Intent(context, ChatRtService::class.java).apply {
                action = ChatRtService.ACTION_END_CALL
            }
        context.startService(intent)
    }

    /**
     * Pause the ChatRT call (e.g., during phone call)
     * Requirement: 5.2
     */
    fun pauseCall() {
        val intent =
            Intent(context, ChatRtService::class.java).apply {
                action = ChatRtService.ACTION_PAUSE_CALL
            }
        context.startService(intent)
    }

    /**
     * Resume the ChatRT call after interruption
     * Requirement: 5.2
     */
    fun resumeCall() {
        val intent =
            Intent(context, ChatRtService::class.java).apply {
                action = ChatRtService.ACTION_RESUME_CALL
            }
        context.startService(intent)
    }

    /**
     * Get service state flows if bound
     */
    fun getServiceStateFlows(): ServiceStateFlows? =
        chatRtService?.let { service ->
            ServiceStateFlows(
                isCallActive = service.isCallActive,
                isCallPaused = service.isCallPaused,
                connectionState = service.connectionState,
            )
        }

    /**
     * Check if call is currently active
     */
    fun isCallActive(): Boolean = chatRtService?.isCallActive?.value ?: false

    /**
     * Check if call is currently paused
     */
    fun isCallPaused(): Boolean = chatRtService?.isCallPaused?.value ?: false

    /**
     * Get current connection state
     */
    fun getConnectionState(): ConnectionState = chatRtService?.connectionState?.value ?: ConnectionState.DISCONNECTED
}

/**
 * Data class for service state flows
 */
data class ServiceStateFlows(
    val isCallActive: Flow<Boolean>,
    val isCallPaused: Flow<Boolean>,
    val connectionState: Flow<ConnectionState>,
)
