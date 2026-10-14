package dev.jdgomez.customnotifier.calendar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.edit

/** Whether the app may read and write calendars and, when not, whether Android still shows its dialog. */
sealed interface CalendarAccessState {
    data object Granted : CalendarAccessState

    data class Missing(
        val canAsk: Boolean,
    ) : CalendarAccessState
}

/** What the UI needs to know about, and to do for, the calendar permissions of the app. */
interface CalendarAccess {
    fun current(): CalendarAccessState

    /** Records a denied request: [explanationBefore] and [explanationAfter] are the rationale flags around the request. */
    fun recordDenied(
        explanationBefore: Boolean,
        explanationAfter: Boolean,
    )

    /** The app's system settings screen, where the user can allow calendar access again. */
    fun settingsIntent(): Intent

    companion object {
        val PERMISSIONS = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    }
}

/** [CalendarAccess] from the platform; the permanent denial rule is the one of the notification permission. */
class PlatformCalendarAccess(
    private val context: Context,
) : CalendarAccess {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun current(): CalendarAccessState {
        val granted =
            CalendarAccess.PERMISSIONS.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        if (!granted) return CalendarAccessState.Missing(canAsk = !prefs.getBoolean(KEY_PERMANENTLY_DENIED, false))
        prefs.edit { remove(KEY_PERMANENTLY_DENIED) }
        return CalendarAccessState.Granted
    }

    override fun recordDenied(
        explanationBefore: Boolean,
        explanationAfter: Boolean,
    ) {
        if (explanationBefore && !explanationAfter) prefs.edit { putBoolean(KEY_PERMANENTLY_DENIED, true) }
    }

    override fun settingsIntent(): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private companion object {
        const val PREFS = "calendar_access"
        const val KEY_PERMANENTLY_DENIED = "permanently_denied"
    }
}
