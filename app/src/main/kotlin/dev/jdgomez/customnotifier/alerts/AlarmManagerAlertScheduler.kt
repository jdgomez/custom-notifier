package dev.jdgomez.customnotifier.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import dev.jdgomez.customnotifier.domain.AlertScheduler
import dev.jdgomez.customnotifier.domain.ProductId
import java.time.Duration
import java.time.Instant

/**
 * [AlertScheduler] over [AlarmManager]: one inexact alarm per product that wakes the device and is delivered
 * within [WINDOW] after the alert moment, whatever the lead time (ADR 0017). The alarm is identified by a
 * per-product data URI, so products never collide.
 */
class AlarmManagerAlertScheduler(
    private val context: Context,
) : AlertScheduler {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    override fun schedule(
        id: ProductId,
        at: Instant,
    ) {
        alarms.setWindow(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), WINDOW.toMillis(), pendingIntent(id))
    }

    override fun cancel(id: ProductId) {
        alarms.cancel(pendingIntent(id))
    }

    private fun pendingIntent(id: ProductId): PendingIntent {
        val intent =
            Intent(context, AlertAlarmReceiver::class.java)
                .setData("customnotifier://alert/${id.value}".toUri())
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private companion object {
        val WINDOW: Duration = Duration.ofHours(3)
    }
}
