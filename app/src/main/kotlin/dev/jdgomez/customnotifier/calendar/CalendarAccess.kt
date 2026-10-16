package dev.jdgomez.customnotifier.calendar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Whether the app may read and write calendars: both permissions are needed, a partial grant is [Missing]. */
sealed interface CalendarAccessState {
    data object Granted : CalendarAccessState

    data object Missing : CalendarAccessState
}

/** What the UI needs to know about, and to do for, the calendar permissions of the app. */
interface CalendarAccess {
    fun current(): CalendarAccessState

    /** The app's system settings screen, where the user can allow calendar access again. */
    fun settingsIntent(): Intent

    companion object {
        val PERMISSIONS = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    }
}

/** [CalendarAccess] from the platform. Nothing about denials is stored: the platform is read every time. */
class PlatformCalendarAccess(
    private val context: Context,
) : CalendarAccess {
    override fun current(): CalendarAccessState {
        val granted =
            CalendarAccess.PERMISSIONS.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        return if (granted) CalendarAccessState.Granted else CalendarAccessState.Missing
    }

    override fun settingsIntent(): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
