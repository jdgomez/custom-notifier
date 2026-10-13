package dev.jdgomez.customnotifier.domain

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Rebuilds every product's alarm and delivers due alerts. Idempotent: every trigger (boot, update,
 * clock or zone change, app start, alarm, product change) runs the same code. [zone] is read on every use.
 */
class AlertReconciler(
    private val products: ProductRepository,
    private val deliveries: AlertDeliveries,
    private val scheduler: AlertScheduler,
    private val notifier: AlertNotifier,
    private val clock: Clock,
    private val zone: () -> ZoneId,
) {
    private val lock = Mutex()
    private var known: Set<ProductId> = emptySet()

    /** Reconciles the stored products. */
    suspend fun reconcile() = reconcile(products.observeAll().first())

    /** Reconciles the given [current] products; ones seen before and missing now lose their alarm. */
    suspend fun reconcile(current: List<Product>) =
        lock.withLock {
            val now = clock.instant()
            val zone = zone()
            val ids = current.map { it.id }.toSet()
            (known - ids).forEach(scheduler::cancel)
            known = ids
            current.forEach { product -> reconcile(product, now, zone) }
        }

    private suspend fun reconcile(
        product: Product,
        now: Instant,
        zone: ZoneId,
    ) {
        val id = product.id
        when (val action = AlertAction.decide(product.nextAlertAt(now, zone), deliveries.isDelivered(id))) {
            is AlertAction.Schedule -> {
                scheduler.schedule(id, action.at.toInstant())
                deliveries.clear(id)
            }
            AlertAction.Deliver -> {
                scheduler.cancel(id)
                val today = now.atZone(zone).toLocalDate()
                if (notifier.notify(product, product.depletionDateIn(zone), today)) deliveries.record(id)
            }
            AlertAction.Skip -> scheduler.cancel(id)
        }
    }
}
