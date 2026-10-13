package dev.jdgomez.customnotifier.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import dev.jdgomez.customnotifier.domain.AlertScheduler
import dev.jdgomez.customnotifier.domain.ProductId
import java.time.Instant

/**
 * [AlertScheduler] over [AlarmManager]: one inexact alarm per product that also fires in Doze and
 * wakes the device (ADR 0017). The alarm is identified by a per-product data URI, so products never collide.
 */
class AlarmManagerAlertScheduler(
    private val context: Context,
) : AlertScheduler {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    override fun schedule(
        id: ProductId,
        at: Instant,
    ) {
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pendingIntent(id))
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
}
