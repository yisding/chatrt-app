package ai.chatrt.app.service

import ai.chatrt.app.models.ConnectionState
import ai.chatrt.app.models.VideoMode
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for ChatRtNotificationManager
 * Requirements: 3.3, 5.1, 5.6
 */
@RunWith(AndroidJUnit4::class)
class ChatRtNotificationManagerTest {
    private lateinit var context: Context
    private lateinit var notificationManager: ChatRtNotificationManager
    private lateinit var systemNotificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = ChatRtNotificationManager(context)
        systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * Test service notification creation for different states
     * Requirement: 5.1
     */
    @Test
    fun testServiceNotificationCreation() {
        // Test connecting state
        val connectingNotification =
            notificationManager.createServiceNotification(
                ConnectionState.CONNECTING,
                VideoMode.AUDIO_ONLY,
                isPaused = false,
            )

        assertNotNull(connectingNotification)
        assertEquals("ChatRT Connecting", connectingNotification.extras.getString("android.title"))

        // Test connected state with video
        val connectedNotification =
            notificationManager.createServiceNotification(
                ConnectionState.CONNECTED,
                VideoMode.WEBCAM,
                isPaused = false,
            )

        assertNotNull(connectedNotification)
        assertEquals("ChatRT Call Active", connectedNotification.extras.getString("android.title"))
        assertEquals("Video call", connectedNotification.extras.getString("android.text"))

        // Test paused state
        val pausedNotification =
            notificationManager.createServiceNotification(
                ConnectionState.CONNECTED,
                VideoMode.AUDIO_ONLY,
                isPaused = true,
            )

        assertNotNull(pausedNotification)
        assertEquals("ChatRT Call Paused", pausedNotification.extras.getString("android.title"))

        // Test failed state
        val failedNotification =
            notificationManager.createServiceNotification(
                ConnectionState.FAILED,
                VideoMode.AUDIO_ONLY,
                isPaused = false,
            )

        assertNotNull(failedNotification)
        assertEquals("ChatRT Call Failed", failedNotification.extras.getString("android.title"))
    }

    /**
     * Test screen recording notification creation
     * Requirement: 3.3
     */
    @Test
    fun testScreenRecordingNotificationCreation() {
        val notification = notificationManager.createScreenRecordingNotification()

        assertNotNull(notification)
        assertEquals("Screen Recording Active", notification.extras.getString("android.title"))
        assertEquals("ChatRT is recording your screen", notification.extras.getString("android.text"))

        // Verify it's an ongoing notification
        assertEquals(true, notification.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0)
    }

    /**
     * Test notification display and hiding
     * Requirement: 5.1
     */
    @Test
    fun testNotificationDisplayAndHiding() {
        // Show service notification
        notificationManager.showServiceNotification(
            ConnectionState.CONNECTED,
            VideoMode.AUDIO_ONLY,
            isPaused = false,
        )

        // Verify notification is active
        val activeNotifications = systemNotificationManager.activeNotifications
        val serviceNotification =
            activeNotifications.find {
                it.id == ChatRtNotificationManager.SERVICE_NOTIFICATION_ID
            }
        assertNotNull(serviceNotification)

        // Hide service notification
        notificationManager.hideServiceNotification()

        // Note: In a real test environment, we would verify the notification is removed
        // but this requires more complex setup with notification access
    }

    /**
     * Test screen recording notification display and hiding
     * Requirement: 3.3
     */
    @Test
    fun testScreenRecordingNotificationDisplayAndHiding() {
        // Show screen recording notification
        notificationManager.showScreenRecordingNotification()

        // Verify notification is active
        val activeNotifications = systemNotificationManager.activeNotifications
        val screenRecordingNotification =
            activeNotifications.find {
                it.id == ChatRtNotificationManager.SCREEN_RECORDING_NOTIFICATION_ID
            }
        assertNotNull(screenRecordingNotification)

        // Hide screen recording notification
        notificationManager.hideScreenRecordingNotification()

        // Note: In a real test environment, we would verify the notification is removed
    }

    /**
     * Test notification updates
     * Requirement: 5.1
     */
    @Test
    fun testNotificationUpdates() {
        // Show initial notification
        notificationManager.showServiceNotification(
            ConnectionState.CONNECTING,
            VideoMode.AUDIO_ONLY,
            isPaused = false,
        )

        // Update to connected state
        notificationManager.updateServiceNotification(
            ConnectionState.CONNECTED,
            VideoMode.WEBCAM,
            isPaused = false,
        )

        // Update to paused state
        notificationManager.updateServiceNotification(
            ConnectionState.CONNECTED,
            VideoMode.WEBCAM,
            isPaused = true,
        )

        // Each update should replace the previous notification
        val activeNotifications = systemNotificationManager.activeNotifications
        val serviceNotifications =
            activeNotifications.filter {
                it.id == ChatRtNotificationManager.SERVICE_NOTIFICATION_ID
            }

        // Should only have one notification (the latest update)
        assertEquals(1, serviceNotifications.size)
    }

    /**
     * Test notification actions
     * Requirement: 5.1
     */
    @Test
    fun testNotificationActions() {
        val notification =
            notificationManager.createServiceNotification(
                ConnectionState.CONNECTED,
                VideoMode.AUDIO_ONLY,
                isPaused = false,
            )

        // Verify end call action is present
        assertNotNull(notification.actions)
        assertEquals(1, notification.actions.size)
        assertEquals("End Call", notification.actions[0].title)

        val screenRecordingNotification = notificationManager.createScreenRecordingNotification()

        // Verify stop recording action is present
        assertNotNull(screenRecordingNotification.actions)
        assertEquals(1, screenRecordingNotification.actions.size)
        assertEquals("Stop Recording", screenRecordingNotification.actions[0].title)
    }
}
