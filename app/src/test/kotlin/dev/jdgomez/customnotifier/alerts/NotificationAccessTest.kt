package dev.jdgomez.customnotifier.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import dev.jdgomez.customnotifier.CustomNotifierApplication
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationAccessTest {
    private val app: CustomNotifierApplication = ApplicationProvider.getApplicationContext()
    private val manager = app.getSystemService(NotificationManager::class.java)
    private val access = PlatformNotificationAccess(app)

    private fun createChannel(importance: Int = NotificationManager.IMPORTANCE_DEFAULT) =
        manager.createNotificationChannel(NotificationChannel(NotificationAlertNotifier.CHANNEL_ID, "Depletion alerts", importance))

    private fun grant() = shadowOf(app).grantPermissions(NotificationAccess.PERMISSION)

    private fun deny() = shadowOf(app).denyPermissions(NotificationAccess.PERMISSION)

    @Test
    fun `permission missing`() {
        deny()
        assertEquals(NotificationAccessState.PermissionMissing, access.current())
    }

    @Test
    fun `granted, alerts are allowed and revoking shows the permission missing again`() {
        createChannel()
        grant()
        assertEquals(NotificationAccessState.Allowed, access.current())
        deny()
        assertEquals(NotificationAccessState.PermissionMissing, access.current())
    }

    @Test
    fun `granted but notifications off for the app`() {
        grant()
        createChannel()
        shadowOf(manager).setNotificationsEnabled(false)
        assertEquals(NotificationAccessState.AppNotificationsOff, access.current())
    }

    @Test
    fun `granted but the channel is off`() {
        grant()
        createChannel(NotificationManager.IMPORTANCE_NONE)
        assertEquals(NotificationAccessState.ChannelOff, access.current())
    }

    @Test
    fun `settings intents target the app or the channel`() {
        val appIntent = access.settingsIntent(NotificationAccessState.PermissionMissing)
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, appIntent.action)
        assertEquals(app.packageName, appIntent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
        val channelIntent: Intent = access.settingsIntent(NotificationAccessState.ChannelOff)
        assertEquals(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS, channelIntent.action)
        assertEquals(NotificationAlertNotifier.CHANNEL_ID, channelIntent.getStringExtra(Settings.EXTRA_CHANNEL_ID))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class NotificationAccessApi26Test {
    private val app: CustomNotifierApplication = ApplicationProvider.getApplicationContext()
    private val manager = app.getSystemService(NotificationManager::class.java)
    private val access = PlatformNotificationAccess(app)

    @Test
    fun `no runtime permission, allowed unless the channel or the app is off`() {
        manager.createNotificationChannel(
            NotificationChannel(NotificationAlertNotifier.CHANNEL_ID, "Depletion alerts", NotificationManager.IMPORTANCE_DEFAULT),
        )
        assertEquals(NotificationAccessState.Allowed, access.current())

        manager.createNotificationChannel(
            NotificationChannel(NotificationAlertNotifier.CHANNEL_ID, "Depletion alerts", NotificationManager.IMPORTANCE_NONE),
        )
        assertEquals(NotificationAccessState.ChannelOff, access.current())
    }
}
