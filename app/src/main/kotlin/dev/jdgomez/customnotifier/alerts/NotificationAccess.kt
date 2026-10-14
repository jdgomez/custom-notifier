package dev.jdgomez.customnotifier.alerts

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.jdgomez.customnotifier.permissions.PermanentDenial

/** Whether alerts can be shown and, when not, why. */
sealed interface NotificationAccessState {
    data object Allowed : NotificationAccessState

    /** The runtime permission (Android 13 and later) is not granted; [canAsk] tells whether Android still shows its dialog. */
    data class PermissionMissing(
        val canAsk: Boolean,
    ) : NotificationAccessState

    data object AppNotificationsOff : NotificationAccessState

    data object ChannelOff : NotificationAccessState
}

/** What the UI needs to know about, and to do for, the notification state of the app. */
interface NotificationAccess {
    fun current(): NotificationAccessState

    /** Records a denied request: [explanationBefore] and [explanationAfter] are the rationale flags around the request. */
    fun recordDenied(
        explanationBefore: Boolean,
        explanationAfter: Boolean,
    )

    /** The system settings screen where the user can fix [state]: the channel's when only it is off, else the app's. */
    fun settingsIntent(state: NotificationAccessState): Intent

    companion object {
        /** `Manifest.permission.POST_NOTIFICATIONS` as a plain string: the constant needs API 33 and is only used from API 33. */
        const val PERMISSION = "android.permission.POST_NOTIFICATIONS"
    }
}

/**
 * Reads the notification state of the app from the platform. Android gives no "permanently denied" signal, so the second
 * denial (an explanation was due before the request and is not after it) is stored in [prefs].
 */
class PlatformNotificationAccess(
    private val context: Context,
) : NotificationAccess {
    private val denial = PermanentDenial(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))

    override fun current(): NotificationAccessState {
        val manager = NotificationManagerCompat.from(context)
        return when {
            needsRuntimePermission() && !hasRuntimePermission() -> {
                NotificationAccessState.PermissionMissing(canAsk = !denial.isRecorded)
            }
            !manager.areNotificationsEnabled() -> NotificationAccessState.AppNotificationsOff
            manager.getNotificationChannel(NotificationAlertNotifier.CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE ->
                NotificationAccessState.ChannelOff
            else -> NotificationAccessState.Allowed.also { denial.clear() }
        }
    }

    override fun recordDenied(
        explanationBefore: Boolean,
        explanationAfter: Boolean,
    ) {
        denial.record(explanationBefore, explanationAfter)
    }

    override fun settingsIntent(state: NotificationAccessState): Intent {
        val intent =
            if (state == NotificationAccessState.ChannelOff) {
                Intent(
                    Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS,
                ).putExtra(Settings.EXTRA_CHANNEL_ID, NotificationAlertNotifier.CHANNEL_ID)
            } else {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            }
        return intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun needsRuntimePermission() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    private fun hasRuntimePermission() =
        ContextCompat.checkSelfPermission(context, NotificationAccess.PERMISSION) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val PREFS = "notification_access"
    }
}
