package dev.jdgomez.customnotifier.settings

import dev.jdgomez.customnotifier.domain.CalendarId
import dev.jdgomez.customnotifier.domain.DeviceCalendar

/** The "Calendar" row shown while the channel is on and calendar access is there. */
sealed interface CalendarRow {
    data class Chosen(
        val calendar: DeviceCalendar,
    ) : CalendarRow

    /** The chosen calendar no longer exists or is no longer writable. */
    data object Unavailable : CalendarRow
}

/** The dialog open on the Settings screen, if any. */
sealed interface SettingsDialog {
    /** Explains why calendar access is needed, before the system dialog. */
    data object Explanation : SettingsDialog

    /** Android no longer shows the system dialog: only the system settings can allow access. */
    data object DeniedForGood : SettingsDialog

    data class Chooser(
        val calendars: List<DeviceCalendar>,
        val selected: CalendarId?,
    ) : SettingsDialog

    data object NoCalendar : SettingsDialog
}

/** What the Settings screen shows. [requestPermission] is a one-shot: the screen shows the system dialog and consumes it. */
data class SettingsState(
    val channelOn: Boolean = false,
    val calendarRow: CalendarRow? = null,
    val accessRemoved: Boolean = false,
    val dialog: SettingsDialog? = null,
    val requestPermission: Boolean = false,
)
