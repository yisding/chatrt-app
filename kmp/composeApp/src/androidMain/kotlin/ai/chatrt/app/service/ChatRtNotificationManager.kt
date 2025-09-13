package ai.chatrt.app.service

import ai.chatrt.app.models.ConnectionState
import ai.chatrt.app.models.VideoMode
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Notification manager for ChatRT service and screen recording
 * Requirements: 3.3, 5.1, 5.6
 */
class ChatRtNotificationManager(
    private val context: Context,
) {
    companion object {
        // Notification IDs
        const val SERVICE_NOTIFICATION_ID = 1001
        const val SCREEN_RECORDING_NOTIFICATION_ID = 1002

        // Notification channels
        const val SERVICE_CHANNEL_ID = "chatrt_service_channel"
        const val SERVICE_CHANNEL_NAME = "ChatRT Service"
        const val SCREEN_RECORDING_CHANNEL_ID = "chatrt_screen_recording_channel"
        const val SCREEN_RECORDING_CHANNEL_NAME = "Screen Recording"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    /**
     * Create notification channels for Android O+
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Service notification channel
            val serviceChannel =
                NotificationChannel(
                    SERVICE_CHANNEL_ID,
                    SERVICE_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Notifications for ChatRT background service"
                    setShowBadge(false)
                    enableLights(false)
                    enableVibration(false)
                }

            // Screen recording notification channel
            val screenRecordingChannel =
                NotificationChannel(
                    SCREEN_RECORDING_CHANNEL_ID,
                    SCREEN_RECORDING_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Notifications for active screen recording"
                    setShowBadge(true)
                    enableLights(true)
                    enableVibration(false)
                }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(screenRecordingChannel)
        }
    }

    /**
     * Create service notification for call status
     * Requirement: 5.1
     */
    fun createServiceNotification(
        connectionState: ConnectionState,
        videoMode: VideoMode,
        isPaused: Boolean = false,
    ): Notification {
        val intent =
            Intent(context, ai.chatrt.app.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val (title, message, icon) = getServiceNotificationContent(connectionState, videoMode, isPaused)

        return NotificationCompat
            .Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(createEndCallAction())
            .build()
    }

    /**
     * Create screen recording notification
     * Requirement: 3.3
     */
    fun createScreenRecordingNotification(): Notification {
        val intent =
            Intent(context, ai.chatrt.app.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat
            .Builder(context, SCREEN_RECORDING_CHANNEL_ID)
            .setContentTitle("Screen Recording Active")
            .setContentText("ChatRT is recording your screen")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFFFF5722.toInt()) // Orange color for visibility
            .addAction(createStopScreenRecordingAction())
            .build()
    }

    /**
     * Show service notification
     */
    fun showServiceNotification(
        connectionState: ConnectionState,
        videoMode: VideoMode,
        isPaused: Boolean = false,
    ) {
        val notification = createServiceNotification(connectionState, videoMode, isPaused)
        notificationManager.notify(SERVICE_NOTIFICATION_ID, notification)
    }

    /**
     * Show screen recording notification
     * Requirement: 3.3
     */
    fun showScreenRecordingNotification() {
        val notification = createScreenRecordingNotification()
        notificationManager.notify(SCREEN_RECORDING_NOTIFICATION_ID, notification)
    }

    /**
     * Hide service notification
     */
    fun hideServiceNotification() {
        notificationManager.cancel(SERVICE_NOTIFICATION_ID)
    }

    /**
     * Hide screen recording notification
     * Requirement: 3.3
     */
    fun hideScreenRecordingNotification() {
        notificationManager.cancel(SCREEN_RECORDING_NOTIFICATION_ID)
    }

    /**
     * Update service notification
     */
    fun updateServiceNotification(
        connectionState: ConnectionState,
        videoMode: VideoMode,
        isPaused: Boolean = false,
    ) {
        showServiceNotification(connectionState, videoMode, isPaused)
    }

    /**
     * Get notification content based on state
     */
    private fun getServiceNotificationContent(
        connectionState: ConnectionState,
        videoMode: VideoMode,
        isPaused: Boolean,
    ): Triple<String, String, Int> =
        when {
            isPaused ->
                Triple(
                    "ChatRT Call Paused",
                    "Call paused due to interruption",
                    android.R.drawable.ic_media_pause,
                )
            connectionState == ConnectionState.CONNECTED -> {
                val modeText =
                    when (videoMode) {
                        VideoMode.AUDIO_ONLY -> "Voice call"
                        VideoMode.WEBCAM -> "Video call"
                        VideoMode.SCREEN_SHARE -> "Screen sharing"
                    }
                Triple(
                    "ChatRT Call Active",
                    modeText,
                    android.R.drawable.ic_menu_call,
                )
            }
            connectionState == ConnectionState.CONNECTING ->
                Triple(
                    "ChatRT Connecting",
                    "Establishing connection...",
                    android.R.drawable.ic_popup_sync,
                )
            connectionState == ConnectionState.FAILED ->
                Triple(
                    "ChatRT Call Failed",
                    "Connection failed",
                    android.R.drawable.ic_dialog_alert,
                )
            else ->
                Triple(
                    "ChatRT",
                    "Ready to connect",
                    android.R.drawable.ic_menu_info_details,
                )
        }

    /**
     * Create end call action for notification
     */
    private fun createEndCallAction(): NotificationCompat.Action {
        val endCallIntent =
            Intent(context, ChatRtService::class.java).apply {
                action = ChatRtService.ACTION_END_CALL
            }

        val endCallPendingIntent =
            PendingIntent.getService(
                context,
                0,
                endCallIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat.Action
            .Builder(
                android.R.drawable.ic_menu_close_clear_cancel,
                "End Call",
                endCallPendingIntent,
            ).build()
    }

    /**
     * Create stop screen recording action for notification
     */
    private fun createStopScreenRecordingAction(): NotificationCompat.Action {
        val stopIntent =
            Intent(context, ChatRtService::class.java).apply {
                action = ChatRtService.ACTION_END_CALL
            }

        val stopPendingIntent =
            PendingIntent.getService(
                context,
                0,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat.Action
            .Builder(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Recording",
                stopPendingIntent,
            ).build()
    }
}
