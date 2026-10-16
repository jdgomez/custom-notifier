package dev.jdgomez.customnotifier

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Reminders
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.jdgomez.customnotifier.domain.CalendarChannel
import dev.jdgomez.customnotifier.domain.CalendarId
import dev.jdgomez.customnotifier.domain.ConsumptionRate
import dev.jdgomez.customnotifier.domain.LeadTime
import dev.jdgomez.customnotifier.domain.PackageSize
import dev.jdgomez.customnotifier.domain.Product
import dev.jdgomez.customnotifier.domain.ProductId
import dev.jdgomez.customnotifier.domain.ProductName
import dev.jdgomez.customnotifier.domain.Quantity
import dev.jdgomez.customnotifier.domain.Stock
import dev.jdgomez.customnotifier.domain.UnitLabel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import dev.jdgomez.customnotifier.domain.Rule as AlertRule

/** The alert events written to a calendar created by the test (a local account), read back through the Calendar Provider. */
@RunWith(AndroidJUnit4::class)
class CalendarEventSyncTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val application = ApplicationProvider.getApplicationContext<CustomNotifierApplication>()
    private val container = application.container
    private val resolver = application.contentResolver
    private val zone = ZoneId.systemDefault()
    private var calendar = 0L

    @Before
    fun prepare() {
        CALENDAR_PERMISSIONS.forEach { instrumentation.uiAutomation.grantRuntimePermission(application.packageName, it) }
        assumeTrue("the device already has a writable calendar", !deviceHasWritableCalendar())
        calendar = createCalendar()
    }

    @After
    fun removeTestCalendar() {
        runBlocking { container.calendarChannelSettings.set(CalendarChannel.Off) }
        withShellPermissions { resolver.delete(syncAdapterUri(), "${Calendars.ACCOUNT_NAME} = ?", arrayOf(ACCOUNT)) }
    }

    /** Runs out in 30 days with a 10 day lead time: the alert is in 20 days. */
    private fun seed(name: String) =
        Product(
            ProductId(UUID.randomUUID()),
            ProductName(name),
            UnitLabel("pills"),
            PackageSize(30),
            ConsumptionRate(units = 1, days = 1),
            Stock(Quantity.of(30), Instant.now()),
            AlertRule(LeadTime(10)),
        ).also { runBlocking { container.productRepository.save(it) } }

    private fun turnOn() = runBlocking { container.calendarChannelSettings.set(CalendarChannel.On(CalendarId(calendar))) }

    private class Row(
        val id: Long,
        val title: String,
        val description: String,
        val allDay: Int,
        val start: Long,
        val hasAlarm: Int,
    )

    private fun events(): List<Row> =
        resolver
            .query(
                Events.CONTENT_URI,
                arrayOf(Events._ID, Events.TITLE, Events.DESCRIPTION, Events.ALL_DAY, Events.DTSTART, Events.HAS_ALARM),
                "${Events.CALENDAR_ID} = ? AND ${Events.DELETED} = 0",
                arrayOf(calendar.toString()),
                Events.TITLE,
            )!!
            .use { c ->
                buildList {
                    while (c.moveToNext()) {
                        add(
                            Row(c.getLong(0), c.getString(1), c.getString(2).orEmpty(), c.getInt(3), c.getLong(4), c.getInt(5)),
                        )
                    }
                }
            }

    private fun titles() = events().map { it.title }

    private fun awaitTitles(vararg expected: String) {
        val deadline = System.currentTimeMillis() + UI_TIMEOUT_MS
        while (titles() != expected.toList() && System.currentTimeMillis() < deadline) Thread.sleep(POLL_MS)
        assertEquals(expected.toList(), titles())
    }

    @Test
    fun turningOnCreatesAllDayEventsOnTheAlertDateWithoutReminders() {
        val product = seed("Coffee")
        seed("Rice")
        turnOn()
        awaitTitles("Buy Coffee", "Buy Rice")

        val coffee = events().first { it.title == "Buy Coffee" }
        val alertDate: LocalDate = product.depletionDateIn(zone).date.minusDays(10)
        assertEquals(1, coffee.allDay)
        assertEquals(alertDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), coffee.start)
        assertEquals(0, coffee.hasAlarm)
        assertTrue(coffee.description.endsWith("Added by Custom Notifier."))
        val reminders =
            resolver.query(
                Reminders.CONTENT_URI,
                arrayOf(Reminders._ID),
                "${Reminders.EVENT_ID} = ?",
                arrayOf(coffee.id.toString()),
                null,
            )!!
        assertEquals(0, reminders.use { it.count })
    }

    @Test
    fun restockMovesTheEventAndAnEventDeletedOutsideComesBackOnlyAfterAProductChange() {
        val coffee = seed("Coffee")
        turnOn()
        awaitTitles("Buy Coffee")
        val first = events().single()

        runBlocking { container.productRepository.save(coffee.restock(1, Instant.now())) }
        val deadline = System.currentTimeMillis() + UI_TIMEOUT_MS
        while (events().singleOrNull()?.start == first.start && System.currentTimeMillis() < deadline) Thread.sleep(POLL_MS)
        assertEquals(1, events().size)
        assertTrue(events().single().start > first.start)

        resolver.delete(ContentUris.withAppendedId(Events.CONTENT_URI, events().single().id), null, null)
        container.syncCalendar()
        Thread.sleep(SETTLE_MS)
        assertTrue(events().isEmpty())

        runBlocking { container.productRepository.save(coffee.restock(2, Instant.now())) }
        awaitTitles("Buy Coffee")
    }

    @Test
    fun turningOffDeletesEveryEvent() {
        seed("Coffee")
        seed("Rice")
        turnOn()
        awaitTitles("Buy Coffee", "Buy Rice")
        runBlocking { container.calendarChannelSettings.set(CalendarChannel.Off) }
        awaitTitles()
    }

    private fun createCalendar(): Long =
        withShellPermissions {
            val values =
                ContentValues().apply {
                    put(Calendars.ACCOUNT_NAME, ACCOUNT)
                    put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                    put(Calendars.NAME, "Events test")
                    put(Calendars.CALENDAR_DISPLAY_NAME, "Events test")
                    put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER)
                    put(Calendars.OWNER_ACCOUNT, ACCOUNT)
                    put(Calendars.VISIBLE, 1)
                    put(Calendars.SYNC_EVENTS, 1)
                }
            assertNotNull(resolver.insert(syncAdapterUri(), values)).lastPathSegment!!.toLong()
        }

    private fun deviceHasWritableCalendar() =
        withShellPermissions {
            resolver
                .query(
                    Calendars.CONTENT_URI,
                    arrayOf(Calendars._ID),
                    "${Calendars.CALENDAR_ACCESS_LEVEL} >= ${Calendars.CAL_ACCESS_CONTRIBUTOR}",
                    null,
                    null,
                )?.use { it.count > 0 } == true
        }

    private fun <T> withShellPermissions(block: () -> T): T {
        val automation = instrumentation.uiAutomation
        automation.adoptShellPermissionIdentity(*CALENDAR_PERMISSIONS)
        try {
            return block()
        } finally {
            automation.dropShellPermissionIdentity()
        }
    }

    private fun syncAdapterUri(): Uri =
        Calendars.CONTENT_URI
            .buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(Calendars.ACCOUNT_NAME, ACCOUNT)
            .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()

    private companion object {
        const val ACCOUNT = "event-test@example.com"
        const val POLL_MS = 250L
        const val SETTLE_MS = 2_000L
        val CALENDAR_PERMISSIONS = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    }
}
