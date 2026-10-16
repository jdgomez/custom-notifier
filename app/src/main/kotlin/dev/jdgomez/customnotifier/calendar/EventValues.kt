package dev.jdgomez.customnotifier.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.res.Configuration
import android.database.Cursor
import android.provider.CalendarContract.Events
import dev.jdgomez.customnotifier.R
import dev.jdgomez.customnotifier.alerts.mediumDate
import dev.jdgomez.customnotifier.domain.AlertEvent
import dev.jdgomez.customnotifier.domain.CalendarId
import dev.jdgomez.customnotifier.domain.DeviceCalendar
import dev.jdgomez.customnotifier.domain.EventId
import dev.jdgomez.customnotifier.domain.LanguageTag
import java.time.ZoneOffset
import java.util.Locale

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

internal fun eventUri(id: EventId) = ContentUris.withAppendedId(Events.CONTENT_URI, id.value)

internal fun Cursor.toCalendars(): List<DeviceCalendar> =
    buildList {
        while (moveToNext()) add(DeviceCalendar(CalendarId(getLong(0)), getString(1).orEmpty(), getString(2).orEmpty()))
    }

private fun Context.inLanguage(language: LanguageTag): Context =
    createConfigurationContext(Configuration(resources.configuration).apply { setLocale(Locale.forLanguageTag(language.value)) })

/** The provider fields of an alert event. All-day events are UTC midnights with a UTC event time zone, so the date never shifts. */
internal fun Context.eventValues(event: AlertEvent): ContentValues {
    val texts = inLanguage(event.language)
    val start =
        event.date
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    return ContentValues().apply {
        put(Events.TITLE, texts.getString(R.string.alert_title, event.productName.value))
        put(Events.DESCRIPTION, texts.getString(R.string.calendar_event_description, texts.mediumDate(event.depletionDate.date)))
        put(Events.ALL_DAY, 1)
        put(Events.DTSTART, start)
        put(Events.DTEND, start + DAY_MILLIS)
        put(Events.EVENT_TIMEZONE, "UTC")
        put(Events.HAS_ALARM, 0)
    }
}
