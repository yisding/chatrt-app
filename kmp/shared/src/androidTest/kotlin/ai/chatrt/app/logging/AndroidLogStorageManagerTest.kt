package ai.chatrt.app.logging

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@RunWith(AndroidJUnit4::class)
class AndroidLogStorageManagerTest {
    private lateinit var context: Context
    private lateinit var storageManager: AndroidLogStorageManager
    private lateinit var testLogsDir: File

    @OptIn(ExperimentalUuidApi::class)
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        storageManager = AndroidLogStorageManager(context)
        testLogsDir = File(context.filesDir, "logs")

        // Clean up any existing test files
        if (testLogsDir.exists()) {
            testLogsDir.deleteRecursively()
        }
    }

    @After
    fun cleanup() {
        if (testLogsDir.exists()) {
            testLogsDir.deleteRecursively()
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun testSaveAndLoadLogs() =
        runTest {
            val testLogs =
                listOf(
                    LogEntry(
                        id = Uuid.random().toString(),
                        timestamp = Clock.System.now(),
                        level = LogLevel.INFO,
                        category = LogCategory.GENERAL,
                        tag = "Test",
                        message = "Test message 1",
                    ),
                    LogEntry(
                        id = Uuid.random().toString(),
                        timestamp = Clock.System.now(),
                        level = LogLevel.ERROR,
                        category = LogCategory.WEBRTC,
                        tag = "WebRTC",
                        message = "Test error message",
                        throwable = RuntimeException("Test exception"),
                    ),
                )

            storageManager.saveLogs(testLogs)
            val loadedLogs = storageManager.loadLogs()

            assertEquals(2, loadedLogs.size)
            assertEquals("Test message 1", loadedLogs[0].message)
            assertEquals("Test error message", loadedLogs[1].message)
            assertEquals(LogLevel.INFO, loadedLogs[0].level)
            assertEquals(LogLevel.ERROR, loadedLogs[1].level)
            assertEquals(LogCategory.GENERAL, loadedLogs[0].category)
            assertEquals(LogCategory.WEBRTC, loadedLogs[1].category)
        }

    @Test
    fun testDeleteLogs() =
        runTest {
            val testLogs =
                listOf(
                    LogEntry(
                        id = "test-1",
                        timestamp = Clock.System.now(),
                        level = LogLevel.INFO,
                        category = LogCategory.GENERAL,
                        tag = "Test",
                        message = "Test message",
                    ),
                )

            storageManager.saveLogs(testLogs)
            assertEquals(1, storageManager.loadLogs().size)

            storageManager.deleteLogs()
            assertEquals(0, storageManager.loadLogs().size)
        }

    @Test
    fun testGetStorageSize() =
        runTest {
            val testLogs =
                listOf(
                    LogEntry(
                        id = "test-1",
                        timestamp = Clock.System.now(),
                        level = LogLevel.INFO,
                        category = LogCategory.GENERAL,
                        tag = "Test",
                        message = "Test message with some content to increase size",
                    ),
                )

            val initialSize = storageManager.getStorageSize()
            storageManager.saveLogs(testLogs)
            val sizeAfterSave = storageManager.getStorageSize()

            assertTrue(sizeAfterSave > initialSize)
        }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun testArchiveLogs() =
        runTest {
            val oldTimestamp = Clock.System.now().minus(kotlinx.datetime.DateTimePeriod(days = 10))
            val newTimestamp = Clock.System.now()

            val testLogs =
                listOf(
                    LogEntry(
                        id = Uuid.random().toString(),
                        timestamp = oldTimestamp,
                        level = LogLevel.INFO,
                        category = LogCategory.GENERAL,
                        tag = "Old",
                        message = "Old log message",
                    ),
                    LogEntry(
                        id = Uuid.random().toString(),
                        timestamp = newTimestamp,
                        level = LogLevel.INFO,
                        category = LogCategory.GENERAL,
                        tag = "New",
                        message = "New log message",
                    ),
                )

            storageManager.saveLogs(testLogs)

            val cutoffTime = Clock.System.now().minus(kotlinx.datetime.DateTimePeriod(days = 5))
            val archiveId = storageManager.archiveLogs(cutoffTime)

            assertTrue(archiveId.isNotEmpty())

            // Check that only new logs remain
            val remainingLogs = storageManager.loadLogs()
            assertEquals(1, remainingLogs.size)
            assertEquals("New log message", remainingLogs[0].message)

            // Check that archive was created
            val archivedLogs = storageManager.getArchivedLogs()
            assertTrue(archivedLogs.contains(archiveId))
        }

    @Test
    fun testDeleteArchivedLogs() =
        runTest {
            val testLogs =
                listOf(
                    LogEntry(
                        id = "test-1",
                        timestamp = Clock.System.now().minus(kotlinx.datetime.DateTimePeriod(days = 10)),
                        level = LogLevel.INFO,
                        category = LogCategory.GENERAL,
                        tag = "Test",
                        message = "Test message",
                    ),
                )

            storageManager.saveLogs(testLogs)

            val cutoffTime = Clock.System.now().minus(kotlinx.datetime.DateTimePeriod(days = 5))
            val archiveId = storageManager.archiveLogs(cutoffTime)

            assertTrue(storageManager.getArchivedLogs().contains(archiveId))

            storageManager.deleteArchivedLogs(archiveId)

            assertFalse(storageManager.getArchivedLogs().contains(archiveId))
        }

    @Test
    fun testEmptyLogsHandling() =
        runTest {
            val loadedLogs = storageManager.loadLogs()
            assertEquals(0, loadedLogs.size)

            val storageSize = storageManager.getStorageSize()
            assertTrue(storageSize >= 0)

            val archivedLogs = storageManager.getArchivedLogs()
            assertEquals(0, archivedLogs.size)
        }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun testLogSerializationWithMetadata() =
        runTest {
            val testLogs =
                listOf(
                    LogEntry(
                        id = Uuid.random().toString(),
                        timestamp = Clock.System.now(),
                        level = LogLevel.DEBUG,
                        category = LogCategory.WEBRTC,
                        tag = "WebRTC",
                        message = "Connection event",
                        metadata =
                            mapOf(
                                "connectionId" to "conn-123",
                                "eventType" to "CONNECTION_CREATED",
                                "details" to mapOf("iceServers" to 2),
                            ),
                    ),
                )

            storageManager.saveLogs(testLogs)
            val loadedLogs = storageManager.loadLogs()

            assertEquals(1, loadedLogs.size)
            val loadedLog = loadedLogs[0]

            assertEquals("Connection event", loadedLog.message)
            assertEquals(LogCategory.WEBRTC, loadedLog.category)
            assertEquals("conn-123", loadedLog.metadata["connectionId"])
            assertEquals("CONNECTION_CREATED", loadedLog.metadata["eventType"])
        }
}
