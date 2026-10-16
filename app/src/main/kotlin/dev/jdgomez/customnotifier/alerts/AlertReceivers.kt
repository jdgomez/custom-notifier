package dev.jdgomez.customnotifier.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.jdgomez.customnotifier.CustomNotifierApplication

private fun BroadcastReceiver.reconcile(context: Context) {
    val container = (context.applicationContext as CustomNotifierApplication).container
    val pending = goAsync()
    container.reconcileAlerts { pending.finish() }
}

/** A product's alarm fired: reconciles, which delivers the due alert (or nothing for a product that is gone). */
class AlertAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) = reconcile(context)
}

/** Device boot, app update, time zone, clock and language changes: the alarms are rebuilt and missed alerts delivered. */
class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action in ACTIONS) reconcile(context)
    }

    private companion object {
        val ACTIONS =
            setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_LOCALE_CHANGED,
            )
    }
}
