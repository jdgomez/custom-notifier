package dev.jdgomez.customnotifier.alerts

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** [date] in the medium format of the device's current locale: the one date format of notifications and calendar events. */
fun Context.mediumDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(resources.configuration.locales[0]))
