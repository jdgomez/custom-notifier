package dev.jdgomez.customnotifier.calendar

import dev.jdgomez.customnotifier.domain.CalendarChannel
import dev.jdgomez.customnotifier.domain.CalendarGateway
import dev.jdgomez.customnotifier.domain.DeviceCalendar

/** Why the calendar channel, while on, cannot work. */
enum class CalendarProblem {
    /** The app has no calendar access (read and write). */
    AccessRemoved,

    /** Access is there, but the chosen calendar no longer exists or is no longer writable. */
    CalendarGone,
}

/** What the calendar channel can do right now. */
sealed interface ChannelCheck {
    data object Off : ChannelCheck

    data class Working(
        val calendar: DeviceCalendar,
    ) : ChannelCheck

    data class Broken(
        val problem: CalendarProblem,
    ) : ChannelCheck
}

/** The one rule for "the channel cannot work", shared by the Settings screen and the product list banner. */
suspend fun checkChannel(
    channel: CalendarChannel,
    access: CalendarAccess,
    gateway: CalendarGateway,
): ChannelCheck =
    when {
        channel !is CalendarChannel.On -> ChannelCheck.Off
        access.current() != CalendarAccessState.Granted -> ChannelCheck.Broken(CalendarProblem.AccessRemoved)
        else ->
            gateway.find(channel.calendarId)?.let { ChannelCheck.Working(it) }
                ?: ChannelCheck.Broken(CalendarProblem.CalendarGone)
    }
