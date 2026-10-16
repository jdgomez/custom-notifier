package dev.jdgomez.customnotifier.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

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

/** The identity of an event, as given by the device's calendar provider. */
@JvmInline
value class EventId(
    val value: Long,
)

/** A language as a BCP 47 tag, for example "en" or "es-MX". */
@JvmInline
value class LanguageTag(
    val value: String,
)

/**
 * The content wanted in the calendar for one product: an all-day event on [date], the product's alert date, with its texts
 * written in [language].
 */
data class AlertEvent(
    val productName: ProductName,
    val depletionDate: DepletionDate,
    val date: LocalDate,
    val language: LanguageTag,
)

/** Whether alerts also go to a calendar: for the whole app, off or on with exactly one chosen calendar. */
sealed interface CalendarChannel {
    data object Off : CalendarChannel

    data class On(
        val calendarId: CalendarId,
    ) : CalendarChannel
}

/** Thrown by the event operations of [CalendarGateway] when calendar access is lost or the provider is not available. */
class CalendarUnavailableException : RuntimeException("calendar access or provider unavailable")

/** Port to the calendars of the device. Without calendar access every operation behaves as if there were no calendar. */
interface CalendarGateway {
    /** The visible, synced calendars the app can write to, ordered by account then name. */
    suspend fun writableCalendars(): List<DeviceCalendar>

    /** The calendar [id], or null when it is gone or no longer writable. */
    suspend fun find(id: CalendarId): DeviceCalendar?

    /** Whether calendar access is granted, so that the event operations can work. */
    suspend fun canWrite(): Boolean

    /** Creates the all-day event for [event] in [calendar]; null when the provider did not create it; throws [CalendarUnavailableException] without access. */
    suspend fun create(
        calendar: CalendarId,
        event: AlertEvent,
    ): EventId?

    /** Whether the event [id] still exists, not deleted, in [calendar]; throws [CalendarUnavailableException] when it cannot tell. */
    suspend fun exists(
        calendar: CalendarId,
        id: EventId,
    ): Boolean

    /** Rewrites the event [id] to [event]; false, touching nothing, when it is gone; throws [CalendarUnavailableException] when it cannot tell. */
    suspend fun update(
        calendar: CalendarId,
        id: EventId,
        event: AlertEvent,
    ): Boolean

    /** Deletes the event [id] from [calendar]; does nothing when it is gone; throws [CalendarUnavailableException] when it cannot tell. */
    suspend fun delete(
        calendar: CalendarId,
        id: EventId,
    )
}

/** Port to the stored calendar channel setting. */
interface CalendarChannelSettings {
    fun observe(): Flow<CalendarChannel>

    suspend fun set(channel: CalendarChannel)
}
