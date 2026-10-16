package dev.jdgomez.customnotifier

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import dev.jdgomez.customnotifier.alerts.AlarmManagerAlertScheduler
import dev.jdgomez.customnotifier.alerts.NotificationAccess
import dev.jdgomez.customnotifier.alerts.NotificationAlertNotifier
import dev.jdgomez.customnotifier.alerts.PlatformNotificationAccess
import dev.jdgomez.customnotifier.calendar.CalendarAccess
import dev.jdgomez.customnotifier.calendar.PlatformCalendarAccess
import dev.jdgomez.customnotifier.calendar.ProviderCalendarGateway
import dev.jdgomez.customnotifier.data.CustomNotifierDatabase
import dev.jdgomez.customnotifier.data.MIGRATION_1_2
import dev.jdgomez.customnotifier.data.MIGRATION_2_3
import dev.jdgomez.customnotifier.data.MIGRATION_3_4
import dev.jdgomez.customnotifier.data.RoomAlertDeliveries
import dev.jdgomez.customnotifier.data.RoomCalendarChannelSettings
import dev.jdgomez.customnotifier.data.RoomCalendarEventRecords
import dev.jdgomez.customnotifier.data.RoomProductRepository
import dev.jdgomez.customnotifier.domain.AlertReconciler
import dev.jdgomez.customnotifier.domain.CalendarChannel
import dev.jdgomez.customnotifier.domain.CalendarChannelSettings
import dev.jdgomez.customnotifier.domain.CalendarEventSync
import dev.jdgomez.customnotifier.domain.CalendarGateway
import dev.jdgomez.customnotifier.domain.Product
import dev.jdgomez.customnotifier.domain.ProductChanges
import dev.jdgomez.customnotifier.domain.ProductRepository
import dev.jdgomez.customnotifier.domain.TrackingProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    private val productChanges = ProductChanges()

    /** The stored products; every save or delete is noted for the calendar sync, which recreates only the events of changed products. */
    val productRepository: ProductRepository by lazy {
        TrackingProductRepository(RoomProductRepository(database.productDao()), productChanges)
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val notifier: NotificationAlertNotifier by lazy { NotificationAlertNotifier(appContext) }

    val notificationAccess: NotificationAccess by lazy { PlatformNotificationAccess(appContext) }

    val calendarGateway: CalendarGateway by lazy { ProviderCalendarGateway(appContext) }

    val calendarChannelSettings: CalendarChannelSettings by lazy { RoomCalendarChannelSettings(database.calendarChannelDao()) }

    val calendarAccess: CalendarAccess by lazy { PlatformCalendarAccess(appContext) }

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

    private val calendarEventSync: CalendarEventSync by lazy {
        CalendarEventSync(calendarGateway, RoomCalendarEventRecords(database.calendarEventDao()), productChanges)
    }

    private suspend fun syncCalendar(
        products: List<Product>,
        channel: CalendarChannel,
    ) = calendarEventSync.sync(products, channel, clock.instant(), zone())

    /** Synchronizes the alert events with the stored products and channel now, for example once calendar access is granted. */
    fun syncCalendar() {
        applicationScope.launch { syncStoredCalendar() }
    }

    private suspend fun syncStoredCalendar() =
        syncCalendar(productRepository.observeAll().first(), calendarChannelSettings.observe().first())

    /** Reconciles the stored products and the alert events now, then calls [onDone]; used by the broadcast receivers. */
    fun reconcileAlerts(onDone: () -> Unit) {
        applicationScope.launch {
            try {
                alertReconciler.reconcile()
                syncStoredCalendar()
            } finally {
                onDone()
            }
        }
    }

    /**
     * Creates the notification channel; from now on reconciles the alarms on every product change and the alert events on every
     * product or channel change. Two collectors: slow or failing calendar work never delays or stops the alarms.
     */
    fun startAlerts() {
        notifier.createChannel()
        applicationScope.launch { productRepository.observeAll().collect { alertReconciler.reconcile(it) } }
        applicationScope.launch {
            combine(productRepository.observeAll(), calendarChannelSettings.observe(), ::Pair)
                .conflate()
                .collect { (products, channel) -> syncCalendar(products, channel) }
        }
    }

    private companion object {
        const val DATABASE_NAME = "custom-notifier.db"
    }
}
