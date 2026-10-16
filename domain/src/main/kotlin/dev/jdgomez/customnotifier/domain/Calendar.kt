package dev.jdgomez.customnotifier.domain

import kotlinx.coroutines.flow.Flow

/** The identity of a [DeviceCalendar], as given by the device's calendar provider. */
@JvmInline
value class CalendarId(
    val value: Long,
)

/** A calendar on the device that can receive alert events, shown by [name] under its [accountName]. */
data class DeviceCalendar(
    val id: CalendarId,
    val name: String,
    val accountName: String,
)

/** Whether alerts also go to a calendar: for the whole app, off or on with exactly one chosen calendar. */
sealed interface CalendarChannel {
    data object Off : CalendarChannel

    data class On(
        val calendarId: CalendarId,
    ) : CalendarChannel
}

/** Port to the calendars of the device. Without calendar access every operation behaves as if there were no calendar. */
interface CalendarGateway {
    /** The visible, synced calendars the app can write to, ordered by account then name. */
    suspend fun writableCalendars(): List<DeviceCalendar>

    /** The calendar [id], or null when it is gone or no longer writable. */
    suspend fun find(id: CalendarId): DeviceCalendar?
}

/** Port to the stored calendar channel setting. */
interface CalendarChannelSettings {
    fun observe(): Flow<CalendarChannel>

    suspend fun set(channel: CalendarChannel)
}
