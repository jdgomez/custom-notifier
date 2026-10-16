package dev.jdgomez.customnotifier.domain

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

/**
 * What the app created in the calendar for [productId]: the [eventId] in [calendarId] and the [content] last written.
 * A null [eventId] means the event was found gone and waits for the user to change the product.
 */
data class CalendarEventRecord(
    val productId: ProductId,
    val calendarId: CalendarId,
    val eventId: EventId?,
    val content: AlertEvent,
)

/** Port to the stored [CalendarEventRecord]s, one per product. */
interface CalendarEventRecords {
    suspend fun all(): List<CalendarEventRecord>

    suspend fun put(record: CalendarEventRecord)

    suspend fun remove(productId: ProductId)
}

/** The products the user saved or deleted since the last calendar synchronization handled them. */
class ProductChanges {
    private val ids: MutableSet<ProductId> = ConcurrentHashMap.newKeySet()

    fun record(id: ProductId) {
        ids += id
    }

    fun pending(): Set<ProductId> = ids.toSet()

    fun clear(handled: Set<ProductId>) {
        ids -= handled
    }
}

/** A [ProductRepository] that notes every saved or deleted product in [changes] before delegating to [delegate]. */
class TrackingProductRepository(
    private val delegate: ProductRepository,
    private val changes: ProductChanges,
) : ProductRepository by delegate {
    override suspend fun save(product: Product) {
        changes.record(product.id)
        delegate.save(product)
    }

    override suspend fun delete(id: ProductId) {
        changes.record(id)
        delegate.delete(id)
    }
}

/**
 * Keeps one alert event per product with a next alert in the chosen calendar. Idempotent: it writes only what differs, and
 * recreates an event the user deleted only for a product the user changed ([ProductChanges]).
 */
class CalendarEventSync(
    private val gateway: CalendarGateway,
    private val records: CalendarEventRecords,
    private val changes: ProductChanges,
) {
    private val lock = Mutex()

    suspend fun sync(
        products: List<Product>,
        channel: CalendarChannel,
        now: Instant,
        zone: ZoneId,
    ) = lock.withLock {
        // Nothing to write and nothing to delete: do not even ask the device for calendar access.
        if (channel is CalendarChannel.Off && records.all().isEmpty()) return@withLock changes.clear(changes.pending())
        val target = target(channel) ?: return@withLock
        val pending = changes.pending()
        val completed =
            when (target) {
                Target.None -> removeAll()
                is Target.Calendar -> syncAll(products, target.id, pending, now, zone)
            }
        if (completed) changes.clear(pending)
    }

    private sealed interface Target {
        data object None : Target

        data class Calendar(
            val id: CalendarId,
        ) : Target
    }

    /** Null when nothing may be written now: no access, or the chosen calendar is unusable. */
    private suspend fun target(channel: CalendarChannel): Target? =
        when {
            !gateway.canWrite() -> null
            channel is CalendarChannel.On ->
                gateway.find(channel.calendarId)?.let { Target.Calendar(channel.calendarId) }
            else -> Target.None
        }

    private suspend fun removeAll(): Boolean {
        records.all().forEach { discard(it) }
        return true
    }

    private suspend fun discard(record: CalendarEventRecord) {
        record.eventId?.let { gateway.delete(record.calendarId, it) }
        records.remove(record.productId)
    }

    private suspend fun syncAll(
        products: List<Product>,
        calendar: CalendarId,
        pending: Set<ProductId>,
        now: Instant,
        zone: ZoneId,
    ): Boolean {
        val wanted =
            products
                .filter { it.nextAlertAt(now, zone) != NextAlert.None }
                .associate { it.id to AlertEvent(it.name, it.depletionDateIn(zone), it.alertDateIn(zone)) }
        val existing = records.all()
        existing.filter { it.calendarId != calendar || it.productId !in wanted }.forEach { discard(it) }
        val kept = existing.filter { it.calendarId == calendar && it.productId in wanted }.associateBy { it.productId }
        return wanted.all { (id, content) -> syncOne(id, content, kept[id], calendar, id in pending) }
    }

    /** False when the event could not be created and the whole sync must stop. */
    private suspend fun syncOne(
        id: ProductId,
        content: AlertEvent,
        record: CalendarEventRecord?,
        calendar: CalendarId,
        changed: Boolean,
    ): Boolean {
        val eventId = record?.eventId
        val alive =
            when {
                record == null -> return create(id, content, calendar)
                eventId == null -> false
                record.content != content -> gateway.update(calendar, eventId, content)
                else -> gateway.exists(calendar, eventId)
            }
        return when {
            alive -> {
                if (record.content != content) records.put(record.copy(content = content))
                true
            }
            changed -> create(id, content, calendar)
            else -> {
                if (eventId != null) records.put(record.copy(eventId = null))
                true
            }
        }
    }

    private suspend fun create(
        id: ProductId,
        content: AlertEvent,
        calendar: CalendarId,
    ): Boolean {
        val eventId = gateway.create(calendar, content) ?: return false
        records.put(CalendarEventRecord(id, calendar, eventId, content))
        return true
    }
}
