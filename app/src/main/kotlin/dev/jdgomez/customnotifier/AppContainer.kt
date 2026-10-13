package dev.jdgomez.customnotifier

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import dev.jdgomez.customnotifier.alerts.AlarmManagerAlertScheduler
import dev.jdgomez.customnotifier.alerts.NotificationAlertNotifier
import dev.jdgomez.customnotifier.data.CustomNotifierDatabase
import dev.jdgomez.customnotifier.data.MIGRATION_1_2
import dev.jdgomez.customnotifier.data.RoomAlertDeliveries
import dev.jdgomez.customnotifier.data.RoomProductRepository
import dev.jdgomez.customnotifier.domain.AlertReconciler
import dev.jdgomez.customnotifier.domain.ProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneId

/**
 * Manual dependency injection: the application-wide dependencies, built lazily (ADR 0015).
 *
 * [clock] gives the current instant and [zone] the device's current time zone, read on every use so a
 * zone change is picked up; both can be replaced in tests.
 */
class AppContainer(
    context: Context,
    val clock: Clock = Clock.systemUTC(),
    val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    private val appContext = context.applicationContext

    private val database: CustomNotifierDatabase by lazy {
        Room
            .databaseBuilder<CustomNotifierDatabase>(context, DATABASE_NAME)
            .setDriver(AndroidSQLiteDriver())
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    val productRepository: ProductRepository by lazy { RoomProductRepository(database.productDao()) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val notifier: NotificationAlertNotifier by lazy { NotificationAlertNotifier(appContext) }

    private val alertReconciler: AlertReconciler by lazy {
        AlertReconciler(
            productRepository,
            RoomAlertDeliveries(database.alertDeliveryDao()),
            AlarmManagerAlertScheduler(appContext),
            notifier,
            clock,
            zone,
        )
    }

    /** Reconciles the stored products now, then calls [onDone]; used by the broadcast receivers. */
    fun reconcileAlerts(onDone: () -> Unit) {
        applicationScope.launch {
            try {
                alertReconciler.reconcile()
            } finally {
                onDone()
            }
        }
    }

    /** Creates the notification channel and reconciles on every product change, from now on. */
    fun startAlerts() {
        notifier.createChannel()
        applicationScope.launch { productRepository.observeAll().collect { alertReconciler.reconcile(it) } }
    }

    private companion object {
        const val DATABASE_NAME = "custom-notifier.db"
    }
}
