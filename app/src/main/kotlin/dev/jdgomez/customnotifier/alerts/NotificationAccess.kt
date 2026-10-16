package dev.jdgomez.customnotifier.alerts

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Whether alerts can be shown and, when not, why. */
sealed interface NotificationAccessState {
    data object Allowed : NotificationAccessState

    /** The runtime permission (Android 13 and later) is not granted. */
    data object PermissionMissing : NotificationAccessState

    data object AppNotificationsOff : NotificationAccessState

    data object ChannelOff : NotificationAccessState
}

/** What the UI needs to know about, and to do for, the notification state of the app. */
interface NotificationAccess {
    fun current(): NotificationAccessState

    /** The system settings screen where the user can fix [state]: the channel's when only it is off, else the app's. */
    fun settingsIntent(state: NotificationAccessState): Intent

    companion object {
        /** `Manifest.permission.POST_NOTIFICATIONS` as a plain string: the constant needs API 33 and is only used from API 33. */
        const val PERMISSION = "android.permission.POST_NOTIFICATIONS"
    }
}

/** Reads the notification state of the app from the platform. Nothing about denials is stored. */
class PlatformNotificationAccess(
    private val context: Context,
) : NotificationAccess {
    override fun current(): NotificationAccessState {
        val manager = NotificationManagerCompat.from(context)
        return when {
            needsRuntimePermission() && !hasRuntimePermission() -> NotificationAccessState.PermissionMissing
            !manager.areNotificationsEnabled() -> NotificationAccessState.AppNotificationsOff
            manager.getNotificationChannel(NotificationAlertNotifier.CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE ->
                NotificationAccessState.ChannelOff
            else -> NotificationAccessState.Allowed
        }
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
}
