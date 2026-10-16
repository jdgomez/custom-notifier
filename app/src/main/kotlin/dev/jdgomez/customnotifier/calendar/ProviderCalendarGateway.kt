package dev.jdgomez.customnotifier.calendar

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import dev.jdgomez.customnotifier.domain.AlertEvent
import dev.jdgomez.customnotifier.domain.CalendarGateway
import dev.jdgomez.customnotifier.domain.CalendarId
import dev.jdgomez.customnotifier.domain.CalendarUnavailableException
import dev.jdgomez.customnotifier.domain.DeviceCalendar
import dev.jdgomez.customnotifier.domain.EventId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** [CalendarGateway] over the Calendar Provider: visible, synced calendars the app may write events to. */
class ProviderCalendarGateway(
    private val context: Context,
    private val access: CalendarAccess = PlatformCalendarAccess(context),
) : CalendarGateway {
    private val resolver = context.contentResolver

    override suspend fun writableCalendars(): List<DeviceCalendar> = query(WRITABLE, emptyArray())

    override suspend fun find(id: CalendarId): DeviceCalendar? =
        query("$WRITABLE AND ${Calendars._ID} = ?", arrayOf(id.value.toString())).firstOrNull()

    override suspend fun canWrite(): Boolean = access.current() == CalendarAccessState.Granted

    override suspend fun create(
        calendar: CalendarId,
        event: AlertEvent,
    ): EventId? =
        io {
            val values = context.eventValues(event).apply { put(Events.CALENDAR_ID, calendar.value) }
            resolver.insert(Events.CONTENT_URI, values)?.let { EventId(ContentUris.parseId(it)) }
        }

    override suspend fun exists(
        calendar: CalendarId,
        id: EventId,
    ): Boolean = io { present(calendar, id) }

    // An event that is not found is never touched: it may have been deleted by the user.
    override suspend fun update(
        calendar: CalendarId,
        id: EventId,
        event: AlertEvent,
    ): Boolean = io { present(calendar, id) && resolver.update(eventUri(id), context.eventValues(event), null, null) > 0 }

    override suspend fun delete(
        calendar: CalendarId,
        id: EventId,
    ) = io {
        if (present(calendar, id)) resolver.delete(eventUri(id), null, null)
    }

    private fun present(
        calendar: CalendarId,
        id: EventId,
    ): Boolean =
        resolver
            .query(
                Events.CONTENT_URI,
                arrayOf(Events._ID),
                "${Events._ID} = ? AND ${Events.CALENDAR_ID} = ? AND ${Events.DELETED} = 0",
                arrayOf(id.value.toString(), calendar.value.toString()),
                null,
            )?.use { it.moveToFirst() } ?: throw CalendarUnavailableException()

    // A revoked permission surfaces as a SecurityException. It is not "the event is gone": the sync must not touch its records.
    private suspend fun <T> io(block: () -> T): T =
        withContext(Dispatchers.IO) {
            try {
                block()
            } catch (_: SecurityException) {
                throw CalendarUnavailableException()
            }
        }

    // A revoked permission surfaces as a SecurityException: the caller checks access separately, so it means "no calendar".
    private suspend fun query(
        selection: String,
        args: Array<String>,
    ): List<DeviceCalendar> =
        withContext(Dispatchers.IO) {
            try {
                resolver
                    .query(
                        Calendars.CONTENT_URI,
                        PROJECTION,
                        selection,
                        args,
                        "${Calendars.ACCOUNT_NAME}, ${Calendars.CALENDAR_DISPLAY_NAME}",
                    )?.use { it.toCalendars() }
                    .orEmpty()
            } catch (_: SecurityException) {
                emptyList()
            }
        }

    private companion object {
        val PROJECTION = arrayOf(Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME, Calendars.ACCOUNT_NAME)
        const val WRITABLE =
            "${Calendars.VISIBLE} = 1 AND ${Calendars.SYNC_EVENTS} = 1 AND ${Calendars.CALENDAR_ACCESS_LEVEL} >= ${Calendars.CAL_ACCESS_CONTRIBUTOR}"
    }
}
