package dev.jdgomez.customnotifier.calendar

import android.content.ContentResolver
import android.database.Cursor
import android.provider.CalendarContract.Calendars
import dev.jdgomez.customnotifier.domain.CalendarGateway
import dev.jdgomez.customnotifier.domain.CalendarId
import dev.jdgomez.customnotifier.domain.DeviceCalendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** [CalendarGateway] over the Calendar Provider: visible, synced calendars the app may write events to. */
class ProviderCalendarGateway(
    private val resolver: ContentResolver,
) : CalendarGateway {
    override suspend fun writableCalendars(): List<DeviceCalendar> = query(WRITABLE, emptyArray())

    override suspend fun find(id: CalendarId): DeviceCalendar? =
        query("$WRITABLE AND ${Calendars._ID} = ?", arrayOf(id.value.toString())).firstOrNull()

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

    private fun Cursor.toCalendars(): List<DeviceCalendar> =
        buildList {
            while (moveToNext()) add(DeviceCalendar(CalendarId(getLong(0)), getString(1).orEmpty(), getString(2).orEmpty()))
        }

    private companion object {
        val PROJECTION = arrayOf(Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME, Calendars.ACCOUNT_NAME)
        const val WRITABLE =
            "${Calendars.VISIBLE} = 1 AND ${Calendars.SYNC_EVENTS} = 1 AND ${Calendars.CALENDAR_ACCESS_LEVEL} >= ${Calendars.CAL_ACCESS_CONTRIBUTOR}"
    }
}
