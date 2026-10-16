package dev.jdgomez.customnotifier.data

import dev.jdgomez.customnotifier.domain.CalendarChannel
import dev.jdgomez.customnotifier.domain.CalendarChannelSettings
import dev.jdgomez.customnotifier.domain.CalendarId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** [CalendarChannelSettings] over Room. */
class RoomCalendarChannelSettings(
    private val dao: CalendarChannelDao,
) : CalendarChannelSettings {
    override fun observe(): Flow<CalendarChannel> =
        dao.observe().map { row -> row?.calendarId?.let { CalendarChannel.On(CalendarId(it)) } ?: CalendarChannel.Off }

    override suspend fun set(channel: CalendarChannel) {
        val id = (channel as? CalendarChannel.On)?.calendarId?.value
        dao.save(CalendarChannelEntity(calendarId = id))
    }
}
