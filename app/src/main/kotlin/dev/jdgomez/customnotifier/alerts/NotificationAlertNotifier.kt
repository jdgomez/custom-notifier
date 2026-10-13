package dev.jdgomez.customnotifier.alerts

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.jdgomez.customnotifier.MainActivity
import dev.jdgomez.customnotifier.R
import dev.jdgomez.customnotifier.domain.AlertNotifier
import dev.jdgomez.customnotifier.domain.DepletionDate
import dev.jdgomez.customnotifier.domain.Product
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** [AlertNotifier] posting one notification per product on the "Depletion alerts" channel. */
class NotificationAlertNotifier(
    private val context: Context,
) : AlertNotifier {
    private val manager = NotificationManagerCompat.from(context)

    /** Creates the channel; idempotent, called when the application starts. */
    fun createChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.alert_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        manager.createNotificationChannel(channel)
    }

    // The permission is checked through areNotificationsEnabled; POST_NOTIFICATIONS is declared in the manifest.
    @SuppressLint("MissingPermission")
    override fun notify(
        product: Product,
        depletionDate: DepletionDate,
        today: LocalDate,
    ): Boolean {
        val channel = manager.getNotificationChannel(CHANNEL_ID)
        if (!manager.areNotificationsEnabled() || channel?.importance == NotificationManager.IMPORTANCE_NONE) return false
        val text =
            if (depletionDate.date == today) {
                context.getString(R.string.alert_text_today)
            } else {
                val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(context.resources.configuration.locales[0])
                context.getString(R.string.alert_text, depletionDate.date.format(format))
            }
        val open =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_alert)
                .setContentTitle(context.getString(R.string.alert_title, product.name.value))
                .setContentText(text)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
        // The product identity is the tag, so products never collide and a repeat replaces the earlier one.
        manager.notify(product.id.value.toString(), 0, notification)
        return true
    }

    companion object {
        const val CHANNEL_ID = "depletion_alerts"
    }
}
