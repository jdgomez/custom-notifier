package dev.jdgomez.customnotifier.alerts

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import dev.jdgomez.customnotifier.CustomNotifierApplication
import dev.jdgomez.customnotifier.domain.DepletionDate
import dev.jdgomez.customnotifier.domain.ProductId
import dev.jdgomez.customnotifier.productlist.testProduct
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AlertAdaptersTest {
    private val app: CustomNotifierApplication = ApplicationProvider.getApplicationContext()
    private val alarmManager = app.getSystemService(AlarmManager::class.java)
    private val notificationManager = app.getSystemService(NotificationManager::class.java)
    private val scheduler = AlarmManagerAlertScheduler(app)
    private val notifier = NotificationAlertNotifier(app)

    private fun alarms() = shadowOf(alarmManager).scheduledAlarms

    private fun awaitUntil(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000L
        while (!condition()) {
            check(System.nanoTime() < deadline) { "condition not met in time" }
            Thread.sleep(20)
        }
    }

    @Test
    fun `schedules one wake-up alarm with a 3 hour window per product and replaces it`() {
        val a = ProductId(UUID.randomUUID())
        val b = ProductId(UUID.randomUUID())
        val first = Instant.parse("2030-03-01T09:00:00Z")
        scheduler.schedule(a, first)
        scheduler.schedule(b, first.plusSeconds(60))
        scheduler.schedule(a, first.plusSeconds(3600))

        assertEquals(
            setOf(first.plusSeconds(3600).toEpochMilli(), first.plusSeconds(60).toEpochMilli()),
            alarms().map { it.triggerAtMs }.toSet(),
        )
        assertEquals(2, alarms().size)
        assertTrue(alarms().all { it.getType() == AlarmManager.RTC_WAKEUP })
        // Inexact with a window that starts at the alert moment and does not depend on how far ahead it is.
        assertTrue(alarms().all { it.getWindowLengthMs() == THREE_HOURS_MS })

        scheduler.cancel(a)
        assertEquals(listOf(first.plusSeconds(60).toEpochMilli()), alarms().map { it.triggerAtMs })
    }

    @Test
    fun `posts the notification with the specified texts on the channel`() {
        val product = testProduct("Vitamin D", Instant.parse("2026-03-01T12:00:00Z"))
        notifier.createChannel()

        val posted = notifier.notify(product, DepletionDate(LocalDate.parse("2026-03-11")), LocalDate.parse("2026-03-05"))

        assertTrue(posted)
        assertEquals("Depletion alerts", notificationManager.getNotificationChannel(NotificationAlertNotifier.CHANNEL_ID).name)
        assertEquals(
            NotificationManager.IMPORTANCE_DEFAULT,
            notificationManager.getNotificationChannel(NotificationAlertNotifier.CHANNEL_ID).importance,
        )
        val notification = shadowOf(notificationManager).getNotification(product.id.value.toString(), 0)
        assertNotNull(notification)
        assertEquals("Buy Vitamin D", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Runs out Mar 11, 2026", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals(NotificationAlertNotifier.CHANNEL_ID, notification.channelId)
        assertTrue(notification.flags and Notification.FLAG_AUTO_CANCEL != 0)
        assertNotNull(notification.contentIntent)
    }

    @Test
    fun `says runs out today on the depletion date and replaces the earlier notification`() {
        val product = testProduct("Vitamin D", Instant.parse("2026-03-01T12:00:00Z"))
        notifier.createChannel()
        val today = LocalDate.parse("2026-03-11")

        notifier.notify(product, DepletionDate(today), today)
        notifier.notify(product, DepletionDate(today), today)

        assertEquals(1, shadowOf(notificationManager).size())
        val text =
            shadowOf(
                notificationManager,
            ).getNotification(product.id.value.toString(), 0).extras.getCharSequence(Notification.EXTRA_TEXT)
        assertEquals("Runs out today", text.toString())
    }

    @Test
    fun `posts nothing while notifications are disabled`() {
        val product = testProduct("Vitamin D", Instant.parse("2026-03-01T12:00:00Z"))
        notifier.createChannel()
        shadowOf(notificationManager).setNotificationsEnabled(false)

        assertFalse(notifier.notify(product, DepletionDate(LocalDate.parse("2026-03-11")), LocalDate.parse("2026-03-05")))
        assertEquals(0, shadowOf(notificationManager).size())
    }

    @Test
    fun `a system broadcast makes the receiver rebuild the alarm`() =
        runBlocking {
            val container = app.container
            val product = testProduct("Vitamin D", Instant.now(), units = 30)
            container.productRepository.save(product)
            awaitUntil { alarms().size == 1 }
            scheduler.cancel(product.id)
            assertTrue(alarms().isEmpty())

            app.sendBroadcast(Intent(Intent.ACTION_TIME_CHANGED))
            shadowOf(Looper.getMainLooper()).idle()

            awaitUntil { alarms().size == 1 }
        }

    @Test
    fun `an alarm for a product that no longer exists posts nothing`() {
        val missing = ProductId(UUID.randomUUID())
        scheduler.schedule(missing, Instant.parse("2030-03-01T09:00:00Z"))

        app.sendBroadcast(
            Intent(app, AlertAlarmReceiver::class.java).setData(android.net.Uri.parse("customnotifier://alert/${missing.value}")),
        )
        shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(300)

        assertEquals(0, shadowOf(notificationManager).size())
    }

    @Test
    fun `the merged manifest has no exact alarm permission`() {
        val info = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
        val requested = info.requestedPermissions.orEmpty().toList()

        assertTrue("android.permission.RECEIVE_BOOT_COMPLETED" in requested)
        assertTrue("android.permission.POST_NOTIFICATIONS" in requested)
        assertNull(requested.firstOrNull { it.endsWith("SCHEDULE_EXACT_ALARM") || it.endsWith("USE_EXACT_ALARM") })
    }

    private companion object {
        const val THREE_HOURS_MS = 3 * 60 * 60 * 1000L
    }
}
